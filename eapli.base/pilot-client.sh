#!/bin/bash
set -e

HOST="${1:-localhost}"
PORT="${2:-2228}"

echo "================================================="
echo "       A iniciar o AlSafe Pilot Client           "
echo "  Servidor : $HOST:$PORT                         "
echo "================================================="

mvn compile exec:java \
    -Dexec.mainClass="rcomp.PilotClient" \
    -Dexec.args="$HOST $PORT" \
    -pl exemplo.core