package io.github.ramiro.escapesj.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ramiro.escapesj.modelo.Cliente;
import io.github.ramiro.escapesj.modelo.Cuit;
import io.github.ramiro.escapesj.persistencia.ClienteCacheRepository;
import io.github.ramiro.escapesj.persistencia.ConfigRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de consulta al padrón de AFIP usando la REST API de Afip SDK.
 * <p>
 * Flujo de 2 pasos:
 * 1. POST /api/v1/afip/auth → obtiene token y sign del WSAA
 * 2. POST /api/v1/afip/requests → consulta el padrón con token y sign
 * <p>
 * Cache proxy en SQLite para evitar llamadas repetidas.
 */
public class AfipService {

    private static final String AFIP_SDK_AUTH_URL = "https://app.afipsdk.com/api/v1/afip/auth";
    private static final String AFIP_SDK_API_URL = "https://app.afipsdk.com/api/v1/afip/requests";
    private static final String WSID = "ws_sr_padron_a13";
    private static final Gson gson = new Gson();

    private final ConfigRepository configRepo;
    private final ClienteCacheRepository cacheRepo;

    // Cliente HTTP reutilizado para todas las peticiones
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // Cache del Ticket de Acceso (token + sign) para no pedirlo en cada consulta
    private String wsaaToken;
    private String wsaaSign;
    private Instant wsaaExpiration;

    public AfipService(ConfigRepository configRepo, ClienteCacheRepository cacheRepo) {
        this.configRepo = configRepo;
        this.cacheRepo = cacheRepo;

        // Limpiar entradas expiradas al iniciar el servicio
        int limpiados = cacheRepo.limpiarExpirados();
        if (limpiados > 0) {
            System.out.println("Cache AFIP: Se limpiaron " + limpiados + " entradas expiradas.");
        }
    }

    /**
     * Verifica si el servicio AFIP está configurado (tiene access_token).
     */
    public boolean estaConfigurado() {
        return !configRepo.getAfipAccessToken().isBlank();
    }

    /**
     * Guarda un nombre ingresado manualmente en el cache local.
     * Así, la próxima vez que se ingrese el mismo DNI, el nombre se autocompleta.
     */
    public void guardarEnCacheManual(String dni, String nombre) {
        if (dni != null && !dni.isBlank() && nombre != null && !nombre.isBlank()
                && esNombreValido(nombre)) {
            cacheRepo.guardar(dni, nombre, "", "20"); // Sin CUIT conocido, prefijo genérico
        }
    }

    /**
     * Verifica que un nombre no sea un placeholder o texto de estado de la UI.
     */
    private boolean esNombreValido(String nombre) {
        return nombre != null && !nombre.isBlank()
                && !nombre.equals("Se completa automáticamente")
                && !nombre.equals("Buscando...")
                && !nombre.equals("Error al buscar")
                && !nombre.startsWith("No se encontró");
    }

    /**
     * Busca en el cache local de SQLite.
     */
    private Optional<Cliente> buscarEnCache(String dni) {
        return cacheRepo.buscarPorDni(dni)
                .filter(entrada -> esNombreValido(entrada.nombre())) // Rechazar entradas con basura
                .flatMap(entrada ->
                    Cuit.intentarCrear(dni, entrada.prefijoCuit())
                            .map(cuit -> new Cliente(entrada.nombre(), cuit))
                );
    }

    /**
     * Busca SOLO en cache local (sin llamar a AFIP).
     * Seguro para llamar desde el hilo de UI sin bloquear.
     */
    public Optional<Cliente> buscarSoloEnCache(String dni) {
        if (dni == null || dni.isBlank()) return Optional.empty();
        return buscarEnCache(dni);
    }

