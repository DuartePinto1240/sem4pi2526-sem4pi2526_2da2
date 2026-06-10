#!/bin/bash
set -e

echo "================================================="
echo "     A iniciar o AlSafe Logging Server           "
echo "  UDP  port : 9090                               "
echo "  HTTP port : 8090                               "
echo "================================================="
echo ""
echo "  Páginas disponíveis após arranque:"
echo "  http://localhost:8090/        (eventos)"
echo "  http://localhost:8090/active  (utilizadores activos)"
echo "================================================="

mvn compile exec:java \
    -Dexec.mainClass="rcomp.logging.RemoteAccessesLoggingServer" \
    -pl exemplo.core