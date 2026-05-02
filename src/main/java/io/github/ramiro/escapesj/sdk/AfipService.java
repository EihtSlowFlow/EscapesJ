package io.github.ramiro.escapesj.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

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
            System.err.println("Error: No se encontró config.properties. Intentando variables de entorno.");
            this.afipToken = System.getenv("AFIP_TOKEN");
            this.afipBaseUrl = "https://apis.afip.gov.ar";
        }
    }

    public Map<String, Object> consultarPadron(String cuit) {
        if (afipToken == null || afipToken.isEmpty()) {
            System.err.println("Error: Token de AFIP no configurado.");
            return null;
        }
        try {
            String urlStr = afipBaseUrl + "/padron/personeria/" + cuit;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + afipToken);

            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();

                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                // Ahora 'gson' ya existe y no dará error
                return gson.fromJson(json, Map.class);
            } else {
                System.err.println("Respuesta AFIP no exitosa: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            System.err.println("Error consultando CUIT " + cuit + ": " + e.getMessage());
        }
        return null;
    }

    public Map<String, Object> buscarPorDni(String dni) {
        String[] prefijosPersonaFisica = {"20", "27", "23", "24"};

        for (String prefijo : prefijosPersonaFisica) {
            String cuitPosible = ValidadorCUIT.construirCuit(dni, prefijo);
            System.out.println("Probando con CUIT: " + cuitPosible);

            Map<String, Object> resultado = consultarPadron(cuitPosible);

            if (resultado != null) {
                System.out.println("¡Encontrado con prefijo " + prefijo + "!");
                return resultado;
            }
        }
        System.out.println("No se encontró el DNI en el padrón con ningún prefijo común.");
        return null;
    }
}