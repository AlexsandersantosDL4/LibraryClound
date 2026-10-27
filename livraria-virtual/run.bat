@echo off
chcp 65001 >nul
echo =========================================================
echo       INICIANDO SISTEMA LIBRARYCLOUND (BACK-END)
echo =========================================================

set JAVAC_CMD=javac
set JAVA_CMD=java

where javac >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\jbr\bin\javac.exe" (
        set "JAVAC_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\jbr\bin\javac.exe"
        set "JAVA_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\jbr\bin\java.exe"
    )
)

if not exist bin mkdir bin

echo [1/3] Compilando arquivos Java com Java 21...
dir /s /B src\main\java\*.java > sources.txt
"%JAVAC_CMD%" --release 21 -encoding UTF-8 -cp "lib/*" -d bin @sources.txt
del sources.txt >nul 2>&1

if %ERRORLEVEL% NEQ 0 (
    echo [ERRO] Falha na compilação do código Java!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Copiando arquivos estáticos do frontend...
if not exist "bin\public" mkdir "bin\public"
xcopy /E /I /Y "src\main\resources\public" "bin\public" >nul

echo [3/3] Iniciando Servidor HTTP e Banco SQLite...
echo Acesse em seu navegador: http://localhost:8080
echo Pressione Ctrl+C nesta janela para parar o servidor.
echo ---------------------------------------------------------
"%JAVA_CMD%" -cp "bin;lib/*" com.livraria.Main
pause
