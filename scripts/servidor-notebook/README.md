# Servidor no notebook (Ubuntu Server + Docker)

O ZeroPapel roda num notebook com Celeron N2806, 2 GB de RAM e HD de 320 GB, com Ubuntu Server
24.04 LTS sem interface gráfica. A aplicação e o MariaDB sobem pelo `docker-compose.prod.yml`
da raiz do repositório, com a memória limitada para caber em 2 GB. O `docker-compose.yml` de
desenvolvimento continua igual e não é usado aqui.

A imagem **nunca é gerada no notebook**: o build da SPA e do jar estoura 2 GB. Ela é gerada no
PC e enviada pronta pelo `publicar.sh`.

| Arquivo | Onde roda | O que faz |
|---|---|---|
| `preparar-servidor.sh` | notebook, uma vez | Docker, fuso, tampa fechada sem suspender, zram, firewall, backup diário |
| `publicar.sh` | PC, a cada versão | testes, imagem, envio por SSH, backup, troca de versão, espera subir |
| `backup.sh` | notebook (cron às 4h e antes de cada publicação) | dump do banco em `~/zeropapel/backups`, guarda 30 dias |
| `voltar-versao.sh` | notebook | volta para a versão anterior da imagem |

Consumo esperado: Ubuntu ~250 MB, MariaDB até 384 MB, aplicação até 700 MB (heap de 384 MB).
Sobra memória para o sistema e para a zram.

---

## 1. Instalar o Ubuntu Server com rede

### Por que a rede "sumiu" no servidor da firma

Não precisou reinstalar: faltou configuração, o sistema estava bom. O instalador do Ubuntu
Server grava a configuração de rede (o **netplan**, em `/etc/netplan/`) só para as interfaces
que ele conseguiu configurar **durante a instalação**. Com o cabo desconectado, a placa de rede
ficou de fora do arquivo, e depois de ligar o cabo ninguém pediu IP ao roteador.

Instalar sem rede também deixa de fora as atualizações e, no caso do Wi-Fi, às vezes o pacote
`wpasupplicant`, sem o qual o netplan não conecta em Wi-Fi.

### O que fazer no notebook

**Instale com o cabo de rede ligado.** É o caminho sem surpresa: o instalador configura a
placa por DHCP, baixa atualizações e já instala o OpenSSH (marque "Install OpenSSH server" na
tela de SSH).

Se mesmo assim a rede não vier, resolve sem reinstalar:

```bash
ip link
sudo nano /etc/netplan/01-rede.yaml
```

Para cabo (troque `enp1s0` pelo nome que o `ip link` mostrou):

```yaml
network:
  version: 2
  ethernets:
    enp1s0:
      dhcp4: true
      optional: true
```

Para Wi-Fi (precisa do `wpasupplicant`; sem rede nenhuma, instale o `.deb` por pendrive):

```yaml
network:
  version: 2
  wifis:
    wlp2s0:
      dhcp4: true
      optional: true
      access-points:
        "NomeDaRede":
          password: "senha-do-wifi"
```

```bash
sudo chmod 600 /etc/netplan/01-rede.yaml
sudo netplan apply
ip addr
```

O `optional: true` evita que o boot fique 2 minutos parado esperando rede quando o cabo está
fora.

**Em produção, use cabo.** Wi-Fi de notebook antigo cai e volta, e cada queda é um polling do
iFood perdido e um atendente sem sistema.

### IP fixo

O atendente e o gerente vão digitar o endereço do servidor, então ele não pode mudar. O jeito
mais simples é **reservar o IP no roteador** (DHCP estático ou "reserva de endereço", pelo MAC
que o `ip link` mostra). Assim o notebook continua em `dhcp4: true` e sempre recebe o mesmo IP.

### Cuidados específicos deste notebook

- **Pendrive não aparece no boot**: notebook Bay Trail às vezes só dá boot em modo Legacy/CSM.
  Troque na BIOS.
- **Travamento aleatório**: o Bay Trail teve um bug antigo de congelar em estado de economia de
  energia. Com kernel atual é raro. Se acontecer, acrescente `intel_idle.max_cstate=1` em
  `GRUB_CMDLINE_LINUX_DEFAULT` no `/etc/default/grub` e rode `sudo update-grub`.
