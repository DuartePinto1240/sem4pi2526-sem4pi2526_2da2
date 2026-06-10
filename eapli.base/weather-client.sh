#!/bin/bash
set -e

HOST="${1:-localhost}"
PORT="${2:-2226}"

echo "================================================="
echo "      A iniciar o AlSafe Weather Client          "
echo "  Servidor : $HOST:$PORT                         "
echo "================================================="

mvn compile exec:java \
    -Dexec.mainClass="rcomp.WeatherClient" \
    -Dexec.args="$HOST $PORT" \
    -pl exemplo.core