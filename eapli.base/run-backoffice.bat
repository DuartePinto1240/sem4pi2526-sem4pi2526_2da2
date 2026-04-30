@echo off
echo =================================================
echo    A iniciar o AlSafe Backoffice (Modo Rapido)   
echo =================================================

REM Compila apenas os ficheiros modificados e executa a classe Main no modulo correto
call mvn compile exec:java -Dexec.mainClass="Main" -pl exemplo.app.backoffice.console

echo.
pause