- **Bateria**: ela funciona como nobreak, o que é ótimo para queda de luz. Mas ficar 24 h na
  tomada envelhece a bateria e, em aparelho velho, ela pode **estufar**. Olhe de vez em quando;
  estufada, tire e deixe o notebook só na fonte.
- **Tela**: deixe a tampa fechada (o `preparar-servidor.sh` faz o notebook ignorar a tampa).

---

## 2. Preparar o servidor (uma vez)

No notebook:

```bash
git clone https://github.com/pedroLuz144/ZeroPapel.git
bash ZeroPapel/scripts/servidor-notebook/preparar-servidor.sh
```

Crie o `~/zeropapel/.env` a partir do `.env.example`, **com valores novos, não os do PC**:

| Variável | Valor em produção |
|---|---|
| `SENHA_BD` | senha nova, forte (`openssl rand -base64 24`) |
| `URL_BD`, `USER_BD` | qualquer coisa: o compose sobrescreve |
| `JWT_SECRET` | chave nova (`openssl rand -base64 64`); nunca a do PC |
| `CORS_ALLOWED_ORIGIN` | o endereço público do Funnel, `https://zeropapel.<rede>.ts.net` (ver seção 4) |
| `ADMIN_USUARIO`, `ADMIN_SENHA` | o primeiro gerente; troque a senha depois do primeiro login |
| `IFOOD_*` | deixe `IFOOD_POLLING_ATIVO=false` até o iFood liberar uma loja real |

```bash
chmod 600 ~/zeropapel/.env
sudo reboot
```

### Acesso SSH por chave

O `publicar.sh` usa SSH sem senha. No **PC**:

```bash
ssh-keygen -t ed25519
ssh-copy-id usuario@IP-do-notebook
ssh usuario@IP-do-notebook
```

Depois que a chave funcionar, desligue o login por senha no notebook:
`PasswordAuthentication no` em `/etc/ssh/sshd_config.d/zeropapel.conf` e
`sudo systemctl restart ssh`.

---

## 3. Publicar uma versão (do PC)

```bash
scripts/servidor-notebook/publicar.sh usuario@IP-do-notebook
```

Ele roda os testes (menos o `CdpApplicationTests`, que precisa de banco), gera a imagem
`zeropapel:<commit>`, envia por SSH, faz backup do banco, troca a versão e espera a aplicação
responder. `--sem-testes` pula os testes. A primeira subida no Celeron leva uns 2 minutos.

No notebook:

```bash
cd ~/zeropapel
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f app
docker stats --no-stream
./voltar-versao.sh
```

O Flyway roda as migrations na subida. `voltar-versao.sh` não desfaz migration: se a versão
nova mudou o schema, a anterior pode não subir, e aí o caminho é restaurar o backup.

Restaurar um backup:

```bash
gunzip -c backups/zeropapel-AAAAMMDD-HHMM.sql.gz \
  | docker compose -f docker-compose.prod.yml exec -T banco \
      sh -c 'exec mariadb -uroot -p"$MARIADB_ROOT_PASSWORD"'
```

O backup fica no próprio HD do notebook. Se o HD morrer, vai junto. Configure o `rclone` com
um remoto chamado `drive` (`rclone config`) e o `backup.sh` passa a mandar uma cópia ao Google
Drive sozinho.

---

## 4. Acesso: quem entra por onde

| Quem | De onde | Como |
|---|---|---|
| Atendente (PDV) | dentro da loja | `http://IP-do-notebook:8080` na rede da loja |
| Gerente (dashboard, fechamento) | celular, de qualquer lugar | link público do **Tailscale Funnel**, sem instalar nada |
| Você (manutenção, SSH, publicar) | de qualquer lugar | Tailscale |
| iFood | não entra | o servidor é que chama o iFood |

Tudo aqui é de graça: o Tailscale no plano pessoal e o Funnel com o endereço `*.ts.net`, sem
domínio próprio.

### O iFood não precisa entrar no servidor

A ingestão é por **polling** (decisão 3 de `docs/integracao-ifood.md`): é o ZeroPapel que pergunta
ao iFood a cada 30 s se há eventos novos. Só existe tráfego de saída. Não precisa abrir porta no
roteador, ter IP fixo de internet nem domínio para o iFood funcionar.

