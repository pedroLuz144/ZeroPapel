#!/usr/bin/env bash
set -euo pipefail

BASE="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_ARQUIVO="$BASE/docker-compose.prod.yml"
DESTINO="$BASE/backups"
DIAS_GUARDADOS=30
ARQUIVO="$DESTINO/zeropapel-$(date +%Y%m%d-%H%M).sql.gz"

falhar() {
    echo "$(date '+%F %T') ERRO no backup: $1" >&2
    exit 1
}

[ -f "$COMPOSE_ARQUIVO" ] \
    || falhar "$COMPOSE_ARQUIVO não existe. Ele chega no primeiro publicar.sh; até lá não há banco para copiar."

command -v docker >/dev/null || falhar "docker não encontrado no PATH."

if [ -z "$(docker compose -f "$COMPOSE_ARQUIVO" ps --status running -q banco 2>/dev/null)" ]; then
    falhar "o container do banco não está rodando (docker compose -f $COMPOSE_ARQUIVO ps)."
fi

mkdir -p "$DESTINO"
trap 'rm -f "$ARQUIVO.parcial"' EXIT
docker compose -f "$COMPOSE_ARQUIVO" exec -T banco \
    sh -c 'exec mariadb-dump -uroot -p"$MARIADB_ROOT_PASSWORD" --single-transaction --routines --databases zeropapel' \
    | gzip > "$ARQUIVO.parcial" \
    || falhar "o mariadb-dump falhou; nenhum arquivo novo foi gravado."
mv "$ARQUIVO.parcial" "$ARQUIVO"
echo "$(date '+%F %T') backup gravado: $ARQUIVO ($(du -h "$ARQUIVO" | cut -f1))"

find "$DESTINO" -name 'zeropapel-*.sql.gz' -mtime +"$DIAS_GUARDADOS" -delete

if command -v rclone >/dev/null && rclone listremotes | grep -q '^drive:'; then
    if rclone copy "$ARQUIVO" drive:zeropapel-backups/; then
        echo "Cópia enviada ao Google Drive"
    else
        falhar "a cópia local foi gravada, mas o envio ao Google Drive falhou (rclone)."
    fi
fi
