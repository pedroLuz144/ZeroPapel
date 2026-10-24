# Servidor no notebook (Ubuntu Server + Docker)

O ZeroPapel roda num notebook com Celeron N2806, 2 GB de RAM e HD de 320 GB, com Ubuntu Server
24.04 LTS sem interface gráfica. A aplicação e o MariaDB sobem pelo `docker-compose.prod.yml`
da raiz do repositório, com a memória limitada para caber em 2 GB. O `docker-compose.yml` de
desenvolvimento continua igual e não é usado aqui.

A imagem **nunca é gerada no notebook**: o build da SPA e do jar estoura 2 GB. Ela é gerada no
PC e enviada pronta pelo `publicar.sh`.

| Arquivo | Onde roda | O que faz |
|---|---|---|
| `preparar-servidor.sh` | notebook, uma vez (vai do PC por `scp`) | Docker, fuso, tampa fechada sem suspender, zram, firewall, backup diário |
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

Ninguém da loja digita o IP: o sistema é aberto pelo link do Funnel (seção 4). O IP fixo serve
para você entrar por SSH e publicar de dentro da loja. O jeito mais simples é **reservar o IP no
roteador** (DHCP estático ou "reserva de endereço", pelo MAC
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

O notebook **não recebe o repositório**: nada de `git clone` nem de `.env.example` lá. Ele só
tem o que você manda do PC: o `preparar-servidor.sh` (uma vez, abaixo) e, a cada versão, o que o
`publicar.sh` envia para `~/zeropapel` (imagem, `docker-compose.prod.yml`, `backup.sh` e
`voltar-versao.sh`). O `.env` é criado direto no notebook.

No **PC**, na raiz do repositório:

```bash
scp scripts/servidor-notebook/preparar-servidor.sh usuario@IP-do-notebook:
ssh usuario@IP-do-notebook
```

No notebook (pela sessão SSH):

```bash
bash preparar-servidor.sh
sudo reboot
```

O reboot é para valer o grupo `docker` e a tampa fechada.

### O `.env` de produção

Crie o `~/zeropapel/.env` no notebook, **com valores novos, não os do PC**. O heredoc abaixo já
sorteia a senha do banco e a chave do JWT ali mesmo:

```bash
cd ~/zeropapel
umask 077
cat > .env <<EOF
URL_BD=jdbc:mariadb://banco:3306/zeropapel
USER_BD=root
SENHA_BD=$(openssl rand -hex 24)
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
JWT_EXPIRATION_MS=1800000
CORS_ALLOWED_ORIGIN=https://PREENCHER.ts.net
ADMIN_USUARIO=
ADMIN_SENHA=
ADMIN_NOME=Administrador
SEED_SENHA=
IFOOD_CLIENT_ID=
IFOOD_CLIENT_SECRET=
IFOOD_POLLING_ATIVO=false
IFOOD_FORMA_DE_PAGAMENTO_ID=
EOF
chmod 600 .env
nano .env
```

No `nano`, preencha `ADMIN_USUARIO` e `ADMIN_SENHA`; o `CORS_ALLOWED_ORIGIN` fica para depois de
ligar o Funnel (seção 4). Formato de properties: sem aspas, sem `export`, sem espaço em volta do
`=`.

Não use `openssl rand -base64 64`: ele quebra a saída em duas linhas, e a segunda vira lixo no
`.env`. Com 48 bytes sai uma linha só de 64 caracteres, acima do mínimo de 32 bytes do JWT.

| Variável | Valor em produção |
|---|---|
| `URL_BD`, `USER_BD` | qualquer coisa: o compose sobrescreve (`banco` e `root`) |
| `SENHA_BD` | senha nova do `root` do MariaDB (`openssl rand -hex 24`). Ver o aviso abaixo |
| `JWT_SECRET` | chave nova (`openssl rand -base64 48 \| tr -d '\n'`); nunca a do PC |
| `JWT_EXPIRATION_MS` | `1800000` (30 min); a SPA renova pelo refresh token |
| `CORS_ALLOWED_ORIGIN` | o endereço público do Funnel, `https://<máquina>.<tailnet>.ts.net`, sem barra no fim (seção 4) |
| `ADMIN_USUARIO`, `ADMIN_SENHA` | o primeiro gerente (senha de 8 a 72 caracteres); só vale com a tabela de usuários vazia. Troque a senha depois do primeiro login |
| `ADMIN_NOME` | opcional; em branco vira "Administrador" |
| `SEED_SENHA` | em branco: o `popular_banco.py` não roda em produção |
| `IFOOD_CLIENT_ID`, `IFOOD_CLIENT_SECRET`, `IFOOD_FORMA_DE_PAGAMENTO_ID` | em branco até o iFood liberar uma loja real |
| `IFOOD_POLLING_ATIVO` | `false` até o iFood liberar uma loja real |

**`SENHA_BD` só vale na primeira subida do banco.** O MariaDB grava a senha do `root` quando cria
o volume `zeropapel-prod-mariadb` e depois ignora a variável. Trocar a `SENHA_BD` no `.env` com o
volume já criado faz a aplicação não conectar mais. Para trocar de verdade:

```bash
cd ~/zeropapel
docker compose -f docker-compose.prod.yml exec banco sh -c 'exec mariadb -uroot -p"$MARIADB_ROOT_PASSWORD"'
```

E, dentro do `mariadb`:

```sql
ALTER USER 'root'@'%' IDENTIFIED BY 'SENHA-NOVA';
ALTER USER 'root'@'localhost' IDENTIFIED BY 'SENHA-NOVA';
exit
```

Depois ponha a mesma `SENHA-NOVA` no `.env` e rode `docker compose -f docker-compose.prod.yml up -d`
(isso recria os dois containers com a variável nova). A outra saída é apagar o volume
(`docker compose -f docker-compose.prod.yml down -v`), que **apaga todos os dados**: só antes de
ter venda de verdade, ou com um backup para restaurar.

### Acesso SSH por chave

O `publicar.sh` usa SSH sem senha (`BatchMode`). No **PC**:

```bash
ls ~/.ssh/id_ed25519.pub || ssh-keygen -t ed25519
ssh-copy-id usuario@IP-do-notebook
ssh -o BatchMode=yes usuario@IP-do-notebook 'echo chave ok'
```

O `ssh-copy-id` pergunta se confia no notebook (`yes`) e grava a chave dele em
`~/.ssh/known_hosts`. Com `BatchMode` essa pergunta não aparece: o `publicar.sh` falha com
*Host key verification failed* para um nome que ainda não foi aceito. Por isso, antes de publicar
pelo nome do Tailscale (`usuario@zeropapel`, seção 4), entre uma vez com `ssh usuario@zeropapel`
sem `BatchMode` e responda `yes`.

Só **depois** que o último comando acima responder sem pedir senha, desligue o login por senha no
notebook. O arquivo precisa começar com `00-`: o instalador do Ubuntu Server costuma deixar
`/etc/ssh/sshd_config.d/50-cloud-init.conf` com `PasswordAuthentication yes`, e o `sshd` fica
com o **primeiro** valor que lê, em ordem alfabética dos arquivos. Um `zeropapel.conf` vem
depois do `50-` e não teria efeito.

```bash
echo 'PasswordAuthentication no' | sudo tee /etc/ssh/sshd_config.d/00-zeropapel.conf
sudo sshd -t && sudo systemctl restart ssh
sudo sshd -T | grep -i '^passwordauthentication'
```

O último tem de mostrar `passwordauthentication no`. Não feche a sessão aberta antes de conferir,
de outro terminal do PC, que o `ssh usuario@IP-do-notebook` continua entrando.

---

## 3. Publicar uma versão (do PC)

```bash
scripts/servidor-notebook/publicar.sh usuario@IP-do-notebook
```

Ele roda os testes (menos o `CdpApplicationTests`, que precisa de banco), gera a imagem
`zeropapel:<commit>`, envia por SSH, faz backup do banco, troca a versão e espera a aplicação
responder. `--sem-testes` pula os testes. A primeira subida no Celeron leva uns 2 minutos.

A imagem é gerada pelo `Dockerfile`, que já faz o build da SPA: não precisa rodar
`npm run build` antes. Precisa de Docker no PC.

O `usuario@...` pode ser o IP do notebook na rede da loja ou o nome dele no Tailscale
(`usuario@zeropapel`, seção 4).

No notebook:

```bash
cd ~/zeropapel
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/login
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f app
docker stats --no-stream
./voltar-versao.sh
```

O `curl` tem de mostrar `200`. Use GET, não `curl -I`: o `-I` manda HEAD, e a segurança só libera
GET nas rotas da SPA, então ele responde 401 mesmo com tudo certo.

O Flyway roda as migrations na subida. `voltar-versao.sh` não desfaz migration: se a versão
nova mudou o schema, a anterior pode não subir, e aí o caminho é restaurar o backup.

Restaurar um backup:

```bash
gunzip -c backups/zeropapel-AAAAMMDD-HHMM.sql.gz \
  | docker compose -f docker-compose.prod.yml exec -T banco \
      sh -c 'exec mariadb -uroot -p"$MARIADB_ROOT_PASSWORD"'
```

O `backup.sh` roda pelo cron às 4h e grava em `~/zeropapel/backup.log`. Antes do primeiro
`publicar.sh` não existe nem o script nem o banco, então o log mostra erro até lá. Depois, se o
compose ou o container do banco não estiver rodando, ele grava `ERRO no backup: ...` com o motivo
e sai com erro, sem deixar arquivo pela metade. Confira de vez em quando:

```bash
tail ~/zeropapel/backup.log
```

### Cópia no Google Drive (rclone)

O backup fica no próprio HD do notebook. Se o HD morrer, vai junto. Com o `rclone` instalado e um
remoto chamado `drive`, o `backup.sh` passa a mandar uma cópia ao Google Drive sozinho. O
`preparar-servidor.sh` não instala o `rclone`.

No notebook:

```bash
sudo apt-get install -y rclone
rclone config
```

No `rclone config`: `n` (novo remoto), nome `drive`, tipo `drive` (Google Drive), `client_id` e
`client_secret` em branco, escopo `1` (acesso completo) ou `3` (só arquivos criados pelo rclone),
o resto no padrão. Na pergunta *Use web browser to automatically authenticate rclone with remote?*
responda **`n`**: o notebook não tem navegador. Ele mostra um comando `rclone authorize "drive" ...`.

No **PC** (com navegador), instale o `rclone` (`sudo apt-get install -y rclone`) e rode o comando
que o notebook mostrou. O navegador abre, você entra na conta Google, e o terminal do PC imprime
um token em JSON. Copie esse token inteiro e cole no notebook, na pergunta *config_token>*. Responda
`n` para *team drive* e confirme com `y`.

Teste:

```bash
rclone lsd drive:
~/zeropapel/backup.sh
```

O backup tem de terminar com `Cópia enviada ao Google Drive`.

---

## 4. Acesso: quem entra por onde

| Quem | De onde | Como |
|---|---|---|
| Atendente (PDV) | dentro da loja | link público do **Tailscale Funnel** (HTTPS) |
| Gerente (dashboard, fechamento) | celular, de qualquer lugar | link público do **Tailscale Funnel**, sem instalar nada |
| Você (manutenção, SSH, publicar) | de qualquer lugar | SSH pelo Tailscale; o sistema pelo link do Funnel |

A aplicação só escuta em `127.0.0.1:8080` no notebook (`docker-compose.prod.yml`). **Não existe
acesso por `http://IP-do-notebook:8080` na rede da loja**, nem por `http://zeropapel:8080` no
Tailscale: em HTTP puro a senha do login e o token iriam abertos pela rede, e qualquer aparelho
no mesmo Wi-Fi poderia ler. Toda entrada no sistema é pelo Funnel, que termina o HTTPS no
notebook e repassa para o `127.0.0.1:8080`. Consequência: sem internet na loja, o PDV fica fora
do ar, porque até o computador do caixa passa pelo Funnel.

Tudo aqui é de graça: o Tailscale no plano pessoal e o Funnel com o endereço `*.ts.net`, sem
domínio próprio.

### O iFood não precisa entrar no servidor

A ingestão é por **polling** (decisão 3 de `docs/integracao-ifood.md`): é o ZeroPapel que pergunta
ao iFood a cada 30 s se há eventos novos. Só existe tráfego de saída. Não precisa abrir porta no
roteador, ter IP fixo de internet nem domínio para o iFood funcionar.

### Dentro da loja

O computador ou tablet do caixa abre o mesmo link do Funnel que a gerente usa
(`https://zeropapel.<rede>.ts.net`). Salve como favorito ou atalho na tela inicial. O IP
reservado no roteador continua servindo para o SSH dentro da loja.

Para testar no próprio notebook, sem passar pelo Funnel (GET, não `curl -I`; tem de dar `200`):

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/login
```

### Tailscale: a sua rede privada

O Tailscale cria uma rede privada entre os seus aparelhos. O notebook ganha um endereço
`100.x.y.z` e um nome (`zeropapel`) que só os aparelhos da sua conta enxergam. Funciona atrás de
CGNAT (comum nas operadoras de fibra daqui, onde abrir porta nem é possível).

No notebook:

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
sudo tailscale set --hostname zeropapel
```

O `tailscale up` mostra um link: abra no navegador e entre com a sua conta. Instale o app
Tailscale no seu PC, na mesma conta. A partir daí:

- SSH: `ssh usuario@zeropapel` (ou pelo IP `100.x.y.z` do `tailscale ip -4`), e
  `publicar.sh usuario@zeropapel` de qualquer lugar. É o OpenSSH normal do notebook, com a mesma
  chave da seção 2, passando pelo túnel do Tailscale
- sistema: pelo link do Funnel, como todo mundo (o `8080` não responde pelo endereço do
  Tailscale, só no `127.0.0.1` do notebook)

**Não use `tailscale up --ssh`.** Ele liga o *Tailscale SSH*, que toma a porta 22 para quem chega
pela tailnet, ignora o `~/.ssh/authorized_keys` e, na política padrão da conta, roda em *check
mode*: de tempos em tempos exige reautenticar no navegador. O `publicar.sh` usa
`ssh -o BatchMode=yes`, que não abre navegador nem pergunta nada, e aí trava ou falha. Se já tiver
ligado, desligue com `sudo tailscale set --ssh=false`.

### Link público: Tailscale Funnel

O Funnel publica o `127.0.0.1:8080` do notebook na internet com HTTPS e certificado válido, num endereço
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
   valor, login e qualquer POST pelo link público voltam 403. Como o Funnel é a única entrada,
   esse é o único valor que faz sentido em produção. Mudou o `.env` com a aplicação já no ar?
   Aplique com `docker compose -f docker-compose.prod.yml up -d app`, em `~/zeropapel`.

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
**passa por fora do ufw**. Por isso o compose de produção publica a aplicação só em
`127.0.0.1:8080`: nenhum aparelho da rede da loja nem do Tailscale alcança a porta, só o próprio
notebook (e, por ele, o Funnel). Não troque para `"8080:8080"`: isso volta a abrir o HTTP sem
criptografia para toda a rede, por fora do firewall. Mesmo assim, o Wi-Fi da loja onde fica o
servidor **não** deve ser o mesmo Wi-Fi aberto para clientes, por causa do SSH.

O MariaDB não publica porta nenhuma: só a aplicação, dentro da rede do Docker, fala com ele.
