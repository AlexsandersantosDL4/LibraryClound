# Script PowerShell para compilar e rodar a LibraryClound
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "      INICIANDO SISTEMA LIBRARYCLOUND (BACK-END)         " -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan

$javac = "javac"
$java = "java"

$intellijJdk = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\jbr\bin"
if (-not (Get-Command javac -ErrorAction SilentlyContinue) -and (Test-Path "$intellijJdk\javac.exe")) {
    $javac = "$intellijJdk\javac.exe"
    $java = "$intellijJdk\java.exe"
}

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "[1/3] Compilando arquivos Java com Java 21 e SQLite..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& $javac --release 21 -encoding UTF-8 -cp "lib/*" -d bin $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Falha na compilação!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[2/3] Copiando recursos do frontend..." -ForegroundColor Yellow
if (-not (Test-Path "bin\public")) {
    New-Item -ItemType Directory -Path "bin\public" | Out-Null
}
Copy-Item -Path "src\main\resources\public\*" -Destination "bin\public" -Recurse -Force

Write-Host "[3/3] Iniciando Servidor HTTP e Banco SQLite..." -ForegroundColor Green
Write-Host "Acesse no seu navegador: http://localhost:8080" -ForegroundColor Cyan
& $java -cp "bin;lib/*" com.livraria.Main
