#!/bin/bash
set -e

echo "================================================="
echo "      A iniciar o AlSafe Weather Server          "
echo "  Porta TCP : 2226                               "
echo "================================================="

mvn compile exec:java \
    -Dexec.mainClass="rcomp.WeatherServer" \
    -pl exemplo.core