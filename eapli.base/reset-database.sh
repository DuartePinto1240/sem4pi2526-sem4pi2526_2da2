#!/bin/bash
set -e

# Apaga/recria a base 'alsafe' e arranca o backoffice (schema Hibernate + bootstrap).
# Uso: ./reset-database.sh

cd "$(dirname "$0")"

# Carregar variáveis do .env
if [ -f .env ]; then
    source .env
else
    echo ">> ERRO: ficheiro .env não encontrado."
    echo "   Cria um .env com DB_HOST, DB_PORT, DB_USER, DB_PASSWORD e DB_NAME."
    exit 1
fi

export PGPASSWORD="$DB_PASSWORD"

echo "================================================="
echo "   RESET AlSafe — BD + bootstrap"
echo "================================================="
echo ""
echo "  1. Apaga e recria a base '$DB_NAME'"
echo "  2. Arranca o backoffice (tabelas + dados iniciais)"
echo ""
read -r -p "Continuar? (sim/nao): " r
[[ "$(echo "$r" | tr '[:upper:]' '[:lower:]')" == "sim" ]] || exit 0

echo ""
echo ">> A recriar a base de dados..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 <<SQL
DROP DATABASE IF EXISTS ${DB_NAME} WITH (FORCE);
CREATE DATABASE ${DB_NAME};
SQL

echo ">> Base recriada."
echo ">> A compilar e correr bootstrap (Main)..."
echo ""

mvn compile exec:java -Dexec.mainClass="Main" -pl exemplo.app.backoffice.console