    /**
     * Búsqueda asíncrona (no bloqueante) utilizable por la UI de Swing.
     * Encadena llamadas HTTP asíncronas internamente sin bloquear hilos.
     */
    public CompletableFuture<Optional<Cliente>> buscarClientePorDniAsync(String dni) {
        if (dni == null || dni.isBlank() || !dni.matches("\\d+")) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        // 1. Intentar desde cache (síncrono y rápido, base de datos local embebida)
        Optional<Cliente> desdeCache = buscarEnCache(dni);
        if (desdeCache.isPresent()) {
            System.out.println("AFIP Cache HIT para DNI: " + dni);
            return CompletableFuture.completedFuture(desdeCache);
        }

        // 2. Si no hay cache, consultar la API (requiere configuración)
        if (!estaConfigurado()) {
            System.out.println("AFIP no configurado. Sin cache para DNI: " + dni);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        System.out.println("AFIP Cache MISS para DNI: " + dni + " — Consultando REST API de Afip SDK...");
        return consultarApiYCachearAsync(dni);
    }

    // ================================================================
    //  FLUJO DE AUTENTICACIÓN + CONSULTA (ASYNC)
    // ================================================================

    private String getEnvironment() {
        String cuit = configRepo.getAfipCuit();
        boolean esTestCuit = "20409378472".equals(cuit);
        if (esTestCuit && configRepo.isAfipProduction()) {
            System.out.println("AFIP: CUIT de testing detectado, forzando ambiente 'dev'.");
        }
        return (configRepo.isAfipProduction() && !esTestCuit) ? "prod" : "dev";
    }

    /**
     * Paso 1: Obtiene el Ticket de Acceso de forma asíncrona.
     */
    private CompletableFuture<Boolean> autenticarAsync() {
        if (wsaaToken != null && wsaaSign != null && wsaaExpiration != null
                && Instant.now().isBefore(wsaaExpiration)) {
            System.out.println("AFIP Auth: Reutilizando Ticket de Acceso vigente.");
            return CompletableFuture.completedFuture(true);
        }

        String environment = getEnvironment();
        String cuit = configRepo.getAfipCuit();

        JsonObject authBody = new JsonObject();
        authBody.addProperty("environment", environment);
        authBody.addProperty("tax_id", cuit);
        authBody.addProperty("wsid", WSID);

        if ("prod".equals(environment)) {
            String certPath = configRepo.getAfipCertPath();
            String keyPath = configRepo.getAfipKeyPath();

            if (certPath.isBlank() || keyPath.isBlank()) {
                System.err.println("AFIP Auth: Modo producción requiere cert y key.");
                return CompletableFuture.completedFuture(false);
            }

            try {
                // RIESGO MITIGADO: Se cargan temporalmente pero el scope es cortísimo y la memoria se libera al enviar JSON.
                // En futuras iteraciones se podría considerar conectarse a WSAA directo sin AfipSDK.
                String certContent = Files.readString(Path.of(certPath), StandardCharsets.UTF_8);
                String keyContent = Files.readString(Path.of(keyPath), StandardCharsets.UTF_8);
                
                authBody.addProperty("cert", certContent);
                authBody.addProperty("key", keyContent);
                
                certContent = null;
                keyContent = null;
            } catch (IOException e) {
                System.err.println("AFIP Auth: Error leyendo cert/key: " + e.getMessage());
                return CompletableFuture.completedFuture(false);
            }
        }

        System.out.println("AFIP Auth → POST " + AFIP_SDK_AUTH_URL + " [" + environment + "]");

        return httpPostAsync(AFIP_SDK_AUTH_URL, authBody, 2).thenApply(responseBody -> {
            if (responseBody == null) return false;

            JsonObject resp = gson.fromJson(responseBody, JsonObject.class);
            if (resp.has("token") && resp.has("sign")) {
                wsaaToken = resp.get("token").getAsString();
                wsaaSign = resp.get("sign").getAsString();

                if (resp.has("expiration")) {
                    try {
                        wsaaExpiration = Instant.parse(resp.get("expiration").getAsString()).minusSeconds(1800);
                    } catch (Exception e) {
                        wsaaExpiration = Instant.now().plusSeconds(3600 * 10);
                    }
                } else {
                    wsaaExpiration = Instant.now().plusSeconds(3600 * 10);
                }

                System.out.println("AFIP Auth ← OK. Token obtenido. Expira: " + wsaaExpiration);
                return true;
            }

            System.err.println("AFIP Auth ← Error: No se recibió token/sign. Respuesta: " + responseBody);
            return false;
        });
    }

    /**
     * Flujo completo encadenado asíncrono.
     */
    private CompletableFuture<Optional<Cliente>> consultarApiYCachearAsync(String dni) {
        return autenticarAsync().thenCompose(authOk -> {
            if (!authOk) {
                System.err.println("AFIP: No se pudo autenticar con el WSAA.");
                return CompletableFuture.completedFuture(Optional.<Cliente>empty());
            }

            String environment = getEnvironment();
            String cuitRepresentada = configRepo.getAfipCuit();
            long dniLong = Long.parseLong(dni);

            JsonObject reqCuit = new JsonObject();
            reqCuit.addProperty("environment", environment);
            reqCuit.addProperty("method", "getIdPersonaListByDocumento");
            reqCuit.addProperty("wsid", WSID);
            
            JsonObject paramsCuit = new JsonObject();
            paramsCuit.addProperty("token", wsaaToken);
            paramsCuit.addProperty("sign", wsaaSign);
            paramsCuit.addProperty("cuitRepresentada", Long.parseLong(cuitRepresentada));
            paramsCuit.addProperty("documento", dniLong);
            reqCuit.add("params", paramsCuit);

            System.out.println("AFIP Padrón → Buscando CUIT para DNI " + dni + "...");
            return httpPostAsync(AFIP_SDK_API_URL, reqCuit, 2).thenCompose(respCuitStr -> {
                if (respCuitStr == null) return CompletableFuture.completedFuture(Optional.<Cliente>empty());

                long cuitNumerico = extraerCuit(gson.fromJson(respCuitStr, JsonElement.class));
                if (cuitNumerico == -1) {
                    System.out.println("AFIP: DNI " + dni + " no encontrado en el padrón.");
                    return CompletableFuture.completedFuture(Optional.<Cliente>empty());
                }

                System.out.println("AFIP: CUIT encontrado para DNI " + dni + ": " + cuitNumerico);

                JsonObject reqPersona = new JsonObject();
                reqPersona.addProperty("environment", environment);
                reqPersona.addProperty("method", "getPersona");
                reqPersona.addProperty("wsid", WSID);
                
                JsonObject paramsPersona = new JsonObject();
                paramsPersona.addProperty("token", wsaaToken);
                paramsPersona.addProperty("sign", wsaaSign);
                paramsPersona.addProperty("cuitRepresentada", Long.parseLong(cuitRepresentada));
                paramsPersona.addProperty("idPersona", cuitNumerico);
                reqPersona.add("params", paramsPersona);

                System.out.println("AFIP Padrón → Obteniendo datos para CUIT " + cuitNumerico + "...");
                return httpPostAsync(AFIP_SDK_API_URL, reqPersona, 2).thenApply(respPersonaStr -> {
                    if (respPersonaStr == null) return Optional.<Cliente>empty();

                    JsonObject respPersona = gson.fromJson(respPersonaStr, JsonObject.class);
                    JsonObject datos = respPersona;
                    if (datos.has("personaReturn") && datos.get("personaReturn").isJsonObject()) {
                        datos = datos.getAsJsonObject("personaReturn");
                    }
                    if (datos.has("persona") && datos.get("persona").isJsonObject()) {
                        datos = datos.getAsJsonObject("persona");
                    }
                    if (datos.has("data") && datos.get("data").isJsonObject()) {
                        datos = datos.getAsJsonObject("data");
                    }

                    String nombre = extraerNombre(datos);
                    String cuitStr = String.valueOf(cuitNumerico);
                    String prefijo = cuitStr.substring(0, 2);

                    cacheRepo.guardar(dni, nombre, cuitStr, prefijo);
                    System.out.println("AFIP: Resultado cacheado — " + nombre + " (CUIT: " + cuitStr + ")");

                    return Cuit.intentarCrear(dni, prefijo).map(cuit -> new Cliente(nombre, cuit));
                });
            });
        }).exceptionally(ex -> {
            System.err.println("Error consultando AFIP para DNI " + dni + ": " + ex.getMessage());
            return Optional.<Cliente>empty();
        });
    }

    // ================================================================
    //  HELPERS (HTTP & JSON)
    // ================================================================

    /**
     * Envía un POST JSON a la URL indicada de forma asíncrona, con reintentos para errores transitorios (5xx o Red).
     */
    private CompletableFuture<String> httpPostAsync(String url, JsonObject body, int maxRetries) {
        String jsonBody = gson.toJson(body);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        if (!url.equals(AFIP_SDK_AUTH_URL)) {
            String accessToken = configRepo.getAfipAccessToken();
            if (accessToken != null && !accessToken.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + accessToken);
            }
        }

        HttpRequest request = requestBuilder.build();
        return sendWithRetry(request, maxRetries, 1);
    }

