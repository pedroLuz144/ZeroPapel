#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Uso: $0 <usuario@servidor> [--sem-testes]" >&2
    echo "Exemplo: $0 pedro@zeropapel" >&2
    exit 1
fi

DESTINO="$1"
RODAR_TESTES=sim
[ "${2:-}" = "--sem-testes" ] && RODAR_TESTES=nao
RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
PASTA_REMOTA="zeropapel"

VERSAO="$(git -C "$RAIZ" rev-parse --short HEAD)"
if [ -n "$(git -C "$RAIZ" status --porcelain -- src frontend pom.xml Dockerfile)" ]; then
    VERSAO="$VERSAO-sujo-$(date +%Y%m%d%H%M)"
fi

echo "==> Conferindo o acesso a $DESTINO"
ssh -o BatchMode=yes "$DESTINO" "test -f $PASTA_REMOTA/.env" || {
    echo "Sem SSH por chave ou sem ~/$PASTA_REMOTA/.env no servidor. Veja o README desta pasta." >&2
    exit 1
}

if [ "$RODAR_TESTES" = sim ]; then
    echo "==> Rodando os testes (sem o CdpApplicationTests, que precisa de banco)"
    (cd "$RAIZ" && bash ./mvnw -q -B test -Dtest='!CdpApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false)
fi

echo "==> Gerando a imagem zeropapel:$VERSAO"
docker build --platform linux/amd64 -t "zeropapel:$VERSAO" "$RAIZ"

echo "==> Enviando a imagem (pode levar alguns minutos)"
docker save "zeropapel:$VERSAO" | gzip -1 | ssh "$DESTINO" 'gunzip | docker load'

echo "==> Enviando o compose e os scripts do servidor"
scp -q "$RAIZ/docker-compose.prod.yml" \
    "$RAIZ/scripts/servidor-notebook/backup.sh" \
    "$RAIZ/scripts/servidor-notebook/voltar-versao.sh" \
    "$DESTINO:$PASTA_REMOTA/"

echo "==> Backup, troca de versão e subida"
ssh "$DESTINO" VERSAO="$VERSAO" PASTA="$PASTA_REMOTA" 'bash -s' <<'REMOTO'
set -euo pipefail
cd "$HOME/$PASTA"
chmod +x backup.sh voltar-versao.sh
COMPOSE="docker compose -f docker-compose.prod.yml"

if [ -n "$($COMPOSE ps -q banco 2>/dev/null)" ]; then
    ./backup.sh
fi

if docker image inspect zeropapel:atual >/dev/null 2>&1; then
    docker tag zeropapel:atual zeropapel:anterior
fi
docker tag "zeropapel:$VERSAO" zeropapel:atual
echo "$VERSAO" > versao-atual

$COMPOSE up -d

echo "Esperando a aplicação responder em 127.0.0.1:8080 (a primeira subida leva uns 2 minutos)"
for _ in $(seq 1 60); do
    if curl -fsS -o /dev/null http://127.0.0.1:8080/login; then
        echo "No ar: versão $VERSAO"
        docker images zeropapel --format '{{.Tag}}' \
            | grep -vxE "atual|anterior|$VERSAO" \
            | xargs -r -I{} docker rmi -f "zeropapel:{}" >/dev/null || true
        docker image prune -f >/dev/null
        exit 0
    fi
    sleep 5
done

echo "A aplicação não respondeu em 5 minutos. Últimas linhas do log:" >&2
$COMPOSE logs --tail 60 app >&2
echo "Para voltar à versão anterior: ~/$PASTA/voltar-versao.sh" >&2
exit 1
REMOTO
