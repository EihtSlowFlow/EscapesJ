; ============================================================
; EscapesJ - Instalador para Windows (NSIS)
; ============================================================

!include "MUI2.nsh"

; --- CONFIGURACIÓN GENERAL ---
Name "EscapesJ"
OutFile "../installer-output/EscapesJ_Instalador.exe"
InstallDir "$PROGRAMFILES\EscapesJ"
InstallDirRegKey HKLM "Software\EscapesJ" "InstallDir"
RequestExecutionLevel admin

; --- ÍCONO ---
!define MUI_ICON "escapesj.ico"
!define MUI_UNICON "escapesj.ico"

; --- INTERFAZ MODERNA ---
!define MUI_ABORTWARNING
!define MUI_WELCOMEPAGE_TITLE "Bienvenido al Instalador de EscapesJ"
!define MUI_WELCOMEPAGE_TEXT "Este asistente instalará EscapesJ en tu computadora.$\r$\n$\r$\nEscapesJ es un sistema de gestión integral para talleres de escapes y servicios automotrices.$\r$\n$\r$\nIMPORTANTE: Se requiere Java 17 o superior.$\r$\nSi no lo tenés, descargalo de https://adoptium.net/$\r$\n$\r$\nHacé clic en Siguiente para continuar."

!define MUI_FINISHPAGE_RUN "$INSTDIR\EscapesJ.bat"
!define MUI_FINISHPAGE_RUN_TEXT "Ejecutar EscapesJ ahora"
!define MUI_FINISHPAGE_TITLE "Instalación Completada"
!define MUI_FINISHPAGE_TEXT "EscapesJ fue instalado correctamente.$\r$\n$\r$\nPodés ejecutarlo desde el Menú Inicio o el acceso directo en el Escritorio."

; --- PÁGINAS ---
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "LICENCIA.txt"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

; --- PÁGINAS DEL DESINSTALADOR ---
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

; --- IDIOMA ---
!insertmacro MUI_LANGUAGE "Spanish"

; ============================================================
; SECCIÓN PRINCIPAL DE INSTALACIÓN
; ============================================================
Section "Instalar EscapesJ" SecMain
    SectionIn RO ; Obligatoria

    SetOutPath "$INSTDIR"

    ; Copiar archivos de la aplicación
    File "EscapesJ.jar"
    File "EscapesJ.bat"
    File "escapesj.ico"

    ; Copiar el JRE embebido (debe ser generado antes con generar_runtime.bat)
    SetOutPath "$INSTDIR\runtime"
    File /r "runtime\*"
    SetOutPath "$INSTDIR"

    ; Guardar ubicación de instalación en el registro
    WriteRegStr HKLM "Software\EscapesJ" "InstallDir" "$INSTDIR"

    ; Crear desinstalador
    WriteUninstaller "$INSTDIR\Desinstalar.exe"

    ; Agregar entrada en "Agregar o quitar programas"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EscapesJ" \
        "DisplayName" "EscapesJ - Sistema de Gestión"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EscapesJ" \
        "UninstallString" "$\"$INSTDIR\Desinstalar.exe$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EscapesJ" \
        "DisplayIcon" "$INSTDIR\escapesj.ico"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EscapesJ" \
        "Publisher" "EscapesJ"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EscapesJ" \
        "DisplayVersion" "1.0"
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EscapesJ" \
        "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EscapesJ" \
        "NoRepair" 1

    ; Crear accesos directos en el Menú Inicio
    CreateDirectory "$SMPROGRAMS\EscapesJ"
    CreateShortCut "$SMPROGRAMS\EscapesJ\EscapesJ.lnk" "$INSTDIR\EscapesJ.bat" "" "$INSTDIR\escapesj.ico" 0
    CreateShortCut "$SMPROGRAMS\EscapesJ\Desinstalar EscapesJ.lnk" "$INSTDIR\Desinstalar.exe"

    ; Crear acceso directo en el Escritorio
    CreateShortCut "$DESKTOP\EscapesJ.lnk" "$INSTDIR\EscapesJ.bat" "" "$INSTDIR\escapesj.ico" 0

SectionEnd

; ============================================================
; SECCIÓN DE DESINSTALACIÓN
; ============================================================
Section "Uninstall"

    ; Eliminar archivos instalados
    Delete "$INSTDIR\EscapesJ.jar"
    Delete "$INSTDIR\EscapesJ.bat"
    Delete "$INSTDIR\escapesj.ico"
    Delete "$INSTDIR\Desinstalar.exe"
    
    ; Eliminar JRE embebido
    RMDir /r "$INSTDIR\runtime"

    ; Eliminar la carpeta de instalación (solo si está vacía)
    RMDir "$INSTDIR"

    ; Eliminar accesos directos
    Delete "$SMPROGRAMS\EscapesJ\EscapesJ.lnk"
    Delete "$SMPROGRAMS\EscapesJ\Desinstalar EscapesJ.lnk"
    RMDir "$SMPROGRAMS\EscapesJ"
    Delete "$DESKTOP\EscapesJ.lnk"

    ; Eliminar entradas del registro
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EscapesJ"
    DeleteRegKey HKLM "Software\EscapesJ"

SectionEnd