    /**
     * Lógica de reintento con backoff exponencial.
     */
    private CompletableFuture<String> sendWithRetry(HttpRequest request, int maxRetries, int attempt) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .handle((response, ex) -> {
                if (ex != null) {
                    if (attempt <= maxRetries) {
                        System.err.println("AFIP Error Red (intento " + attempt + "): " + ex.getMessage() + ". Reintentando...");
                        return delayedRetry(request, maxRetries, attempt);
                    }
                    System.err.println("AFIP Error de Red definitivo: " + ex.getMessage());
                    return CompletableFuture.<String>completedFuture(null);
                }
                
                int status = response.statusCode();
                if (status >= 500 && attempt <= maxRetries) {
                    System.err.println("AFIP Error HTTP " + status + " (intento " + attempt + "). Reintentando...");
                    return delayedRetry(request, maxRetries, attempt);
                } else if (status >= 400) {
                    System.err.println("AFIP Error HTTP " + status + ": " + response.body());
                    return CompletableFuture.<String>completedFuture(null);
                }
                
                return CompletableFuture.completedFuture(response.body());
            }).thenCompose(cf -> cf);
    }

    private CompletableFuture<String> delayedRetry(HttpRequest request, int maxRetries, int attempt) {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(attempt * 1000L); } catch (Exception ignored) {}
            return null;
        }).thenCompose(v -> sendWithRetry(request, maxRetries, attempt + 1));
    }

    private long extraerCuit(JsonElement respuesta) {
        if (respuesta.isJsonObject()) {
            JsonObject obj = respuesta.getAsJsonObject();
            if (obj.has("idPersonaListReturn")) {
                JsonObject inner = obj.getAsJsonObject("idPersonaListReturn");
                if (inner.has("idPersona")) {
                    return extraerCuit(inner.get("idPersona"));
                }
            }
            if (obj.has("data")) return extraerCuit(obj.get("data"));
            if (obj.has("idPersona")) return extraerCuit(obj.get("idPersona"));
            return -1;
        }
        if (respuesta.isJsonArray()) {
            JsonArray arr = respuesta.getAsJsonArray();
            if (arr.isEmpty()) return -1;
            return arr.get(0).getAsLong();
        }
        if (respuesta.isJsonPrimitive() && respuesta.getAsJsonPrimitive().isNumber()) {
            return respuesta.getAsLong();
        }
        return -1;
    }

    private String extraerNombre(JsonObject datos) {
        if (datos.has("datosGenerales") && datos.get("datosGenerales").isJsonObject()) {
            JsonObject dg = datos.getAsJsonObject("datosGenerales");
            String nombre = campo(dg, "nombre");
            String apellido = campo(dg, "apellido");
            if (!nombre.isBlank() || !apellido.isBlank()) return (apellido + " " + nombre).trim();
            String razonSocial = campo(dg, "razonSocial");
            if (!razonSocial.isBlank()) return razonSocial;
        }

        String nombre = campo(datos, "nombre");
        String apellido = campo(datos, "apellido");
        if (!nombre.isBlank() || !apellido.isBlank()) return (apellido + " " + nombre).trim();

        String rs = campo(datos, "razonSocial");
        if (!rs.isBlank()) return rs;
        rs = campo(datos, "razonsocial");
        if (!rs.isBlank()) return rs;

        return "Sin Nombre";
    }

    private String campo(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) return obj.get(key).getAsString();
        return "";
    }
}