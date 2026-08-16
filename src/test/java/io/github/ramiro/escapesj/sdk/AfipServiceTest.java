package io.github.ramiro.escapesj.sdk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ramiro.escapesj.modelo.Cliente;
import io.github.ramiro.escapesj.persistencia.ClienteCacheRepository;
import io.github.ramiro.escapesj.persistencia.ClienteCacheRepository.EntradaCache;
import io.github.ramiro.escapesj.persistencia.ConfigRepository;
import io.github.ramiro.escapesj.persistencia.DatabaseService;
import io.github.ramiro.escapesj.persistencia.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.CompletionException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class AfipServiceTest {

    private AfipService afipService;
    private ClienteCacheRepository cacheRepo;
    private ConfigRepository configRepo;
    private TestHttpClient mockHttpClient;

    class TestHttpClient extends MockHttpClient {
        public List<HttpRequest> requests = new ArrayList<>();
        public boolean failAuth = false;
        public boolean slowAuth = false;
        public int serverErrors = 0;

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            requests.add(request);
            String url = request.uri().toString();

            if (serverErrors > 0) {
                serverErrors--;
                return CompletableFuture.completedFuture(new TestHttpResponse<>(500, (T) "Internal Server Error"));
            }

            int statusCode = 200;
            String body = "";

            if (url.endsWith("/auth")) {
                if (failAuth) {
                    statusCode = 401;
                    body = "{\"error\": \"Unauthorized\"}";
                } else {
                    body = "{\"token\": \"mock_t\", \"sign\": \"mock_s\", \"expiration\": \"2030-01-01T00:00:00Z\"}";
                }
            } else if (url.endsWith("/requests")) {
                long reqCount = requests.stream().filter(r -> r.uri().toString().endsWith("/requests")).count();
                if (reqCount == 1) {
                    body = "{\"data\": {\"idPersona\": 20111111112}}";
                } else {
                    body = "{\"data\": {\"nombre\": \"Leo\", \"apellido\": \"Messi\"}}";
                }
            }

            HttpResponse<T> response = new TestHttpResponse<>(statusCode, (T) body);
            if (slowAuth && url.endsWith("/auth")) {
                return CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                    return response;
                });
            }
            return CompletableFuture.completedFuture(response);
        }
    }

    class TestHttpResponse<T> implements HttpResponse<T> {
        private final int statusCode;
        private final T body;

        TestHttpResponse(int statusCode, T body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override public int statusCode() { return statusCode; }
        @Override public HttpRequest request() { return null; }
        @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return null; }
        @Override public T body() { return body; }
        @Override public Optional<javax.net.ssl.SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return null; }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_2; }
    }

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path tempDir;

    @BeforeEach
    public void setup() throws Exception {
        java.nio.file.Path db = tempDir.resolve("escapesj-test-afip.db");
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + db.toAbsolutePath().toString());
        DatabaseService.reiniciarTest();
        DatabaseService.inicializar();
        configRepo = new ConfigRepository() {
            @Override public void guardar(String clave, String valor) {}
            @Override public Optional<String> obtener(String clave) { return Optional.empty(); }
            @Override public String getAfipCuit() { return "20409378472"; }
            @Override public boolean isAfipProduction() { return false; }
            @Override public String getAfipAccessToken() { return "test-token"; }
            @Override public String getAfipCertPath() { return ""; }
            @Override public String getAfipKeyPath() { return ""; }
        };

        cacheRepo = new ClienteCacheRepository() {
            public boolean guardado = false;
            @Override public int limpiarExpirados() { return 0; }
            @Override public void guardar(String dni, String nombre, String cuit, String prefijoCuit) { guardado = true; }
            @Override public Optional<EntradaCache> buscarPorDni(String dni) {
                if ("40937847".equals(dni)) {
                    return Optional.of(new EntradaCache("40937847", "Perez Juan", "20409378472", "20", "2026-07-28"));
                }
                return Optional.empty();
            }
        };

        mockHttpClient = new TestHttpClient();
        afipService = new AfipService(configRepo, cacheRepo, mockHttpClient);
    }

    @AfterEach
    public void tearDown() throws Exception {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    public void testExtraerNombreVariaciones() throws Exception {
        Method method = AfipService.class.getDeclaredMethod("extraerNombre", JsonObject.class);
        method.setAccessible(true);

        JsonObject pf = JsonParser.parseString("{\"datosGenerales\": {\"nombre\": \"Juan\", \"apellido\": \"Perez\"}}").getAsJsonObject();
        assertEquals("Perez Juan", method.invoke(afipService, pf));

        JsonObject emp = JsonParser.parseString("{\"datosGenerales\": {\"razonSocial\": \"Mi Empresa S.A.\"}}").getAsJsonObject();
        assertEquals("Mi Empresa S.A.", method.invoke(afipService, emp));

        JsonObject fallback = JsonParser.parseString("{\"nombre\": \"Ana\", \"apellido\": \"Gomez\"}").getAsJsonObject();
        assertEquals("Gomez Ana", method.invoke(afipService, fallback));
    }

    @Test
    public void testExtraerCuitVariaciones() throws Exception {
        Method method = AfipService.class.getDeclaredMethod("extraerCuit", com.google.gson.JsonElement.class);
        method.setAccessible(true);

        com.google.gson.JsonElement c1 = JsonParser.parseString("{\"idPersonaListReturn\": {\"idPersona\": 20409378472}}");
        assertEquals(20409378472L, method.invoke(afipService, c1));

        com.google.gson.JsonElement c2 = JsonParser.parseString("{\"data\": {\"idPersona\": 27401234567}}");
        assertEquals(27401234567L, method.invoke(afipService, c2));

        com.google.gson.JsonElement c3 = JsonParser.parseString("[23401234569]");
        assertEquals(23401234569L, method.invoke(afipService, c3));
    }

    @Test
    public void testBuscarClientePorDniAsync_CacheHit() throws Exception {
        CompletableFuture<Optional<Cliente>> future = afipService.buscarClientePorDniAsync("40937847");
        Optional<Cliente> resultado = future.get(2, TimeUnit.SECONDS);

        assertTrue(resultado.isPresent());
        assertEquals("Perez Juan", resultado.get().getNombre());

        assertEquals(0, mockHttpClient.requests.size());
    }

    @Test
    public void testBuscarClientePorDniAsync_ApiHit_Success() throws Exception {
        CompletableFuture<Optional<Cliente>> future = afipService.buscarClientePorDniAsync("11111111");
        Optional<Cliente> resultado = future.get(5, TimeUnit.SECONDS);

        assertTrue(resultado.isPresent());
        assertEquals("Messi Leo", resultado.get().getNombre());
        assertEquals("20111111112", resultado.get().getCuit().getValor());

        assertEquals(3, mockHttpClient.requests.size());
    }

    @Test
    public void testBuscarClientePorDniAsync_Concurrency() throws Exception {
        mockHttpClient.slowAuth = true;

        CompletableFuture<Optional<Cliente>> future1 = afipService.buscarClientePorDniAsync("11111111");
        CompletableFuture<Optional<Cliente>> future2 = afipService.buscarClientePorDniAsync("22222222");

        CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS);

        long authRequests = mockHttpClient.requests.stream().filter(r -> r.uri().toString().endsWith("/auth")).count();
        assertEquals(1, authRequests, "Solo deberia haber una petición a /auth por concurrencia");
    }

    @Test
    public void testReintentoAnteError5xx() throws Exception {
        mockHttpClient.serverErrors = 2; // Falla dos veces, a la tercera funciona

        CompletableFuture<Optional<Cliente>> future = afipService.buscarClientePorDniAsync("11111111");
        Optional<Cliente> resultado = future.get(10, TimeUnit.SECONDS);

        assertTrue(resultado.isPresent());
        assertEquals(5, mockHttpClient.requests.size(), "Deberían ser 3 auth requests (2 retry + 1 OK) + 2 de padrón");
    }

    @Test
    public void testBuscarClientePorDniAsync_ApiHit_AuthFails() throws Exception {
        mockHttpClient.failAuth = true;

        CompletableFuture<Optional<Cliente>> future = afipService.buscarClientePorDniAsync("11111111");
        Optional<Cliente> resultado = future.get(5, TimeUnit.SECONDS);

        assertFalse(resultado.isPresent());
        assertEquals(1, mockHttpClient.requests.size());
    }

    @Test
    public void testBuscarClientePorDniAsync_PropagaPersistenceException() throws Exception {
        // Usar un repositorio real que intentará acceder a la base de datos
        ClienteCacheRepository realCacheRepo = new ClienteCacheRepository();
        AfipService serviceConDbReal = new AfipService(configRepo, realCacheRepo, mockHttpClient);

        // Rompemos la tabla cache_afip para provocar un SQLException al buscar en la caché
        try (Connection conn = DatabaseService.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE cache_afip");
        }

        Exception exception = assertThrows(CompletionException.class, () -> {
            serviceConDbReal.buscarClientePorDniAsync("11111111").join();
        });

        Throwable cause = exception.getCause();
        assertTrue(cause instanceof PersistenceException, "La excepción original debe ser propagada");
        assertTrue(cause.getMessage().contains("Error al buscar DNI en cache"));
    }
}
