@echo off
REM Pulisce e compila il progetto
echo Avvio build con Maven...
call mvn clean install 
if %ERRORLEVEL% NEQ 0 (
    echo Errore durante la compilazione con Maven. Operazione interrotta.
    pause
    exit /b
)

REM Esegue il file JAR generato
echo Avvio applicazione...
call java -jar .\target\pdf-search.jar
if %ERRORLEVEL% NEQ 0 (
    echo Errore durante l'esecuzione dell'applicazione.
    pause
    exit /b
)
