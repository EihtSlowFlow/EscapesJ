package io.github.ramiro.escapesj.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para conectar a la API REST de AFIP.
 * Utiliza librerías estándar de Java (HttpURLConnection) y GSON para parsear JSON.
 * No requiere un JAR especial del SDK, solo conecta directamente a la API REST.
 */
public class AfipService {

    private final String AFIP_API_BASE = "https://apis.afip.gov.ar";
    private final String ACCESS_TOKEN = System.getenv("AFIP_TOKEN") != null
            ? System.getenv("AFIP_TOKEN")
            : "TU_TOKEN_AQUI";

    private final Gson gson = new Gson();

    /**
     * Valida la existencia real de un CUIT en el padrón de AFIP.
     * Consulta directamente a la API REST de AFIP.
     */
    public Map<String, Object> validarYConsultar(long cuit) {
        try {
            // Endpoint para consultar datos del contribuyente
            // Nota: Ajusta el endpoint según la documentación oficial de AFIP
            String url = AFIP_API_BASE + "/padron/personeria/" + cuit;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parsear JSON con GSON
                JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                Map<String, Object> resultado = gson.fromJson(jsonResponse, Map.class);

                System.out.println("CUIT encontrado: " + resultado);
                return resultado;

            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                System.out.println("CUIT no encontrado en el padrón: " + cuit);
                return null;
            } else {
                System.err.println("Error en la consulta. Código HTTP: " + responseCode);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error al consultar CUIT: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Genera un token de autenticación para la API de AFIP.
     * Esto es un placeholder. Cada servicio de AFIP tiene su propio método de autenticación.
     */
    public String obtenerToken(String cuit, String certificado) {
        try {
            String url = AFIP_API_BASE + "/ws/soap/wsfe";

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/soap+xml");
            connection.setDoOutput(true);

            // Construir SOAP request para autenticación
            String soapRequest = construirSoapAuth(cuit, certificado);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = soapRequest.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Extraer token del response SOAP
            return extraerTokenDelSoap(response.toString());

        } catch (Exception e) {
            System.err.println("Error al obtener token: " + e.getMessage());
            return null;
        }
    }

    private String construirSoapAuth(String cuit, String certificado) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"...\">" +
                // Implementar según documentación de AFIP
                "</soapenv:Envelope>";
    }

    private String extraerTokenDelSoap(String soapResponse) {
        // Implementar parsing del token desde la respuesta SOAP
        return null;
    }

    /**
     * Busca un cliente por DNI consultando la API de AFIP.
     * Intenta primero con prefijo 20 (hombre) y luego con 27 (mujer).
     */
    public void buscarClientePorDNI(String dni) {
        long cuitHombre = calcularCUIT(dni, 20);
        long cuitMujer = calcularCUIT(dni, 27);

        // 1. Intentamos buscar como CUIT de hombre
        Map<String, Object> datos = this.validarYConsultar(cuitHombre);

        if (datos != null) {
            completarCampos(datos);
            return;
        }

        // 2. Si falla, intentamos como mujer
        datos = this.validarYConsultar(cuitMujer);

        if (datos != null) {
            completarCampos(datos);
            return;
        }

        // 3. Si ambos fallan, el cliente no está en el padrón o es extranjero/caso especial
        System.out.println("No se encontraron datos. Carga manual habilitada.");
    }

    /**
     * Calcula el CUIT a partir de un DNI.
     * Usa el algoritmo estándar de Argentina: prepend prefijo y calcula dígito verificador.
     */
    private long calcularCUIT(String dni, int prefijo) {
        try {
            long dniNum = Long.parseLong(dni.trim());

            // Construir el número sin el dígito verificador: prefijo + DNI
            String numeroParcial = prefijo + String.format("%08d", dniNum);

            // Calcular dígito verificador
            int digitoVerificador = calcularDigitoVerificador(numeroParcial);

            // Retornar el CUIT completo
            String cuitCompleto = numeroParcial + digitoVerificador;

            // Validar que es matemáticamente correcto
            if (ValidadorCUIT.esMatematicamenteValido(cuitCompleto)) {
                return Long.parseLong(cuitCompleto);
            } else {
                System.err.println("CUIT calculado no es válido: " + cuitCompleto);
                return 0;
            }

        } catch (NumberFormatException e) {
            System.err.println("Error al calcular CUIT: DNI inválido");
            return 0;
        }
    }

    /**
     * Calcula el dígito verificador del CUIT usando el algoritmo estándar AFIP.
     */
    private int calcularDigitoVerificador(String numeroParcial) {
        int[] multiplicadores = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;

        for (int i = 0; i < numeroParcial.length(); i++) {
            int digito = Character.getNumericValue(numeroParcial.charAt(i));
            suma += digito * multiplicadores[i];
        }

        int residuo = suma % 11;
        if (residuo == 0) return 0;
        if (residuo == 1) return 9;
        return 11 - residuo;
    }

    /**
     * Completa los campos de la ventana con los datos del AFIP.
     */
    private void completarCampos(Map<String, Object> datos) {
        System.out.println("Datos del AFIP:");
        for (Map.Entry<String, Object> entry : datos.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        // TODO: Implementar lógica para rellenar los campos de la UI
    }
}
