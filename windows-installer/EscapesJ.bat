@echo off
title EscapesJ - Sistema de Gestion
cd /d "%~dp0"

REM Buscar JRE embebido primero
if exist "runtime\bin\javaw.exe" (
    start "" "runtime\bin\javaw.exe" -jar EscapesJ.jar
    exit
)

REM Buscar Java en el PATH del sistema
where java >nul 2>&1
if %ERRORLEVEL% equ 0 (
    start "" javaw -jar EscapesJ.jar
    exit
)

REM Buscar en ubicaciones comunes de Java
for %%J in (
    "%ProgramFiles%\Java"
    "%ProgramFiles(x86)%\Java"
    "%ProgramFiles%\Eclipse Adoptium"
    "%ProgramFiles%\Temurin"
    "%LOCALAPPDATA%\Programs\Eclipse Adoptium"
) do (
    if exist "%%~J" (
        for /d %%D in ("%%~J\*") do (
            if exist "%%~D\bin\javaw.exe" (
                start "" "%%~D\bin\javaw.exe" -jar EscapesJ.jar
                exit
            )
        )
    )
)

REM Si no se encuentra Java, mostrar mensaje
echo.
echo ============================================
echo   ERROR: Java no fue encontrado.
echo.
echo   EscapesJ requiere Java 17 o superior.
echo   Descargalo desde:
echo   https://adoptium.net/
echo ============================================
echo.
pause
