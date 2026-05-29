# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexto do produto

Sistema de gestão de caixa para uma petiscaria. O objetivo central é automatizar dois processos:
1. **Registro de vendas** — o atendente lança vendas de balcão pelo sistema; vendas de plataformas (iFood, etc.) serão capturadas via integração futura
2. **Fechamento de caixa** — dashboard para o gerente consultar o consolidado da noite, já com deduções de taxas de plataforma e forma de pagamento calculadas automaticamente

Implementado: Pedido, ItemPedido, Plataforma, FormaDePagamento
Ainda por vir: Entregador, Fechamento de caixa (agregação de um período com cálculo de taxas)

Integrações com APIs de plataformas (iFood, AnotaAi) são planejadas mas fora de escopo no momento. Quando implementadas, as classes de integração devem depender de `PedidoService` — elas traduzem o formato externo para `AdicionarPedidoRequest` e delegam para o service, sem duplicar lógica de negócio.

## Rules
- Never expose @Entity directly in controller responses — always use a ResponseDTO
- Never put business logic in controllers — delegate to service
- Services must always use the interface, never the impl class directly
- @PreAuthorize goes on the controller method, not the service
- Do not add new dependencies to pom.xml without asking

## Commands

```bash
# Run the application
./mvnw spring-boot:run

# Build
./mvnw clean package

# Run all tests
./mvnw clean test

# Run a single test class
./mvnw test -Dtest=CdpApplicationTests
```

## Environment Setup

The app requires a `.env` file (use `envExample` as template) with:

```
URL_BD=jdbc:mariadb://localhost:3306/zeropapel
USER_BD=root
SENHA_BD=root
JWT_SECRET=<base64-encoded-secret>
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGIN=http://localhost:8080
```

Requires a running **MariaDB** instance. Hibernate manages the schema automatically (`ddl-auto=update`).

## Architecture

Spring Boot 4 REST API using package-by-feature. Each feature package contains its own controller, service, repository, entity, and DTOs:

```
autenticacao/         → login, refresh token, JWT auth flow
  controller/
  dto/request/
  dto/response/
  entity/             → RefreshToken
  repository/
  service/

usuario/              → cadastro e gestão de usuários
  controller/
  dto/request/
  dto/response/
  entity/             → Usuario (implements UserDetails)
  enumerator/         → Cargo (GERENTE | OPERADOR)
  repository/
  service/

item/                 → itens do cardápio
  controller/
  dto/request/
  dto/response/
  entity/             → Item
  enumerator/         → Status (ATIVO | INATIVO)
  repository/
  service/

categoria/            → categorias do cardápio
pedido/               → pedidos e itens de pedido (Pedido + ItemPedido)
plataforma/           → plataformas de venda (Balcão, iFood, etc.)
formadepagamento/     → formas de pagamento
fechamentodecaixa/    → fechamento de caixa com cálculo de taxas

common/
  exception/          → exceções customizadas + GlobalExceptionHandler

security/             → JwtFilter, JwtService, SecurityConfig, UsuarioDetailsServiceImpl

integracao/           → placeholder para integrações futuras (iFood, AnotaAi)
  ifood/
  anotaai/
```

## Naming Conventions
- Domain classes (entities, services, DTOs): Portuguese
  e.g. `ItemService`, `UsuarioResponseDTO`, `RecursoNaoEncontradoException`
- Variables and methods: camelCase Portuguese
  e.g. `buscarPorId()`, `nomeCompleto`
- Database columns: snake_case Portuguese
  e.g. `data_criacao`, `categoria_id`
- Package names: all lowercase, no camelCase
  e.g. `formadepagamento`, `fechamentodecaixa`

## DTO Mapping
Manual mapping only — no MapStruct or ModelMapper.
Conversion happens in the Service layer, never in Controller or Entity.

## Lombok
Project uses Lombok. Prefer @Data for DTOs, @Getter/@Setter for entities.

### Domain Model

- **Usuario** — implements `UserDetails`; role is `Cargo` enum (GERENTE or OPERADOR); table `usuarios`
- **Item** — menu item with price, status, and FK to `Categoria`; table `cardapio`
- **Categoria** — item categories; table `categorias`
- **RefreshToken** — linked to `Usuario`, has expiration; table `refresh_tokens`
- **Plataforma** — origin platform (e.g. Balcão, iFood) with configurable `taxaPercentual`; table `plataforma`
- **FormaDePagamento** — payment method with configurable `taxaPercentual`; table `forma_de_pagamento`
- **Pedido** — order with `nomeCliente` (nullable), `horarioPedido`, `valor` (stored at time of order), FK to `Plataforma` and `FormaDePagamento`; table `pedidos`
- **ItemPedido** — join entity between `Pedido` and `Item`; stores `quantidade` and `precoUnitario` (price locked at order time); table `itens_pedido`

### Security

Stateless JWT auth via `JwtFilter`. Access tokens expire in 24 h; refresh tokens allow renewal without re-login. Endpoint-level authorization uses `@PreAuthorize("hasRole('GERENTE')")`. Access control per resource:

- **Pedido** — write operations (register, update, delete) open to all authenticated users; only `DELETE` requires GERENTE
- **Plataforma / FormaDePagamento** — all write operations restricted to GERENTE; reads open to all authenticated
- **Item / Categoria** — all write operations restricted to GERENTE; reads open to all authenticated

Public endpoints: `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`.

### Error Handling

`GlobalExceptionHandler` maps:
- `RecursoNaoEncontradoException` → 404
- `ConflitoException` → 409
- `TokenInvalidoException` → 401
- `MethodArgumentNotValidException` → 400

All error responses return JSON `{ "error": "<message>" }`.
