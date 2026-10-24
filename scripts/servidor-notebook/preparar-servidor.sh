#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -eq 0 ]; then
    echo "Rode como o seu usuário (o script chama sudo quando precisa)." >&2
    exit 1
fi

echo "==> Pacotes"
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 zram-tools unattended-upgrades curl ufw
sudo usermod -aG docker "$USER"

echo "==> Fuso horário"
sudo timedatectl set-timezone America/Sao_Paulo

echo "==> Tampa fechada não suspende"
sudo mkdir -p /etc/systemd/logind.conf.d
sudo tee /etc/systemd/logind.conf.d/zeropapel.conf >/dev/null <<'CONF'
[Login]
HandleLidSwitch=ignore
HandleLidSwitchExternalPower=ignore
HandleLidSwitchDocked=ignore
HandleSuspendKey=ignore
CONF
sudo systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target

echo "==> Swap comprimida na RAM (zram)"
sudo tee /etc/default/zramswap >/dev/null <<'CONF'
ALGO=zstd
PERCENT=50
PRIORITY=100
CONF
sudo systemctl restart zramswap

echo "==> Atualizações de segurança automáticas"
sudo dpkg-reconfigure -f noninteractive unattended-upgrades

echo "==> Firewall"
sudo ufw allow OpenSSH
sudo ufw --force enable

echo "==> Pasta e backup diário às 4h"
mkdir -p "$HOME/zeropapel/backups"
LINHA="0 4 * * * $HOME/zeropapel/backup.sh >> $HOME/zeropapel/backup.log 2>&1"
( crontab -l 2>/dev/null | grep -v 'zeropapel/backup.sh' || true; echo "$LINHA" ) | crontab -

echo
echo "Pronto. Falta (detalhes no README desta pasta):"
echo "  1. Reiniciar (sudo reboot) para valer o grupo docker e a tampa"
echo "  2. Criar ~/zeropapel/.env por heredoc (README, seção 2)"
echo "  3. Instalar o Tailscale e ligar o Funnel (README, seção 4)"
echo "  4. Do PC: chave SSH e depois scripts/servidor-notebook/publicar.sh $USER@<endereço>"
echo "     <endereço> é o IP do notebook na rede da loja ou, com o Tailscale, o nome da"
echo "     máquina na tailnet. IPs deste notebook agora: $(hostname -I 2>/dev/null || echo '?')"
echo "  O backup do cron só funciona depois do primeiro publicar.sh; até lá ele grava o erro em"
echo "  ~/zeropapel/backup.log."
