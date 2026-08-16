package io.github.ramiro.escapesj.persistencia;

import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Cache local de consultas a AFIP.
 * Almacena el resultado de buscar un DNI en el padrón para evitar
 * llamadas repetidas a la API.
 *
 * Por defecto las entradas expiran después de 30 días.
 */
public class ClienteCacheRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClienteCacheRepository.class);

    private static final int DIAS_EXPIRACION = diasHastaFinDeMes();

    /**
     * Calcula cuántos días faltan hasta fin de mes para usar como expiración.
     * Mínimo 7 días para que no expire demasiado rápido a fin de mes.
     */
    private static int diasHastaFinDeMes() {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        int diasRestantes = hoy.lengthOfMonth() - hoy.getDayOfMonth();
        return Math.max(diasRestantes, 7);
    }

    public ClienteCacheRepository() {
    }

    /**
     * Busca un DNI en el cache local.
     * Retorna los datos si existen y no están expirados.
     */
    public Optional<EntradaCache> buscarPorDni(String dni) {
        String sql = "SELECT dni, nombre, cuit, prefijo_cuit, fecha_cache FROM cache_afip WHERE dni = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dni);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                EntradaCache entrada = new EntradaCache(
                        rs.getString("dni"),
                        rs.getString("nombre"),
                        rs.getString("cuit"),
                        rs.getString("prefijo_cuit"),
                        rs.getString("fecha_cache")
                );

                // Verificar expiración
                if (entrada.estaVigente(DIAS_EXPIRACION)) {
                    return Optional.of(entrada);
                } else {
                    // Cache expirado, eliminarlo
                    eliminar(dni);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al buscar DNI en cache", e);
        }
        return Optional.empty();
    }

    /**
     * Guarda o actualiza una entrada en el cache.
     */
    public void guardar(String dni, String nombre, String cuit, String prefijoCuit) {
        String sql = "INSERT INTO cache_afip (dni, nombre, cuit, prefijo_cuit, fecha_cache) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(dni) DO UPDATE SET nombre=excluded.nombre, cuit=excluded.cuit, " +
                "prefijo_cuit=excluded.prefijo_cuit, fecha_cache=excluded.fecha_cache";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dni);
            ps.setString(2, nombre);
            ps.setString(3, cuit);
            ps.setString(4, prefijoCuit);
            ps.setString(5, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al guardar DNI en cache", e);
        }
    }

    /**
     * Elimina una entrada del cache (por expiración o limpieza manual).
     */
    public void eliminar(String dni) {
        String sql = "DELETE FROM cache_afip WHERE dni = ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dni);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al eliminar DNI del cache", e);
        }
    }

    /**
     * Limpia todas las entradas expiradas del cache.
     */
    public int limpiarExpirados() {
        String sql = "DELETE FROM cache_afip WHERE fecha_cache < ?";
        try (Connection connection = DatabaseService.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String limite = LocalDateTime.now()
                    .minusDays(DIAS_EXPIRACION)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            ps.setString(1, limite);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error:", e);
            throw new PersistenceException("Error al limpiar expirados", e);
        }
    }

    /**
     * Registro inmutable de una entrada de cache.
     */
    public record EntradaCache(String dni, String nombre, String cuit, String prefijoCuit, String fechaCache) {

        /**
         * Verifica si esta entrada aún es válida según los días de expiración.
         */
        public boolean estaVigente(int diasExpiracion) {
            try {
                LocalDateTime fecha = LocalDateTime.parse(fechaCache, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return ChronoUnit.DAYS.between(fecha, LocalDateTime.now()) < diasExpiracion;
            } catch (Exception e) {
                // Si no se puede parsear la fecha, considerarla expirada
                return false;
            }
        }
    }
}
