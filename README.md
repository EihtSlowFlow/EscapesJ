# EscapesJ 🚗💨

**EscapesJ** es un sistema integral de gestión y facturación diseñado específicamente para talleres de escapes y servicios automotrices. La aplicación ofrece un entorno de escritorio completo (construido en Java Swing) que maneja inventario, historial de servicios, presupuestos dinámicos e integración directa con los WebServices de AFIP para facturación electrónica.

---

## 🌟 Características Principales

*   **📦 Gestión de Inventario:**
    *   Control exhaustivo de stock de productos y repuestos.
    *   Creación, lectura, actualización y eliminación de artículos.
    *   Sincronización automática: el stock se descuenta al generar facturas y se restaura si se eliminan ítems de una orden en curso.

*   **📄 Facturación y Generación de Presupuestos (PDF):**
    *   Sistema multi-ítem para registrar ventas y mano de obra simultáneamente.
    *   Generación de comprobantes y presupuestos en formato PDF con la librería **OpenPDF**.
    *   Selector interactivo de fechas (JDateChooser) para establecer la validez de los presupuestos, con bloqueos de seguridad para fechas vencidas.

*   **🇦🇷 Integración con AFIP (Facturación Electrónica):**
    *   Conexión con el Webservice de Facturación Electrónica (WSFE) de AFIP.
    *   Autocompletado de datos de clientes consultando CUIT/DNI directamente a los servidores de AFIP.
    *   Soporte nativo para Entornos de Prueba (Homologación) y Producción.

*   **📊 Historial de Servicios:**
    *   Registro persistente de todos los trabajos realizados.
    *   Búsqueda de historial de vehículos para fácil seguimiento del mantenimiento.

*   **⚙️ Configuración y Panel de Control:**
    *   Modificación sencilla del entorno de AFIP y carga de certificados sin necesidad de tocar código.
    *   Gestión de administradores y opciones de seguridad.

---

## 🛠️ Stack Tecnológico

*   **Lenguaje:** Java 17
*   **Interfaz Gráfica:** Java Swing (con Look & Feel oscuro personalizado)
*   **Base de Datos:** SQLite (Base de datos local embebida, `sqlite-jdbc`)
*   **PDF:** OpenPDF (`com.github.librepdf:openpdf`)
*   **JSON & APIs:** Gson y Jackson (`jackson-databind`)
*   **Componentes UI Extra:** JCalendar (`com.toedter:jcalendar`)
*   **Testing:** JUnit 5 (`junit-jupiter`)

---

## 🚀 Instalación y Uso

### Prerrequisitos
*   Tener instalado **Java Development Kit (JDK) 17** o superior.
*   Tener **Maven** instalado para la resolución de dependencias.

### Pasos para Ejecutar
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/EihtSlowFlow/EscapesJ.git
   cd EscapesJ
   ```
2. Compilar el proyecto y descargar dependencias:
   ```bash
   mvn clean compile
   ```
3. Ejecutar la aplicación:
   ```bash
   mvn exec:java -Dexec.mainClass="io.github.ramiro.escapesj.main.Principal"
   ```

### Generación del Ejecutable (Fat JAR)
Puedes empaquetar toda la aplicación junto a sus dependencias en un único archivo `.jar` ejecutable:
```bash
mvn package
```
El archivo resultante se ubicará en `target/escapesJ-1.0-SNAPSHOT.jar` y se puede ejecutar con doble clic o mediante:
```bash
java -jar target/escapesJ-1.0-SNAPSHOT.jar
```

---

## 🔐 Certificados de AFIP (Nota de Seguridad)

El sistema requiere certificados criptográficos (`.key` y `.crt`) para comunicarse con la AFIP. 
Por razones de seguridad, **estos archivos NUNCA se suben al repositorio**. 
* Si vas a implementar el proyecto, deberás generar tu propio `.csr`, obtener el `.crt` en la página de AFIP y colocarlos en la carpeta `afip-cert/` localmente.
* La carpeta `afip-cert/` y los archivos `config.properties` están excluidos mediante el `.gitignore`.

---
*Desarrollado para agilizar la administración y potenciar el flujo de trabajo en el taller.* 🛠️
