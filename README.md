# ZeroPapel

[![CI](https://github.com/pedroLuz144/ZeroPapel/actions/workflows/ci.yml/badge.svg)](https://github.com/pedroLuz144/ZeroPapel/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D)](https://vuejs.org/)

Sistema de gestão de caixa para uma petiscaria. Substitui o caderno de anotações por um PDV e
um fechamento de caixa que já desconta as taxas de plataforma e de forma de pagamento.

Dois processos são o núcleo do produto:

1. **Registro de vendas** — o atendente lança a venda de balcão num PDV; vendas de iFood e
   AnotaAi serão capturadas por integração
2. **Fechamento de caixa** — o gerente consulta o consolidado da noite com faturamento bruto,
   taxas deduzidas, faturamento líquido, ticket médio, ranking de itens e curva horária

Desenvolvido como projeto de estágio supervisionado.

## Stack

| Camada | Tecnologia |
|---|---|
| API | Spring Boot 4 · Java 21 · Spring Security com JWT stateless |
| Persistência | Spring Data JPA · MariaDB 11 · Flyway |
| SPA | Vue 3 · TypeScript estrito · Vite · Vue Router |
| Testes | JUnit Jupiter · Mockito · AssertJ · Vitest |

A SPA é compilada para `src/main/resources/static/` e servida pelo próprio Spring, então
produção roda um único artefato `.jar`.

## Como rodar

### Pré-requisitos

- JDK 21
- MariaDB 11 no ar, com um schema criado (ex.: `zeropapel`)
- Node 22

### 1. Configurar o ambiente

Copie `envExample` para `.env` na raiz e preencha:

```bash
cp envExample .env
```

O `JWT_SECRET` precisa ser **base64 de no mínimo 32 bytes** — abaixo disso a aplicação não
sobe, por decisão da biblioteca de JWT. Para gerar um:

```bash
openssl rand -base64 32
```

### 2. Subir a API

```bash
./mvnw spring-boot:run
```

O Flyway cria ou atualiza o schema na subida. Num banco já existente sem histórico de
migração, ele aplica o baseline sem reexecutar o DDL.

### 3. Subir a SPA em modo de desenvolvimento

```bash
cd frontend
npm install
npm run dev
```

Serve em `http://localhost:5173` e encaminha as chamadas de API para a porta 8080.

### 4. Empacotar para produção

```bash
cd frontend && npm run build && cd ..
./mvnw clean package
java -jar target/ZeroPapel-0.0.1-SNAPSHOT.jar
```

O build da SPA precisa vir antes do `package` — é o que embute a interface no `.jar`.

### Popular com dados de teste

```bash
pip install -r scripts/requirements.txt
python scripts/popular_banco.py
```

Lê as credenciais do banco do `.env`. A senha dos usuários criados vem de `SEED_SENHA` ou,
na falta dela, é gerada aleatoriamente e impressa no fim. Nenhuma senha fica no repositório.

## Testes

```bash
./mvnw test                                  # 46 testes
./mvnw test -Dtest=FechamentoServiceImplTest # uma classe
cd frontend && npm run test:run              # Vitest
cd frontend && npm run type-check            # tipos
```

O `CdpApplicationTests` é um smoke test de contexto e exige MariaDB no ar. Os demais são
unitários com repositório mockado, sem banco.

O CI roda tudo isso num MariaDB limpo em container, o que também verifica que as migrations
reproduzem um schema compatível com as entidades a partir do zero.

## Schema

O schema é versionado em `src/main/resources/db/migration/`, no padrão
`V<n>__<descricao>.sql`. O Hibernate roda em `ddl-auto=validate`: a aplicação **não sobe** se
entidade e tabela divergirem.

**Migration já aplicada nunca é editada** — o Flyway guarda o checksum e falha na subida.
Correção se faz com uma migration nova.

## Estrutura

```
src/main/java/com/goldenpetiscaria/zeropapel/
  autenticacao/      login, refresh token, fluxo JWT
  usuario/           cadastro e gestão de usuários (Cargo: GERENTE | OPERADOR)
  item/              itens do cardápio
  categoria/         categorias do cardápio
  pedido/            pedidos e itens de pedido, ciclo de vida do StatusPedido
  plataforma/        canais de venda (Balcão, iFood) com taxa configurável
  formadepagamento/  formas de pagamento com taxa configurável
  fechamentodecaixa/ cálculo e persistência do fechamento
  dashboard/         o mesmo consolidado, em tempo real e só leitura
  integracoes/       iFood e AnotaAi
  common/            exceções e handler global
  security/          JwtFilter, JwtService, SecurityConfig

frontend/src/        SPA (views, components, stores, api)
bruno/               coleção de requisições da API (Bruno)
docs/                decisões de arquitetura
```

Cada pacote de feature é uma fatia vertical com controller, service, repository, entity e DTOs
próprios. As fronteiras entre features e o resto das convenções estão em
[CLAUDE.md](CLAUDE.md).

## API

Autenticação JWT stateless. `POST /auth/login` devolve access token e refresh token;
autorização por endpoint com `@PreAuthorize`.

| Recurso | Leitura | Escrita |
|---|---|---|
| `/pedidos` | autenticado | autenticado (`DELETE` exige GERENTE) |
| `/itens`, `/categorias` | autenticado | GERENTE |
| `/plataformas`, `/formasDePagamento` | autenticado | GERENTE |
| `/fechamento`, `/dashboard` | GERENTE | GERENTE |
| `/usuarios` | GERENTE | GERENTE |

Públicos: `POST /auth/login`, `/auth/refresh`, `/auth/logout`.

Erros saem como `{ "error": "<mensagem>" }`.

## Status

Implementado: usuários, cardápio, categorias, pedidos com ciclo de vida, plataformas, formas de
pagamento, fechamento de caixa persistido, dashboard em tempo real e a SPA completa.

Em andamento: integração iFood (branch `feat/ifood`) — ver
[docs/plano-integracao-ifood.md](docs/plano-integracao-ifood.md).

Planejado: integração AnotaAi, entidade Entregador, aba Integrações nas configurações.
