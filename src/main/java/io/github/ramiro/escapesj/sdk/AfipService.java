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
     * Busca un cliente por DNI.
     * Primero consulta el cache local; si no encuentra, va a la API de AFIP.
     */
    public Optional<Cliente> buscarClientePorDni(String dni) {
        if (dni == null || dni.isBlank() || !dni.matches("\\d+")) {
            return Optional.empty();
        }

        // 1. Intentar desde cache
        Optional<Cliente> desdeCache = buscarEnCache(dni);
        if (desdeCache.isPresent()) {
            System.out.println("AFIP Cache HIT para DNI: " + dni);
            return desdeCache;
        }

        // 2. Si no hay cache, consultar la API (requiere configuración)
        if (!estaConfigurado()) {
            System.out.println("AFIP no configurado. Sin cache para DNI: " + dni);
            return Optional.empty();
        }

        System.out.println("AFIP Cache MISS para DNI: " + dni + " — Consultando REST API de Afip SDK...");
        return consultarApiYCachear(dni);
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

    // ================================================================
    //  FLUJO DE AUTENTICACIÓN + CONSULTA
    // ================================================================

    /**
     * Obtiene el ambiente correcto. El CUIT de testing solo funciona en "dev".
     */
    private String getEnvironment() {
        String cuit = configRepo.getAfipCuit();
        boolean esTestCuit = "20409378472".equals(cuit);
        if (esTestCuit && configRepo.isAfipProduction()) {
            System.out.println("AFIP: CUIT de testing detectado, forzando ambiente 'dev'.");
        }
        return (configRepo.isAfipProduction() && !esTestCuit) ? "prod" : "dev";
    }

    /**
     * Paso 1: Obtiene el Ticket de Acceso (token + sign) del WSAA via Afip SDK.
     * El resultado se cachea en memoria hasta que expire (~12 horas).
     */
    private boolean autenticar() throws IOException {
        // Si ya tenemos un ticket vigente, reutilizarlo
        if (wsaaToken != null && wsaaSign != null && wsaaExpiration != null
                && Instant.now().isBefore(wsaaExpiration)) {
            System.out.println("AFIP Auth: Reutilizando Ticket de Acceso vigente.");
            return true;
        }

        String environment = getEnvironment();
        String cuit = configRepo.getAfipCuit();

        JsonObject authBody = new JsonObject();
        authBody.addProperty("environment", environment);
        authBody.addProperty("tax_id", cuit);
        authBody.addProperty("wsid", WSID);

        // En producción, incluir cert y key en el request
        if ("prod".equals(environment)) {
            String certPath = configRepo.getAfipCertPath();
            String keyPath = configRepo.getAfipKeyPath();

            if (certPath.isBlank() || keyPath.isBlank()) {
                System.err.println("AFIP Auth: Modo producción requiere cert y key. Configurá las rutas en ⚙ Configuración.");
                return false;
            }

            try {
                String certContent = Files.readString(Path.of(certPath), StandardCharsets.UTF_8);
                String keyContent = Files.readString(Path.of(keyPath), StandardCharsets.UTF_8);
                authBody.addProperty("cert", certContent);
                authBody.addProperty("key", keyContent);
                System.out.println("AFIP Auth: Cert y Key cargados desde archivos locales.");
            } catch (IOException e) {
                System.err.println("AFIP Auth: Error leyendo cert/key: " + e.getMessage());
                return false;
            }
        }

        System.out.println("AFIP Auth → POST " + AFIP_SDK_AUTH_URL + " [" + environment + "]");

        String responseBody = httpPost(AFIP_SDK_AUTH_URL, authBody);
        if (responseBody == null) {
            return false;
        }

        JsonObject resp = gson.fromJson(responseBody, JsonObject.class);

        if (resp.has("token") && resp.has("sign")) {
            wsaaToken = resp.get("token").getAsString();
            wsaaSign = resp.get("sign").getAsString();

            // Parsear la expiración, con margen de 30 minutos
            if (resp.has("expiration")) {
                try {
                    wsaaExpiration = Instant.parse(resp.get("expiration").getAsString())
                            .minusSeconds(1800);
                } catch (Exception e) {
                    wsaaExpiration = Instant.now().plusSeconds(3600 * 10); // 10 horas por defecto
                }
            } else {
                wsaaExpiration = Instant.now().plusSeconds(3600 * 10);
            }

            System.out.println("AFIP Auth ← OK. Token obtenido. Expira: " + wsaaExpiration);
            return true;
        }

        System.err.println("AFIP Auth ← Error: No se recibió token/sign. Respuesta: " + responseBody);
        return false;
    }

    /**
     * Flujo completo: Autenticar → Buscar CUIT por DNI → Buscar datos por CUIT → Cachear
     */
    private Optional<Cliente> consultarApiYCachear(String dni) {
        try {
            // Paso 0: Autenticar con WSAA
            if (!autenticar()) {
                System.err.println("AFIP: No se pudo autenticar con el WSAA. Verificá el Access Token.");
                return Optional.empty();
            }

            String environment = getEnvironment();
            String cuitRepresentada = configRepo.getAfipCuit();
            long dniLong = Long.parseLong(dni);

            // Paso 1: Obtener CUIT desde DNI
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
            String respCuitStr = httpPost(AFIP_SDK_API_URL, reqCuit);
            if (respCuitStr == null) {
                return Optional.empty();
            }

            JsonElement respCuitJson = gson.fromJson(respCuitStr, JsonElement.class);
            long cuitNumerico = extraerCuit(respCuitJson);
            if (cuitNumerico == -1) {
                System.out.println("AFIP: DNI " + dni + " no encontrado en el padrón.");
                return Optional.empty();
            }

            System.out.println("AFIP: CUIT encontrado para DNI " + dni + ": " + cuitNumerico);

            // Paso 2: Obtener datos del contribuyente con el CUIT
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
            String respPersonaStr = httpPost(AFIP_SDK_API_URL, reqPersona);
            if (respPersonaStr == null) {
                return Optional.empty();
            }

            JsonObject respPersona = gson.fromJson(respPersonaStr, JsonObject.class);

            // Extraer datos — la respuesta real de AFIP viene con wrappers
            // Posibles estructuras:
            //   {personaReturn: {persona: {datosGenerales: ...}}}
            //   {data: {datosGenerales: ...}}
            //   {datosGenerales: ...}
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

            System.out.println("AFIP Padrón → Datos recibidos: " + gson.toJson(datos).substring(0, Math.min(200, gson.toJson(datos).length())) + "...");

            String nombre = extraerNombre(datos);
            String cuitStr = String.valueOf(cuitNumerico);
            String prefijo = cuitStr.substring(0, 2);

            // Guardar en cache para futuras consultas
            cacheRepo.guardar(dni, nombre, cuitStr, prefijo);
            System.out.println("AFIP: Resultado cacheado — " + nombre + " (CUIT: " + cuitStr + ")");

            return Cuit.intentarCrear(dni, prefijo)
                    .map(cuit -> new Cliente(nombre, cuit));

        } catch (Exception e) {
            System.err.println("Error consultando AFIP para DNI " + dni + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    /**
     * Envía un POST JSON a la URL indicada con el access_token como Bearer.
     * Retorna el body de respuesta como String, o null si hubo error.
     */
    private String httpPost(String url, JsonObject body) throws IOException {
        String accessToken = configRepo.getAfipAccessToken();
        String jsonBody = gson.toJson(body);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String responseBody = response.body();

            System.out.println("AFIP ← Status: " + status + " | " + responseBody);

            if (status >= 400) {
                System.err.println("AFIP Error HTTP " + status + ": " + responseBody);
                return null;
            }

            return responseBody;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("La solicitud fue interrumpida", e);
        }
    }

    /**
     * Extrae el primer CUIT de la respuesta de getIdPersonaListByDocumento.
     * Formato real de AFIP: {"idPersonaListReturn":{"idPersona":[20460006989],...}}
     * También maneja: un número directo, un array, o un objeto con campo "data".
     */
    private long extraerCuit(JsonElement respuesta) {
        if (respuesta.isJsonObject()) {
            JsonObject obj = respuesta.getAsJsonObject();

            // Formato real de AFIP: idPersonaListReturn → idPersona
            if (obj.has("idPersonaListReturn")) {
                JsonObject inner = obj.getAsJsonObject("idPersonaListReturn");
                if (inner.has("idPersona")) {
                    return extraerCuit(inner.get("idPersona"));
                }
            }

            // Formato alternativo con wrapper "data"
            if (obj.has("data")) {
                return extraerCuit(obj.get("data"));
            }

            // Buscar "idPersona" directamente
            if (obj.has("idPersona")) {
                return extraerCuit(obj.get("idPersona"));
            }

            return -1;
        }

        // Si es un array, tomar el primer elemento
        if (respuesta.isJsonArray()) {
            JsonArray arr = respuesta.getAsJsonArray();
            if (arr.isEmpty()) return -1;
            return arr.get(0).getAsLong();
        }

        // Si es un número directo
        if (respuesta.isJsonPrimitive() && respuesta.getAsJsonPrimitive().isNumber()) {
            return respuesta.getAsLong();
        }

        return -1;
    }

    /**
     * Extrae el nombre completo del JSON de datos del contribuyente.
     */
    private String extraerNombre(JsonObject datos) {
        // Persona física: datosGenerales → nombre y apellido
        if (datos.has("datosGenerales") && datos.get("datosGenerales").isJsonObject()) {
            JsonObject dg = datos.getAsJsonObject("datosGenerales");
            String nombre = campo(dg, "nombre");
            String apellido = campo(dg, "apellido");
            if (!nombre.isBlank() || !apellido.isBlank()) {
                return (apellido + " " + nombre).trim();
            }
            String razonSocial = campo(dg, "razonSocial");
            if (!razonSocial.isBlank()) return razonSocial;
        }

        // Fallback: campos en la raíz
        String nombre = campo(datos, "nombre");
        String apellido = campo(datos, "apellido");
        if (!nombre.isBlank() || !apellido.isBlank()) {
            return (apellido + " " + nombre).trim();
        }

        String rs = campo(datos, "razonSocial");
        if (!rs.isBlank()) return rs;
        rs = campo(datos, "razonsocial");
        if (!rs.isBlank()) return rs;

        return "Sin Nombre";
    }

    private String campo(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }
}