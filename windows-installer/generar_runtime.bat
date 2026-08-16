@echo off
title Generar JRE Embebido para EscapesJ
cd /d "%~dp0"

echo ===================================================
echo   Generador de JRE Embebido usando jlink
echo ===================================================
echo.
echo Este script creara una carpeta "runtime" con un JRE
echo optimizado para incluir en el instalador de NSIS.
echo.
echo Requisitos:
echo  1. Estar ejecutando este script en WINDOWS.
echo  2. Tener el JDK 17+ instalado y configurado en el PATH.
echo.
pause

REM Verificar si jlink existe
where jlink >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] No se encontro 'jlink'. Asegurate de tener el JDK instalado y en el PATH.
    pause
    exit /b 1
)

REM Eliminar el runtime anterior si existe
if exist "runtime" (
    echo Eliminando runtime anterior...
    rmdir /s /q "runtime"
)

echo Generando nuevo JRE en la carpeta "runtime"...
REM Modulos necesarios: java.base, java.desktop, java.sql, java.naming, java.net.http, java.security.jgss, java.management, java.instrument, jdk.unsupported, java.xml, jdk.crypto.ec, jdk.crypto.cryptoki, java.prefs
jlink --add-modules java.base,java.desktop,java.sql,java.naming,java.net.http,java.security.jgss,java.management,java.instrument,jdk.unsupported,java.xml,jdk.crypto.ec,jdk.crypto.cryptoki,java.prefs --output runtime --no-header-files --no-man-pages --strip-debug --compress=2

if %ERRORLEVEL% equ 0 (
    echo.
    echo [EXITO] JRE embebido generado correctamente en la carpeta "runtime".
    echo Ahora puedes compilar el instalador con NSIS (EscapesJ.nsi).
) else (
    echo.
    echo [ERROR] Hubo un problema al generar el JRE embebido.
)
pause
