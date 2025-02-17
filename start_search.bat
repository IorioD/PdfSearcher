@echo off
REM Esegue il file JAR generato
echo Avvio applicazione...
call java -jar .\target\pdf-search.jar
if %ERRORLEVEL% NEQ 0 (
    echo Errore durante l'esecuzione dell'applicazione.
    pause
    exit /b
)