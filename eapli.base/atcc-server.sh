#!/bin/bash
set -e

echo "================================================="
echo "       A iniciar o AlSafe ATCC Server            "
echo "  Porta TCP : 2225                               "
echo "================================================="

mvn compile exec:java \
    -Dexec.mainClass="rcomp.ATCCServer" \
    -pl exemplo.core