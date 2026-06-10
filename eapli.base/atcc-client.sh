#!/bin/bash
set -e

HOST="${1:-localhost}"
PORT="${2:-2225}"

echo "================================================="
echo "       A iniciar o AlSafe ATCC Client            "
echo "  Servidor : $HOST:$PORT                         "
echo "================================================="

mvn compile exec:java \
    -Dexec.mainClass="rcomp.ATCCClient" \
    -Dexec.args="$HOST $PORT" \
    -pl exemplo.core