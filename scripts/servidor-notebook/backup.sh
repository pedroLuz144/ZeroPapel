#!/usr/bin/env bash
set -euo pipefail

BASE="$(cd "$(dirname "$0")" && pwd)"
DESTINO="$BASE/backups"
DIAS_GUARDADOS=30
ARQUIVO="$DESTINO/zeropapel-$(date +%Y%m%d-%H%M).sql.gz"

mkdir -p "$DESTINO"
docker compose -f "$BASE/docker-compose.prod.yml" exec -T banco \
    sh -c 'exec mariadb-dump -uroot -p"$MARIADB_ROOT_PASSWORD" --single-transaction --routines --databases zeropapel' \
    | gzip > "$ARQUIVO.parcial"
mv "$ARQUIVO.parcial" "$ARQUIVO"
echo "$(date '+%F %T') backup gravado: $ARQUIVO ($(du -h "$ARQUIVO" | cut -f1))"

find "$DESTINO" -name 'zeropapel-*.sql.gz' -mtime +"$DIAS_GUARDADOS" -delete

if command -v rclone >/dev/null && rclone listremotes | grep -q '^drive:'; then
    rclone copy "$ARQUIVO" drive:zeropapel-backups/ && echo "Cópia enviada ao Google Drive"
fi
