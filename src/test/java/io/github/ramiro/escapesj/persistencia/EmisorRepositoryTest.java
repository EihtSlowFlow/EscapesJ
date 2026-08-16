package io.github.ramiro.escapesj.persistencia;

import io.github.ramiro.escapesj.modelo.Emisor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmisorRepositoryTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseService.reiniciarTest();
        DatabaseService.setCustomDbUrl("jdbc:sqlite:" + tempDir.resolve("emisores.db"));
        DatabaseService.inicializar();
    }

    @AfterEach
    void tearDown() {
        DatabaseService.setCustomDbUrl(null);
        DatabaseService.reiniciarTest();
    }

    @Test
    void guardaYListaElEmisorConSuIdGenerado() {
        EmisorRepository repository = new EmisorRepository();

        Emisor guardado = repository.guardar(
                new Emisor(0, "Taller Sur", "20123456789", "Mitre 123", "2920123456"));

        assertTrue(guardado.id() > 0);
        assertEquals("Taller Sur", guardado.nombre());
        assertEquals(1, repository.listarTodos().size());
        assertEquals(guardado, repository.listarTodos().get(0));
    }

    @Test
    void propagaElErrorCuandoNoPuedeGuardar() {
        DatabaseService.setCustomDbUrl("jdbc:inexistente");

        assertThrows(PersistenceException.class, () -> new EmisorRepository().guardar(
                new Emisor(0, "Taller Sur", "20123456789", "", "")));
    }
}
