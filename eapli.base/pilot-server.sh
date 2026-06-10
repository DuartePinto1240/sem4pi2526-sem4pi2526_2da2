#!/bin/bash
set -e

echo "================================================="
echo "       A iniciar o AlSafe Pilot Server           "
echo "  Porta TCP : 2228                               "
echo "================================================="

mvn compile exec:java \
    -Dexec.mainClass="rcomp.PilotServer" \
    -pl exemplo.core