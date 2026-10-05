<#
  Publica UN cambio en GitHub: 1 modificacion = 1 commit = 1 push.

  Uso:
    .\publicar.ps1 -Mensaje "fix(editor): corrige el zoom con dos dedos" -Archivos app/src/main/java/com/pixelados/ui/components/PixelCanvasView.kt
    .\publicar.ps1 -Mensaje "docs: actualiza el README" -Archivos README.md -Cuerpo "Detalle opcional del cambio"
    .\publicar.ps1 -Mensaje "fix: dos archivos" -Archivos README.md app/build.gradle.kts

  Los archivos se pueden separar con espacios o comas: -Archivos uno.kt, dos.kt

  Si no se pasan archivos, publica lo que ya este preparado con `git add`.
  Con -SinPush se hace solo el commit (para revisarlo antes de subirlo).

  Si GitHub rechaza el push porque hay commits nuevos en el remoto (por ejemplo,
  porque editaste algo por la web), el script integra esos cambios con rebase y
  vuelve a intentar el push una sola vez.
#>
param(
    [Parameter(Mandatory = $true)][string]$Mensaje,
    [string[]]$Archivos,
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$ArchivosExtra,
    [string]$Cuerpo = "",
    [switch]$SinPush
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# Permite escribir `-Archivos uno.kt dos.kt` (PowerShell los manda como restos)
$Archivos = @(@($Archivos) + @($ArchivosExtra) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })

# 1) Preparar unicamente los archivos de este cambio
if ($Archivos.Count -gt 0) {
    foreach ($ruta in $Archivos) {
        if (-not (Test-Path -LiteralPath $ruta)) {
            Write-Host "No existe: $ruta" -ForegroundColor Red
            exit 1
        }
        git add -- $ruta
    }
}

$preparados = @(git diff --cached --name-only)
if ($preparados.Count -eq 0) {
    Write-Host "No hay nada preparado para publicar. Usa -Archivos <rutas> o 'git add' primero." -ForegroundColor Yellow
    exit 1
}

Write-Host "Este commit incluye $($preparados.Count) archivo(s):" -ForegroundColor Cyan
$preparados | ForEach-Object { Write-Host "   $_" }

# 2) Commit
if ([string]::IsNullOrWhiteSpace($Cuerpo)) {
    git commit -m $Mensaje
} else {
    git commit -m $Mensaje -m $Cuerpo
}
if ($LASTEXITCODE -ne 0) {
    Write-Host "El commit no se pudo crear." -ForegroundColor Red
    exit 1
}

# 3) Push (uno por commit)
if (-not $SinPush) {
    git push origin main
    if ($LASTEXITCODE -ne 0) {
        Write-Host "El remoto tiene commits nuevos; se integran con rebase y se reintenta el push..." -ForegroundColor Yellow
        git pull --rebase origin main
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Hay conflictos: hay que resolverlos a mano antes de publicar." -ForegroundColor Red
            exit 1
        }
        git push origin main
        if ($LASTEXITCODE -ne 0) { exit 1 }
    }
}

# 4) Resumen
$publicado = git log -1 --pretty=format:"%h  %ad  %s" --date=format:"%Y-%m-%d %H:%M"
Write-Host ""
Write-Host "Publicado en GitHub: $publicado" -ForegroundColor Green
