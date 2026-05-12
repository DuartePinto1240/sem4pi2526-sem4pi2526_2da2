#!/bin/bash

# Termina o script imediatamente se ocorrer algum erro
set -e

echo "================================================="
echo "   A compilar todo o projeto AlSafe (NFR07)      "
echo "================================================="

# O comando 'clean install' garante que a pasta target é limpa
# e que todos os módulos são compilados e ligados corretamente.
# O '-DskipTests' acelera o processo ignorando os testes unitários.
# Retira o '-DskipTests' se quiseres que o script valide os testes!
mvn clean install -DskipTests

echo "================================================="
echo "   Build concluído com sucesso!                  "
echo "   Os módulos estão prontos a ser executados.    "
echo "================================================="