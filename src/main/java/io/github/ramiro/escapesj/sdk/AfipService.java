package io.github.ramiro.escapesj.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ramiro.escapesj.modelo.Cliente;
import io.github.ramiro.escapesj.modelo.Cuit;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Stream;

public class AfipService {

    private String afipToken;
    private String afipBaseUrl;
    private final Properties properties = new Properties();
    private final Gson gson = new Gson();

    public AfipService() {
        cargarConfiguracion();
    }

    private void cargarConfiguracion() {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);
            this.afipToken = properties.getProperty("afip.token");
            this.afipBaseUrl = properties.getProperty("afip.base_url");
        } catch (IOException e) {
            System.err.println("Advertencia: No se pudo cargar config.properties. Verifique su existencia.");
            this.afipToken = ""; // Evita nulls posteriores
            this.afipBaseUrl = "https://apis.afip.gov.ar";
        }
    }

    public Optional<Cliente> buscarClientePorDni(String dni) {
        return Stream.of("20", "27", "23", "24")
                .map(prefijo -> Cuit.intentarCrear(dni, prefijo))
                .flatMap(Optional::stream)
                .map(this::consultarPadron) // Retorna Optional<Map>
                .flatMap(Optional::stream)
                .map(datos -> {
                    // El Service extrae los datos y construye el objeto del Modelo
                    String nombreCompleto = datos.getOrDefault("nombre", "Sin Nombre").toString();
                    // Aquí podrías separar nombre y apellido si la API lo permite
                    return Cuit.intentarCrear(dni, "20") // O el prefijo exitoso
                            .map(c -> new Cliente(nombreCompleto, c));
                })
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<Map<String, Object>> consultarPadron(Cuit cuit) {
        return cuit.transformar(valorCuit -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(afipBaseUrl + "/padron/personeria/" + valorCuit);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + afipToken);
                conn.setConnectTimeout(3000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return leerRespuesta(conn);
                }
            } catch (Exception e) {
                System.err.println("Error de red consultando CUIT " + valorCuit + ": " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
            return Optional.<Map<String, Object>>empty();
        });
    }

    private Optional<Map<String, Object>> leerRespuesta(HttpURLConnection conn) throws IOException {
        try (Scanner scanner = new Scanner(conn.getInputStream())) {
            String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            if (response.isEmpty()) return Optional.empty();

            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = gson.fromJson(jsonObject, Map.class);
            return Optional.ofNullable(mapa);
        }
    }
}