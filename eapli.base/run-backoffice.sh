#!/bin/bash

# Termina o script imediatamente se ocorrer algum erro crítico
set -e

echo "================================================="
echo "          A iniciar o AlSafe Backoffice          "
echo "================================================="

# Compila apenas os ficheiros modificados e executa a classe Main no módulo correto
mvn compile exec:java -Dexec.mainClass="Main" -pl exemplo.app.backoffice.console