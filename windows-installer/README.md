# EscapesJ - Creador de Instalador de Windows

Esta carpeta contiene los archivos necesarios para empaquetar EscapesJ en un instalador `.exe` utilizando **NSIS (Nullsoft Scriptable Install System)** y **jlink** (para embeber un entorno de ejecución de Java ligero).

Al embeber un JRE, **el cliente final NO necesita tener Java instalado en su computadora** para poder ejecutar el programa.

## Requisitos previos (Solo para el desarrollador)
1. Estar usando **Windows**.
2. Tener instalado **NSIS** (https://nsis.sourceforge.io/).
3. Tener un **JDK 17 o superior** instalado y la variable de entorno `PATH` apuntando a la carpeta `bin` de este JDK (debes poder ejecutar `jlink` desde la terminal).

## Pasos para crear el Instalador

### 1. Construir el JAR actualizado (Si hay cambios en el código)
Primero asegurate de tener la última versión compilada del proyecto. Desde la raíz del proyecto (fuera de esta carpeta) ejecutá:
```bash
mvn clean package
```
Copiá el archivo `.jar` generado (ej: `target/escapesJ-1.0-SNAPSHOT-shaded.jar`) a esta carpeta y renombralo como **`EscapesJ.jar`** (reemplazando al antiguo).

### 2. Generar el JRE Embebido
Haz doble clic sobre el archivo **`generar_runtime.bat`**.
Este script usará `jlink` para crear una carpeta llamada `runtime` con un JRE minimizado que solo contiene los módulos de Java que nuestro programa necesita.

### 3. Compilar el Instalador NSIS
Una vez que veas la carpeta `runtime` creada, haz clic derecho sobre el archivo **`EscapesJ.nsi`** y selecciona **"Compile NSIS Script"**. 
Esto generará el instalador final en la ruta `../installer-output/EscapesJ_Instalador.exe`.

---

## Entregable
El archivo que debes enviarle a tu cliente final es **`EscapesJ_Instalador.exe`**.
El cliente solo tendrá que hacer "Siguiente > Siguiente > Instalar" y el programa se ejecutará perfectamente, sin importar si tienen o no Java en sus computadoras.
