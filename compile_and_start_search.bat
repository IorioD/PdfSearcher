@echo off
REM Pulisce e compila il progetto
echo Starting Maven build...
call mvn clean install 
if %ERRORLEVEL% NEQ 0 (
    echo Error while compiling. Operation aborted.
    pause
    exit /b
)

REM Esegue il file JAR generato
echo Starting application...
call java -jar .\target\pdf-search.jar
if %ERRORLEVEL% NEQ 0 (
    echo Error during application execution.
    pause
    exit /b
)
