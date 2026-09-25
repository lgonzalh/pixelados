$ErrorActionPreference = "Stop"

Write-Host "Configurando variables de entorno..."
$env:JAVA_HOME = "C:\Users\lgonz\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:PATH"

Write-Host "Incrementando version en build.gradle.kts..."
$buildGradle = "app\build.gradle.kts"
$content = Get-Content $buildGradle
$newContent = @()
$versionCode = 1

foreach ($line in $content) {
    if ($line -match 'versionCode\s*=\s*(\d+)') {
        $versionCode = [int]$matches[1] + 1
        $newContent += "        versionCode = $versionCode"
    } else {
        $newContent += $line
    }
}
Set-Content -Path $buildGradle -Value $newContent

Write-Host "Nueva version compilada: $versionCode"

Write-Host "Compilando APK..."
.\gradlew.bat assembleDebug --no-daemon

$apkPath = "app\build\outputs\apk\debug\pixelados.apk"

if (Test-Path $apkPath) {
    Write-Host "Instalando APK en dispositivo..."
    adb install -r $apkPath
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Lanzando aplicación..."
        adb shell am start -n "com.pixelados/.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
        Write-Host "Proceso completado exitosamente!"
    } else {
        Write-Host "Error instalando APK."
    }
} else {
    Write-Host "Error: No se encontro el APK generado en $apkPath"
}
