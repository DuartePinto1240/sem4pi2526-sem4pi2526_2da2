#!/bin/bash
set -e

# Apaga e recria a base de dados 'alsafe' (PostgreSQL DEI).
# Uso: ./clean-database.sh

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

read -r -p "Apagar e recriar a base '$DB_NAME'? (sim/nao): " r
[[ "$(echo "$r" | tr '[:upper:]' '[:lower:]')" == "sim" ]] || exit 0

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 <<SQL
DROP DATABASE IF EXISTS ${DB_NAME} WITH (FORCE);
CREATE DATABASE ${DB_NAME};
SQL

echo ""
echo "Base '${DB_NAME}' recriada (vazia — sem tabelas)."
echo ""
echo "No Adminer vais ver a BD sem tabelas. Isso é normal."
echo "Corre ./run-backoffice.sh ou ./reset-database.sh (BD + bootstrap)."