### Dentro da loja

O notebook e o computador ou tablet do caixa na mesma rede: `http://192.168.x.y:8080`. Salve
como favorito ou atalho na tela inicial. Com o IP reservado no roteador, o endereço não muda.

### Tailscale: a sua rede privada

O Tailscale cria uma rede privada entre os seus aparelhos. O notebook ganha um endereço
`100.x.y.z` e um nome (`zeropapel`) que só os aparelhos da sua conta enxergam. Funciona atrás de
CGNAT (comum nas operadoras de fibra daqui, onde abrir porta nem é possível).

No notebook:

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up --ssh
sudo tailscale set --hostname zeropapel
```

O `tailscale up` mostra um link: abra no navegador e entre com a sua conta. Instale o app
Tailscale no seu PC, na mesma conta. A partir daí:

- sistema: `http://zeropapel:8080`
- SSH: `ssh usuario@zeropapel`, e `publicar.sh usuario@zeropapel` de qualquer lugar

### Link público: Tailscale Funnel

O Funnel publica o 8080 do notebook na internet com HTTPS e certificado válido, num endereço
fixo do tipo `https://zeropapel.tail1234ab.ts.net`. A gerente abre no navegador do celular, sem
app e sem conta no Tailscale. O túnel é de saída, como o do iFood: nenhuma porta do roteador é
aberta e o CGNAT não atrapalha.

**Antes de ligar**, porque a tela de login fica aberta para qualquer pessoa na internet:

1. Conferir as senhas. O login já aceita só 5 tentativas a cada 15 min por usuário, e senha
   nova precisa ter de 8 a 72 caracteres. Mas usuário criado antes dessa regra pode ter senha
   curta, que continua valendo: troque em Configurações → Usuários. Vale rodar o subagente
   `auditor-seguranca` antes de passar o link adiante.
2. No `.env` do notebook, `CORS_ALLOWED_ORIGIN` com o endereço do Funnel, **com `https://` e sem
   barra no fim**. O navegador fala HTTPS com o Tailscale e a aplicação recebe HTTP: sem esse
   valor, login e qualquer POST pelo link público voltam 403. O acesso pela rede da loja
   (`http://192.168.x.y:8080`) e por `http://zeropapel:8080` continua funcionando, porque ali
   o navegador fala com a mesma origem e o CORS nem entra em jogo.

Ligar, no notebook (com o Tailscale já conectado):

```bash
sudo tailscale funnel --bg 8080
```

Na primeira vez ele mostra um link para habilitar HTTPS e o Funnel na sua conta: abra, aprove e
rode o comando de novo. Ele imprime o endereço público. O `--bg` deixa a configuração gravada:
sobrevive a reinício do notebook e do Tailscale.

```bash
tailscale funnel status
sudo tailscale funnel --https=443 off
```

O primeiro mostra o endereço e o que está publicado; o segundo desliga o link público.

O nome do meio (`tail1234ab`) é o da sua rede no Tailscale e é sorteado. Dá para trocar uma
vez por um nome mais fácil em *DNS → Tailnet name* no painel do Tailscale. Troque **antes** de
passar o link para a gerente e de preencher o `CORS_ALLOWED_ORIGIN`: depois, o endereço antigo
para de funcionar.

No celular da gerente: abrir o link no Chrome ou no Safari e usar *Adicionar à tela inicial*.
Fica um ícone como o de um app. A SPA tem layout próprio para telas de até 720 px: menu fixo
embaixo, tabelas largas viram cartões e o painel de pedidos rola de etapa em etapa.

### Firewall e o Docker

O `ufw` liberado pelo `preparar-servidor.sh` cuida do SSH, mas porta publicada pelo Docker
**passa por fora do ufw**. O 8080 fica acessível a qualquer aparelho que alcance o notebook: a
rede da loja e a sua rede do Tailscale. Da internet, a única entrada é o Funnel, com HTTPS; a
porta em si continua inalcançável, porque o roteador não encaminha nada para ela. Por isso o Wi-Fi da loja onde fica o servidor **não** deve ser o
mesmo Wi-Fi aberto para clientes.

O MariaDB não publica porta nenhuma: só a aplicação, dentro da rede do Docker, fala com ele.
