#!/usr/bin/env bash
set -euo pipefail

BASE="$(cd "$(dirname "$0")" && pwd)"

if ! docker image inspect zeropapel:anterior >/dev/null 2>&1; then
    echo "Não há versão anterior guardada." >&2
    exit 1
fi

docker tag zeropapel:atual zeropapel:descartada
docker tag zeropapel:anterior zeropapel:atual
docker tag zeropapel:descartada zeropapel:anterior
docker rmi zeropapel:descartada >/dev/null
docker compose -f "$BASE/docker-compose.prod.yml" up -d app
echo "Voltou para a versão anterior. Rodar de novo desfaz a troca."
echo "Migration do Flyway não é desfeita: se a versão nova criou tabela ou coluna, a anterior pode não subir."
