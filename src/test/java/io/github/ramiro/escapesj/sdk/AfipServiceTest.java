package io.github.ramiro.escapesj.sdk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ramiro.escapesj.modelo.Cliente;
import io.github.ramiro.escapesj.persistencia.ClienteCacheRepository;
import io.github.ramiro.escapesj.persistencia.ConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AfipServiceTest {

    private AfipService afipService;
    private ClienteCacheRepository cacheRepo;

    @BeforeEach
    public void setup() throws Exception {
        ConfigRepository configRepo = new ConfigRepository() {
            @Override
            public void guardar(String clave, String valor) {}
            @Override
            public Optional<String> obtener(String clave) { return Optional.empty(); }
        };

        cacheRepo = new ClienteCacheRepository() {
            @Override
            public int limpiarExpirados() { return 0; }
            @Override
            public void guardar(String dni, String nombre, String cuit, String prefijoCuit) {}
            @Override
            public Optional<EntradaCache> buscarPorDni(String dni) {
                if ("40937847".equals(dni)) {
                    return Optional.of(new EntradaCache("40937847", "Perez Juan", "20409378472", "20", "2026-07-28"));
                }
                return Optional.empty();
            }
        };

        afipService = new AfipService(configRepo, cacheRepo);
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
        
        Field nombreField = Cliente.class.getDeclaredField("nombre");
        nombreField.setAccessible(true);
        String nombre = (String) nombreField.get(resultado.get());
        
        assertEquals("Perez Juan", nombre);
    }

    @Test
    public void testHttpClientEsReutilizadoPorInstancia() throws Exception {
        Field httpClient = AfipService.class.getDeclaredField("httpClient");
        assertFalse(java.lang.reflect.Modifier.isStatic(httpClient.getModifiers()));

        httpClient.setAccessible(true);
        assertSame(httpClient.get(afipService), httpClient.get(afipService));
    }

    @Test
    public void testBuscarSoloEnCacheAsync_CacheHit() throws Exception {
        Optional<Cliente> resultado = afipService.buscarSoloEnCacheAsync("40937847")
                .get(2, TimeUnit.SECONDS);
        assertTrue(resultado.isPresent());
    }
}
