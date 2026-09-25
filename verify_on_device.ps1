# Verificacion en dispositivo real de Pixelados.
#
# Requisitos:
#   1. El telefono conectado por USB con "Depuracion USB" activada.
#   2. Aceptar el dialogo "¿Permitir depuracion USB?" que aparece en el telefono.
#
# Uso:  powershell -ExecutionPolicy Bypass -File .\verify_on_device.ps1
#
# Hace tres cosas:
#   - instala el APK compilado,
#   - ejecuta las pruebas instrumentadas (editor, tamaños de lienzo, detalle),
#   - abre la app en cada pantalla clave y guarda capturas en build\screenshots.
#
# OJO: Gradle desinstala la app al terminar las pruebas instrumentadas, así que
# se borran los lienzos guardados en el teléfono. Si tienes dibujos que quieras
# conservar, expórtalos antes a la galería (Pictures/Pixelados).
$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Users\lgonz\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:PATH"

$adb = "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
$apk = "app\build\outputs\apk\debug\pixelados.apk"
$shots = "build\screenshots"

function Get-DeviceState {
    $out = & $adb devices | Select-String -Pattern "^\S+\s+(\S+)"
    if (-not $out) { return "sin-dispositivo" }
    return ($out.Matches[0].Groups[1].Value)
}

$state = Get-DeviceState
if ($state -ne "device") {
    Write-Host ""
    Write-Host "El telefono no esta listo (estado: $state)." -ForegroundColor Yellow
    Write-Host "  1) Reconecta el cable USB."
    Write-Host "  2) En el telefono: Ajustes > Opciones de desarrollador > Depuracion USB (activada)."
    Write-Host "  3) Acepta el aviso '¿Permitir depuracion USB?' en la pantalla del telefono."
    Write-Host "  4) Vuelve a ejecutar este script."
    exit 1
}

Write-Host "Compilando APK de depuracion..." -ForegroundColor Cyan
.\gradlew.bat assembleDebug -x installPixeladosApk --no-daemon

Write-Host "Instalando en el telefono..." -ForegroundColor Cyan
& $adb install -r -d $apk

Write-Host "Ejecutando pruebas instrumentadas..." -ForegroundColor Cyan
.\gradlew.bat connectedDebugAndroidTest --no-daemon

New-Item -ItemType Directory -Force $shots | Out-Null

Write-Host "Abriendo la app y tomando capturas..." -ForegroundColor Cyan
& $adb shell am force-stop com.pixelados
& $adb shell am start -n "com.pixelados/.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
Start-Sleep -Seconds 5
& $adb exec-out screencap -p > "$shots\01_inicio.png"

Write-Host ""
Write-Host "Listo. Capturas en $shots" -ForegroundColor Green
Write-Host "Para capturar el editor: abre la app, crea un lienzo y ejecuta:"
Write-Host "  & `"$adb`" exec-out screencap -p > `"$shots\02_editor.png`""
Write-Host "Para ver solo el reporte de pruebas: app\build\reports\androidTests\connected\index.html"
