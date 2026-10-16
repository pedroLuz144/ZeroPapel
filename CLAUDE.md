# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexto do produto

Sistema de gestão de caixa para uma petiscaria. O objetivo central é automatizar dois processos:
1. **Registro de vendas** — o atendente lança vendas de balcão pelo sistema; vendas de plataformas (iFood, etc.) serão capturadas via integração futura
2. **Fechamento de caixa** — dashboard para o gerente consultar o consolidado da noite, já com deduções de taxas de plataforma e forma de pagamento calculadas automaticamente

Implementado: Usuario, Item, Categoria, Pedido, ItemPedido, Plataforma, FormaDePagamento,
Loja, CanalDeVenda (domínio multi-loja, etapa 1 de `docs/integracao-ifood.md`),
FechamentoCaixa (`POST /fechamento` persiste) e o consolidado em tempo real (`GET /dashboard`),
além da SPA em `frontend/`. O domínio já recebe pedido de plataforma externa
(`Pedido.plataformaPedidoId`, `ItemPedido` com item opcional e nome gravado,
`PedidoService.registrarPedidoExterno` e `aplicarStatusExterno`; etapa 2 do mesmo doc), e
quem chama essas portas é a ingestão do iFood (`integracoes/ifood/`): autenticação do aplicativo,
listagem de lojas, mapeamento de evento para `StatusPedido`, polling com acknowledgment, inbox de
eventos, busca do pedido e translator. O polling vem **desligado** (`IFOOD_POLLING_ATIVO=false`)
e foi validado em 15/10/2026 só contra a loja de teste do Developer Portal.
Ainda por vir: Entregador; AnotaAi; ligar a ingestão do iFood numa loja real (branch
`feat/ifood`);
fechamento e dashboard filtrados por loja e a regra condicional de taxa por origem (hoje o
consolidado soma todas as lojas juntas); aba Integrações em Configurações.

As classes de integração devem depender de `PedidoService` — elas traduzem o formato externo para `RegistrarPedidoExternoRequest` e delegam para `PedidoService.registrarPedidoExterno`, sem duplicar lógica de negócio.

## Rules
- Não escrever comentário dentro do código. Se a informação é necessária e não cabe num nome de
  método ou variável, ela vai para este arquivo, para `docs/` ou para a mensagem de commit
- Never expose @Entity directly in controller responses — always use a ResponseDTO
- Never put business logic in controllers — delegate to service
- Services must always use the interface, never the impl class directly
- @PreAuthorize goes on the controller method, not the service
- Do not add new dependencies to pom.xml without asking
- Do not add frontend dependencies without asking (`frontend/package.json`)

### Isolamento entre features (package-by-feature)

Cada pacote de feature é uma fatia vertical. O que pode cruzar a fronteira:

- ✅ A **interface** `XService` de outra feature, injetada por construtor
- ✅ A **entidade** de outra feature, quando há relação JPA real (`Item` → `Categoria`)
- ✅ Qualquer coisa de `common/` e `security/`

O que **não** pode:

- ❌ Injetar o `XRepository` de outra feature — passe pelo `XService` dela
- ❌ Injetar `XServiceImpl` em vez da interface
- ❌ Usar DTOs de request/response de outra feature; cada feature define os seus
- ❌ Um controller acessar repositório direto
- ❌ Dependência circular entre features — se A e B precisam uma da outra, o pedaço comum sobe
  para `common/` ou vira uma feature nova

Quando outra feature precisa da entidade para montar uma relação JPA, o service dono pode expor
`buscarEntidadePorId` na interface (`LojaService`, `PlataformaService`). É o caminho para
`CanalDeVenda` apontar para `Loja` e `Plataforma` sem injetar repositório alheio. O retorno é
só para uso entre services: controller continua devolvendo ResponseDTO.

`integracoes/` (iFood, AnotaAi) **compõe** `PedidoService` — traduz o formato externo para
`RegistrarPedidoExternoRequest` e delega para `registrarPedidoExterno`. Nunca
`implements PedidoService`, nunca duplica regra de negócio. Não é o `AdicionarPedidoRequest`
porque esse é o corpo do `POST /pedidos`: abrir preço e nome livres nele deixaria o operador
registrar venda de balcão com preço arbitrário. O status vindo de evento entra por
`PedidoService.aplicarStatusExterno`. Os clients e o translator de `integracoes/ifood/` só falam
com o iFood e traduzem formato: não injetam repositório nem service de outra feature. Quem compõe
é `IfoodIngestaoServiceImpl`, que injeta as interfaces `PedidoService` e `CanalDeVendaService` e
o repositório da própria inbox (`EventoIfoodRepository`).

## Commands

Build tool: **Maven** (via wrapper `./mvnw`) — não há Gradle neste projeto.

```bash
# Run the application
./mvnw spring-boot:run

# Build
./mvnw clean package

# Run all tests
./mvnw clean test

# Run a single test class
./mvnw test -Dtest=ItemServiceImplTest

# Run a single test method
./mvnw test -Dtest=ItemServiceImplTest#adicionarItem_criaItemQuandoCategoriaExiste
```

### Frontend (SPA)

The web UI is a Vite + Vue 3 + TypeScript SPA under `frontend/` (see `frontend/README.md`).

```bash
cd frontend
npm install
npm run dev        # dev server on :5173, proxies API to :8080
npm run build      # outputs to ../src/main/resources/static/ (Spring then serves it)
npm run type-check
npm test           # Vitest em modo watch
npm run test:run   # Vitest uma passada só (CI / verificação)
```

Package manager: **npm** (há `package-lock.json`) — não usar pnpm ou yarn.

The build output is git-ignored — run `npm run build` before `./mvnw package` to embed the UI in the jar.

## Convenções de Commit

Conventional Commits, **assunto em português**, imperativo ou substantivado, sem ponto final:

```
<tipo>: <descrição curta em português>
```

Tipos usados neste repositório:

| Tipo | Quando usar |
|------|-------------|
| `feat` | Nova funcionalidade visível para o usuário (endpoint, tela, regra de negócio) |
| `fix` | Correção de bug em comportamento existente |
| `refactor` | Reorganização sem mudança de comportamento (ex.: package-by-layer → package-by-feature) |
| `test` | Adição ou ajuste de testes, sem mexer em código de produção |
| `docs` | Documentação (README, CLAUDE.md, `docs/integracao-ifood.md`) |
| `chore` | Build, dependências, `.gitignore`, configuração de tooling |
| `wip` | Ponto de salvamento intermediário numa branch de feature (não usar na `main`) |

Regras:
- Um commit por unidade lógica. Backend e frontend da mesma feature podem ir juntos; mudança de tooling vai separada.
- Escopo é opcional e, quando usado, é o pacote da feature: `feat(pedido):`, `fix(fechamentodecaixa):`.
- Não commitar `.env`, `target/`, `node_modules/` nem `src/main/resources/static/` (build da SPA, git-ignored).
- `documentosDoEstagio/` fica intencionalmente untracked.
- Só commitar quando o usuário pedir. Se estiver na `main`, criar branch antes.

## Testes

Stack já disponível — **não adicionar dependências novas de teste sem perguntar**:

- **Java**: JUnit Jupiter 6.0.3 (Spring Boot 4; a API é a mesma do JUnit 5, `org.junit.jupiter.api`), Mockito 5.20 (`mockito-core` + `mockito-junit-jupiter`), AssertJ 3.27. Vêm dos starters modulares `spring-boot-starter-*-test` do Boot 4.
- **TypeScript**: Vitest 5 + `@vue/test-utils` 2 + jsdom. Config no bloco `test` de `frontend/vite.config.ts`; arquivos `*.spec.ts` ao lado do código testado.

Sem banco nos testes unitários: repositórios são mockados. Ver `/gen-tests`.

**Estado atual (15/10/2026):** 263 testes no Java, todos passando (`./mvnw test`), mais 2
ignorados na suíte normal (`IfoodApiRealTest`).

| Classe de teste | Cobre |
|---|---|
| `FechamentoServiceImplTest` (29) | taxa, taxa congelada, taxa de entrega somada, descontada ou ignorada, ticket médio, ranking (inclusive item externo, sem misturar com item de cardápio de mesmo nome, e nome gravado na venda), agrupamentos, marcação do período |
| `PedidoServiceImplTest` (56) | congelamento das taxas (a da plataforma vem do canal de venda da loja), taxa de entrega congelada com a regra do canal ou somada quando a loja entregou, imutabilidade do pedido fechado, preço travado, nome do item gravado, status; `registrarPedidoExterno` (itens livres sem consultar o cardápio, taxas congeladas, idempotência, validação, canal inativo); recusa de edição, de mudança de status e de exclusão de pedido externo pelo caminho público; `aplicarStatusExterno` (não encontrado, fechado sem alteração e com WARN, fora de ordem, repetido, terminal, aplica, cancela de qualquer etapa não terminal) |
| `UsuarioServiceImplTest` (15) | invariante do último gerente, cadastro, atualização parcial, ativação |
| `UsuarioMasterServiceImplTest` (14) | usuário master: cria gerente ativo com senha codificada só com a tabela vazia; tabela não vazia não consulta mais nada; login ou senha ausentes ou em branco não criam; senha fora da regra do cadastro não cria |
| `InicializadorUsuarioMasterTest` (9) | runner repassa as propriedades `admin.*` ao service, aplica o nome padrão e não derruba a subida em nenhum resultado |
| `CanalDeVendaServiceImplTest` (22) | unicidade loja × plataforma, tratamento da taxa de entrega (padrão e alteração), unicidade e normalização do `merchantId`, atualização parcial, recusa de canal ou loja inativos, busca do canal ativo por `merchantId` |
| `EventoPedidoIfoodTest` (21) | cada código do iFood (curto e por extenso) no `StatusPedido` decidido; código desconhecido, sem equivalente ou nulo devolve vazio |
| `IfoodAutenticadorTest` (16) | obtém o token por formulário, reutiliza enquanto vale, renova com margem e depois de expirar (relógio injetado), sem credenciais não chama HTTP, 401/5xx/falha de conexão viram `IntegracaoIfoodException` sem o corpo cru, log sem token nem segredo |
| `IfoodLojaClientTest` (5) | envia `Authorization: Bearer`, desserializa a lista, 401 descarta o token, erro de servidor, sem credenciais |
| `IfoodEventoClientTest` (9) | polling envia o Bearer e desserializa cada evento, guarda o JSON cru, 204 vira lista vazia, `createdAt` inválido vira nulo, 401 descarta o token, erro de servidor; acknowledgment envia os ids no corpo, não chama HTTP sem evento, recusa do iFood |
| `IfoodPedidoClientTest` (3) | busca do pedido desserializa cliente, itens e horário; 404 sem o corpo cru; 401 descarta o token |
| `IfoodPedidoTranslatorTest` (16) | loja, plataforma, forma de pagamento e identidade externa; taxa de entrega e quem entregou; horário UTC para o fuso da loja; opcional embutido no preço unitário; linha com quantidade 1 quando o total não divide exato ou a quantidade é fracionada; nome limitado e nome padrão; pedido sem id, sem itens ou item sem preço |
| `IfoodIngestaoServiceImplTest` (18) | inbox: grava evento novo como pendente, confirma só depois de gravar, não regrava reentrega, não confirma se a gravação falha, limita o payload; sem forma de pagamento não chama nada; processamento: `PLACED` registra, status aplica, status antes do pedido registra e aplica, sem equivalente ou sem `orderId` é ignorado, falha do iFood mantém pendente até o limite de tentativas, falha de regra de negócio marca `FALHOU`, uma falha não trava os eventos seguintes |
| `IfoodPollingJobTest` (3) | recebe e depois processa; falha do polling não impede o processamento; nenhuma exceção escapa do job |
| `IfoodApiRealTest` (2, ignorados) | chama o iFood de verdade (token + listagem de lojas; polling de eventos **sem** acknowledgment), só leitura; só roda com `IFOOD_TESTE_REAL=true` |
| `LojaServiceImplTest` (7) | cadastro, nome único, invariante da única loja ativa, loja inexistente |
| `RefreshTokenServiceImplTest` (8) | rotação, expiração, recusa de usuário inativo, revogação |
| `JwtFilterTest` (5) | token de usuário desativado não autentica; assinatura inválida; header ausente |
| `RespostaNaoAutorizadaTest` (6) | entry point responde 401 e access denied handler responde 403, ambos com corpo JSON `{ "error": ... }` |
| `CdpApplicationTests` (1) | smoke test de subida do contexto (exige MariaDB no ar) |

Sem teste ainda: `ItemServiceImpl`, `CategoriaServiceImpl`, `PlataformaServiceImpl`,
`FormaDePagamentoServiceImpl`, e os controllers. O frontend tem só
`frontend/src/utils/formato.spec.ts` (7), `frontend/src/utils/periodo.spec.ts` (8),
`frontend/src/utils/rotaInterna.spec.ts` (10), `frontend/src/api/http.spec.ts` (15),
`frontend/src/components/CarrinhoEditor.spec.ts` (7) e
`frontend/src/views/LoginView.spec.ts` (10). Não afirme cobertura além do que a tabela acima lista.

## Environment Setup

The app requires a `.env` file in the project root (use `.env.example` as template) with:

```
URL_BD=jdbc:mariadb://localhost:3306/zeropapel
USER_BD=root
SENHA_BD=root
JWT_SECRET=<base64-encoded-secret>
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGIN=http://localhost:5173
ADMIN_USUARIO=admin
ADMIN_SENHA=<senha-do-primeiro-gerente>
ADMIN_NOME=Administrador
IFOOD_CLIENT_ID=<client id do aplicativo Centralizado>
IFOOD_CLIENT_SECRET=<client secret do aplicativo Centralizado>
IFOOD_POLLING_ATIVO=false
IFOOD_FORMA_DE_PAGAMENTO_ID=<id da forma de pagamento dos pedidos do iFood>
```

The `.env` is loaded by the application itself: `application.properties` has
`spring.config.import=optional:file:.env[.properties]`, so `./mvnw spring-boot:run` and
`./mvnw test` work without exporting anything. Things to know:

- The path is relative to the **working directory** (the project root for Maven and surefire).
  Running the jar from another directory does not find it — pass real environment variables there.
- `optional:` is what lets CI and production run with no `.env` at all. Do not remove it.
- Precedence: JVM system property (`-DJWT_SECRET=...`) > real environment variable > `.env` >
  defaults in `application.properties`. A variable exported in the shell or the IDE always wins.
- The file is parsed as Java **properties**, not as shell: no quotes, no `export`, and a backslash
  in a value must be doubled. CRLF line endings are fine.
- `CORS_ALLOWED_ORIGIN` is a single origin. With `npm run dev` it must be `http://localhost:5173`
  (the browser sends that `Origin` on POST and the Vite proxy does not rewrite it); the SPA served
  from the jar is same-origin and never hits CORS.
- `ADMIN_*`, `SEED_SENHA` and `IFOOD_*` are optional; the other six are required and the app
  fails to start without them.
- `IFOOD_CLIENT_ID` / `IFOOD_CLIENT_SECRET` are the credentials of the **Centralizado** app in the
  iFood developer portal (app-level, not per store). Blank: the app starts normally and
  `IfoodAutenticador.obterToken` throws `CredenciaisIfoodAusentesException` without calling iFood.
  `IFOOD_BASE_URL` is optional and defaults to `https://merchant-api.ifood.com.br`.
- `IFOOD_POLLING_ATIVO` (default `false`) turns the ingestion job on; `IFOOD_POLLING_INTERVALO_MS`
  defaults to 30000. `IFOOD_FORMA_DE_PAGAMENTO_ID` is the `FormaDePagamento` stored on every iFood
  order and is required with the polling on: without it each cycle logs a WARN and does nothing
  (no polling, no acknowledgment).

Requires a running **MariaDB** instance.

### Usuário master

`InicializadorUsuarioMaster` (`usuario/inicializacao/`, um `ApplicationRunner`, portanto depois do
Flyway) chama `UsuarioService.criarUsuarioMasterSeNaoHouverUsuarios` em toda subida. Só age com
a tabela `usuarios` **vazia** (usuário inativo também conta como existente): cria um `GERENTE`
ativo com `ADMIN_USUARIO`, `ADMIN_SENHA` e `ADMIN_NOME` (padrão "Administrador"), pelo mesmo
`cadastrarUsuario` e validando o mesmo `CadastrarUsuarioRequest` do `POST /usuarios`. A checagem e
a criação ficam numa única transação.

**Não existe senha padrão.** Tabela vazia sem `ADMIN_USUARIO` ou `ADMIN_SENHA` (ausentes ou em
branco): a aplicação sobe e loga um WARN, sem criar ninguém. Senha fora da regra do cadastro:
loga ERROR e não cria. A senha e o hash nunca vão para o log. O `scripts/popular_banco.py`
continua sendo só o seed de dados de demonstração.

O schema é versionado com **Flyway** (`src/main/resources/db/migration/`), não mais derivado
pelo Hibernate. `ddl-auto=validate`: a aplicação não sobe se entidade e schema divergirem.
Toda mudança de schema é uma migration nova — ver `/migracao`. Nunca editar migration já
aplicada.

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
  enumerator/         → Cargo (GERENTE | OPERADOR), ResultadoUsuarioMaster
  inicializacao/      → InicializadorUsuarioMaster (cria o primeiro gerente na subida)
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
  enumerator/          → StatusPedido (etapas do kanban, ordem e terminais), ResultadoStatusExterno
plataforma/           → tipos de canal (Balcão, iFood, etc.); sem taxa
loja/                 → lojas (multi-loja); sem DELETE, desativa por `ativo`
canaldevenda/         → vínculo Loja × Plataforma: merchantId, taxa e ativo
formadepagamento/     → formas de pagamento
fechamentodecaixa/    → fechamento de caixa com cálculo de taxas (persiste um registro)
dashboard/            → GET /dashboard: mesmo consolidado, em tempo real e só-leitura
                        (delega para FechamentoService.calcularPrevia; não persiste)

common/
  exception/          → exceções customizadas + GlobalExceptionHandler
  web/                → SpaForwardController (encaminha `/`, `/login` e `/app/**` para index.html;
                        rota de tela nova fora de `/app/**` precisa entrar no `@GetMapping`)

security/             → JwtFilter, JwtService, SecurityConfig, UsuarioDetailsServiceImpl

integracoes/          → integrações externas (iFood em andamento, AnotaAi planejado)
  ifood/
    client/           → IfoodAutenticador (token do app em memória), IfoodLojaClient,
                        IfoodEventoClient (polling + acknowledgment), IfoodPedidoClient
    config/           → IfoodConfig (RestClient com timeouts, os beans acima, @EnableScheduling)
    dto/request/      → ConfirmarEventoIfoodRequest
    dto/response/     → TokenIfoodResponse, LojaIfoodResponse, EventoIfoodResponse, PedidoIfoodResponse
    entity/           → EventoIfood (inbox, tabela ifood_evento)
    enumerator/       → EventoPedidoIfood (código do evento → StatusPedido), SituacaoEventoIfood
    exception/        → IntegracaoIfoodException, CredenciaisIfoodAusentesException
    job/              → IfoodPollingJob (@Scheduled, só existe com ifood.polling.ativo=true)
    repository/       → EventoIfoodRepository
    service/          → IfoodIngestaoService (+ Impl): recebe, confirma e processa os eventos
    translator/       → IfoodPedidoTranslator (PedidoIfoodResponse → RegistrarPedidoExternoRequest)
  anotaai/            → ainda não existe
```

The web UI lives in `frontend/` (Vite + Vue 3 + TypeScript SPA) and is built into
`src/main/resources/static/`. See `frontend/README.md`.

## Naming Conventions

### Java
- Domain classes (entities, services, DTOs): Portuguese
  e.g. `ItemService`, `UsuarioResponseDTO`, `RecursoNaoEncontradoException`
- Variables and methods: camelCase Portuguese
  e.g. `buscarPorId()`, `nomeCompleto`
- Database columns: snake_case Portuguese
  e.g. `data_criacao`, `categoria_id`
- Package names: all lowercase, no camelCase
  e.g. `formadepagamento`, `fechamentodecaixa`
- Request DTOs: `AdicionarXRequest` / `AtualizarXRequest`; response DTOs: `XResponseDTO`.
  Todos são `record` com Bean Validation (`@NotBlank`, `@NotNull`, `@DecimalMin`) nos requests.
- Services expõem interface `XService` + `XServiceImpl`; injeção por construtor, campos `final`,
  sem `@Autowired`.

### TypeScript (frontend/)
- Arquivos: `PascalCase.vue` para componentes e views (`PedidosView.vue`, `AbaCardapio.vue`),
  `camelCase.ts` para módulos (`http.ts`, `formato.ts`, `useToast.ts`).
- Componentes reutilizáveis usam prefixo `Base` (`BaseButton`, `BaseModal`); views terminam em
  `View`; abas de Configurações em `views/config/` usam prefixo `Aba`.
- Identificadores em português, igual ao backend (`carregarPedidos`, `plataformaSelecionada`).
- Import interno sempre pelo alias `@/` (`@/api`, `@/stores/auth`) — nunca `../../`.
- Vue 3 com `<script setup lang="ts">`.

### Tipagem estrita (TypeScript)
`strict` vem de `@vue/tsconfig/tsconfig.dom.json` e não deve ser afrouxado. Além disso:

- **Proibido `any`** — em dado externo use `unknown` e estreite com type guard, como
  `extrairMensagem` em `api/http.ts` faz com o corpo do erro.
- **Proibido `as` para calar o compilador.** As únicas asserções aceitas são as da fronteira
  HTTP em `http.ts` (`(await res.json()) as T`), onde o tipo é garantido pelo contrato do backend.
- **Proibido `@ts-ignore` / `@ts-expect-error`** em código de produção.
- `frontend/src/api/types.ts` é o espelho tipado dos DTOs do backend e a **única** fonte de tipos
  de API. Nomes de campo idênticos aos que o Jackson serializa. Nenhuma view declara a forma de
  uma resposta por conta própria.
- Enums do backend viram union de string literal (`type Cargo = 'GERENTE' | 'OPERADOR'`), não `enum`.
- Campos opcionais no request (PATCH parcial) usam `?`; campos nullable na response usam `| null`.
- Toda chamada de API passa pelos objetos de `api/index.ts` (`pedidosApi`, `itensApi`, …).
  Nenhum `fetch` solto em componente.
- `npm run type-check` precisa passar limpo antes de considerar uma tarefa concluída.

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
- **Plataforma** — channel type (e.g. Balcão, iFood): just `nome` and the `entrega` flag (true = delivery channel like iFood/AnotaAi, has an `EM_ROTA` step; false = counter, used by the PDV). No fee here anymore — it lives on `CanalDeVenda`; table `plataforma`
- **Loja** — store with unique `nome` and `ativo`; every order belongs to one. No DELETE: a store is deactivated via `ativo`, and the only active store cannot be deactivated; table `loja` (V5 seeds 'Loja principal')
- **CanalDeVenda** — link Loja × Plataforma with `merchantId` (unique, null for Balcão), `taxaPercentual` (the fee of this store on this channel), `tratamentoTaxaEntrega` (`DESCONTAR` | `SOMAR` | `IGNORAR`, default `DESCONTAR`: what to do with the delivery fee when the platform delivered) and `ativo`; unique `(loja, plataforma)`; table `canal_de_venda`. An order can only be registered through an active channel of an active store (`CanalDeVendaService.buscarCanalAtivo`)
- **FormaDePagamento** — payment method with configurable `taxaPercentual`; table `forma_de_pagamento`
- **Pedido** — order with `nomeCliente` (nullable), `horarioPedido`, `valor` (stored at time of order), `status` (`StatusPedido` enum), `taxaPlataformaPercentual` e `taxaPagamentoPercentual` (congeladas na venda), FK to `Loja` (set at creation, never changes), `Plataforma` and `FormaDePagamento`; table `pedidos`. `AdicionarPedidoRequest` is `(lojaId, plataformaId, nomeCliente, formaDePagamentoId, itens)`. New orders start `EM_ABERTO`; lifecycle `EM_ABERTO → ACEITO → EM_PREPARO → PRONTO → EM_ROTA → CONCLUIDO` (balcão skips `EM_ROTA`), plus `CANCELADO`. Advance via `PATCH /pedidos/{id}/status`. `plataformaPedidoId` (nullable) is the order id on the external platform (the iFood `orderId`); null for PDV orders; unique `(plataforma_id, plataforma_pedido_id)` (V9)
- **Pedido** — campo `fechado` marcado pelo `POST /fechamento`; pedido fechado não aceita mais alteração
- **Pedido** — `taxaEntrega` e `tratamentoTaxaEntrega` (V12), congelados na venda: pedido de balcão ou sem taxa fica com zero e `IGNORAR`. Ver "Taxa de entrega" abaixo
- **ItemPedido** — line of a `Pedido`; stores `nomeItem`, `quantidade` and `precoUnitario`, all three always filled and locked at order time; FK to `Item` is **optional** (set by the PDV, null on an external order — never assume `getItem()` is non-null, and read the name from `nomeItem`, not from `item.getNome()`); table `itens_pedido` (V10)

### Regra de faturamento

`PedidoRepository.findFaturaveisDoPeriodoComItens` é a **única** porta de entrada do consolidado
(fechamento e dashboard) e exclui `CANCELADO`. Pedido cancelado não entra em faturamento, taxa,
ticket médio nem ranking. Se precisar de outro recorte, crie outra query — não afrouxe essa.

Todo cálculo de dinheiro é `BigDecimal` do início ao fim, sem passar por `double`. Taxa é
`valor.multiply(percentual).divide(CEM, 2, HALF_UP)`; a taxa é arredondada **por pedido** e só
depois somada, porque é assim que a plataforma e a adquirente cobram — não troque para somar e
arredondar no fim, isso quebra a reconciliação com o extrato.

### Taxa de entrega

Decisão 11 de `docs/integracao-ifood.md`. `PedidoServiceImpl.registrarPedidoExterno` resolve o
tratamento na venda e o grava no pedido: entrega feita pela plataforma usa
`CanalDeVenda.tratamentoTaxaEntrega`; entrega feita pela loja é sempre `SOMAR`. O
`RegistrarPedidoExternoRequest` traz `taxaEntrega` e `entregaPelaPlataforma`; o translator do iFood
preenche com `total.deliveryFee` e `delivery.deliveredBy != MERCHANT`.

No `FechamentoServiceImpl`, `SOMAR` entra no bruto (e portanto no ticket médio e na curva horária)
e na base da taxa de pagamento, mas **não** na base da comissão do canal; `DESCONTAR` entra em
`totalTaxas` e em `taxaPlataforma` do agrupamento por plataforma; `IGNORAR` não mexe no caixa.
`Pedido.valor` continua sendo só a soma dos itens. Como as taxas percentuais, o fechamento lê o
tratamento gravado no pedido e nunca o do canal.

### Imutabilidade do histórico fechado

`POST /fechamento` marca `Pedido.fechado = true` em todos os pedidos do período. A partir daí o
pedido é **imutável**: `atualizarPedido`, `atualizarStatus` e `excluirPedido` recusam com
`ConflitoException` (409), e `aplicarStatusExterno` devolve `PEDIDO_FECHADO` sem alterar. Sem isso, apagar ou editar um pedido de uma noite já fechada faz o
`buscarFechamentoPorId` devolver um resumo congelado que não fecha com o breakdown recalculado.

A flag existe em vez de uma consulta a `fechamentos_caixa` de propósito: `pedido` consultando o
pacote `fechamentodecaixa`, que já consulta `pedido`, seria dependência circular entre features
— proibida mais acima neste arquivo. A marcação é feita por `fechamentodecaixa`, que já tem
acesso ao `PedidoRepository`.

Cancelar um pedido é `PATCH /pedidos/{id}/status` com `CANCELADO`, antes do fechamento.
`DELETE /pedidos/{id}` continua sendo exclusão física e só funciona em pedido ainda não fechado.

As taxas são **congeladas no pedido** no momento da venda, em
`Pedido.taxaPlataformaPercentual` e `Pedido.taxaPagamentoPercentual`, exatamente como
`ItemPedido.precoUnitario`. `FechamentoServiceImpl` lê essas colunas e **nunca** navega para
`pedido.getPlataforma().getTaxaPercentual()` — fazer isso faz um reajuste de taxa reescrever
todo o histórico já fechado. `CanalDeVenda.taxaPercentual` e `FormaDePagamento.taxaPercentual`
são editáveis na tela de Configurações e valem só para vendas novas.

A taxa da plataforma vem do **canal de venda** (loja × plataforma), não mais de
`Plataforma.taxaPercentual`, que deixou de existir (V8). `PedidoServiceImpl` chama
`CanalDeVendaService.buscarCanalAtivo(lojaId, plataformaId)` e congela
`canal.getTaxaPercentual()` no pedido. Ao trocar a plataforma de um pedido, o canal é buscado
na loja do próprio pedido: a loja do pedido não muda.

### Pedido de plataforma externa

`PedidoService.registrarPedidoExterno(RegistrarPedidoExternoRequest)` é a porta de entrada da
integração e **não é exposto por controller**. Como o `@Valid` do MVC não roda, o próprio service
valida o request com `jakarta.validation.Validator` e recusa com `RequisicaoInvalidaException`.
Canal ativo, congelamento das duas taxas, cálculo do valor e status inicial `EM_ABERTO` são os
mesmos métodos privados do `registrarPedido`; só a origem dos itens muda (nome e preço vêm do
request, `item` fica nulo, o cardápio não é consultado).

É **idempotente** por `(plataformaId, plataformaPedidoId)`: se o pedido já existe, devolve o
existente sem gravar e sem alterar nada, porque o polling do iFood reentrega eventos. A unique
do banco é a segunda barreira. No MariaDB, linhas com `plataforma_pedido_id` nulo não colidem
entre si, então pedido de balcão não é afetado pela unique.

Pedido com `plataformaPedidoId` **não é operado pelo painel**: o ciclo de vida dele é ditado
pelos eventos da plataforma. `atualizarPedido`, `atualizarStatus` e `excluirPedido` (os métodos
que os controllers chamam) recusam com `ConflitoException` (409), e a SPA esconde Editar,
Avançar, Mudar etapa e Excluir, mostrando o badge "Externo". A checagem de pedido fechado vem
antes: pedido externo já fechado responde com a mensagem do fechamento. O `POST /pedidos` do PDV
continua exigindo `itemId` do cardápio e travando o preço do cardápio.

O status de pedido externo muda só por
`PedidoService.aplicarStatusExterno(plataformaId, plataformaPedidoId, status)`, também **sem
controller**, que devolve `ResultadoStatusExterno` e nunca lança por causa do estado do pedido
(argumento nulo ou em branco é `RequisicaoInvalidaException`):

- `PEDIDO_NAO_ENCONTRADO`: o evento de status pode chegar antes do pedido; quem chama decide
  se tenta de novo depois.
- `PEDIDO_FECHADO`: evento tardio em pedido que já entrou num fechamento. **Não altera nada**,
  só loga WARN com plataforma, id externo, status atual e status recebido. A imutabilidade do
  histórico fechado não é reaberta.
- `IGNORADO`: o status não avança. Eventos chegam repetidos e fora de ordem; `CONCLUIDO` e
  `CANCELADO` são terminais, e fora isso só vale status à frente do atual na ordem do ciclo de
  vida, ou `CANCELADO`. A regra mora em `StatusPedido.podeAvancarPara`.
- `APLICADO`: status gravado. Pular etapas é normal (o iFood não informa todas).

O mapeamento de evento do iFood para `StatusPedido` fica em
`integracoes/ifood/enumerator/EventoPedidoIfood` (o pacote `pedido` não conhece códigos do
iFood): `PLACED`/`PLC` → `EM_ABERTO`, `CONFIRMED`/`CFM` → `ACEITO`, `DISPATCHED`/`DSP` →
`EM_ROTA`, `CONCLUDED`/`CON` → `CONCLUIDO`, `CANCELLED`/`CAN` → `CANCELADO`. Qualquer outro código
devolve `Optional.empty()`. Não acrescente evento ao mapeamento sem decisão registrada em
`docs/integracao-ifood.md`.

`CanalDeVendaService.buscarCanalAtivoPorMerchantId` é como a ingestão vai descobrir loja e
plataforma a partir do `merchantId` do evento; aplica as mesmas regras de canal e loja inativos
do `buscarCanalAtivo`.

### Cliente do iFood

Credencial é do aplicativo, não do canal (decisão 7 do doc). `IfoodAutenticador` pede o token em
`POST /authentication/v1.0/oauth/token` (formulário `grantType=client_credentials`, `clientId`,
`clientSecret`; resposta `accessToken`, `type`, `expiresIn` em segundos), guarda em memória e
renova 60 s antes de expirar, com uma única renovação por vez. `IfoodLojaClient.listarLojas` é
`GET /merchant/v1.0/merchants`. O `RestClient` é montado à mão em `IfoodConfig`
(`RestClient.builder()` com `JdkClientHttpRequestFactory`, conexão 5 s e leitura 15 s): o
`spring-boot-restclient` não está no classpath, então não existe `RestClient.Builder`
autoconfigurado para injetar.

Erro do iFood vira `IntegracaoIfoodException` com o status HTTP e sem o corpo da resposta nem a
exceção original como causa. **Nunca** logar `clientSecret`, token ou o header `Authorization`.
Nada aqui é exposto por endpoint.

### Ingestão do iFood

`IfoodPollingJob` roda a cada 30 s e só existe com `ifood.polling.ativo=true`. Cada rodada chama
`IfoodIngestaoService.receberEventos` e depois `processarEventosPendentes`; nenhuma exceção sai
do job.

- **Receber:** `GET /events/v1.0/events:polling` (204 = nada novo), grava cada evento ainda não
  visto em `ifood_evento` (V11) como `PENDENTE`, com o JSON cru em `payload`, e só então confirma
  todos em `POST /events/v1.0/events/acknowledgment`. Não há transação em volta: cada `save`
  comita sozinho, então o acknowledgment nunca vai antes de o evento estar gravado. Falha ao
  gravar interrompe a rodada sem confirmar e o iFood reentrega.
- **Processar:** até 200 pendentes por rodada, em ordem de `criado_em`. Código sem equivalente em
  `EventoPedidoIfood` ou evento sem `orderId` vira `IGNORADO`. `PLACED` busca o pedido
  (`GET /order/v1.0/orders/{id}`), traduz e chama `registrarPedidoExterno`. Os demais chamam
  `aplicarStatusExterno`; se o pedido ainda não existe, o pedido é buscado e registrado e o
  status aplicado em seguida (cobre o `PLACED` perdido). Loja e plataforma saem de
  `CanalDeVendaService.buscarCanalAtivoPorMerchantId`.
- **Falha:** `IntegracaoIfoodException` (rede, 5xx, 404 do pedido) mantém o evento `PENDENTE` e
  soma uma tentativa; na décima vira `FALHOU`. Qualquer outra exceção (canal não cadastrado ou
  inativo, pedido recusado pela validação) vira `FALHOU` na hora. Reprocessar é voltar a linha
  para `PENDENTE` no banco; `registrarPedidoExterno` é idempotente.
- **Translator:** uma linha por item do iFood. O preço unitário é `totalPrice / quantity`, com os
  opcionais embutidos, para o valor do pedido fechar com o total dos itens. Se a divisão não é
  exata em duas casas ou a quantidade é fracionada, a linha entra com quantidade 1 e o total
  como preço: o dinheiro tem prioridade sobre a quantidade do ranking. Descontos e taxa de
  serviço do iFood **não** entram no valor do pedido (em aberto no doc); a taxa de entrega é
  tratada à parte (seção "Taxa de entrega"). `createdAt` (UTC) é
  convertido para o fuso do servidor.
- O payload do **pedido** não é guardado: traz dado pessoal do cliente. Só o evento é.

Caminhos, campos do pedido e acknowledgment foram conferidos em 15/10/2026 contra a loja de teste
do Developer Portal (pedido de teste com `isTest: true`). Antes de ligar o polling numa loja
real, falta confirmar a convivência com o Gestor de Pedidos (risco aberto no doc).

No ranking de itens, item de cardápio agrupa pelo id e item externo pelo nome gravado; os dois
grupos nunca se misturam, mesmo com nome igual. `ItemRankingDTO.itemId` e
`ItemPedidoResponseDTO.itemId` são nulos para item externo.

### Exclusão de plataforma e `on delete cascade` no canal

`canal_de_venda.plataforma_id` é `on delete cascade` (V6, e `@OnDelete` na entidade): excluir
uma plataforma leva junto os canais dela. É no banco de propósito — `plataforma` não pode
consultar `canaldevenda` para limpar os canais antes, porque `canaldevenda` já depende de
`plataforma` e isso seria dependência circular entre features. O histórico não corre risco:
`excluirPlataforma` já recusa com `ConflitoException` quando existe pedido vinculado à
plataforma, então o cascade só alcança canais de plataforma que nunca vendeu.

### Security

Stateless JWT auth via `JwtFilter`. Access tokens expire in 24 h; refresh tokens allow renewal without re-login. Endpoint-level authorization uses `@PreAuthorize("hasRole('GERENTE')")`. Access control per resource:

- **Pedido** — write operations (register, update, delete) open to all authenticated users; only `DELETE` requires GERENTE
- **Plataforma / FormaDePagamento** — all write operations restricted to GERENTE; reads open to all authenticated
- **Loja / CanalDeVenda** — all write operations restricted to GERENTE; reads open to all authenticated (the PDV needs the store list)
- **Item / Categoria** — all write operations restricted to GERENTE; reads open to all authenticated
- **Fechamento / Dashboard** — all operations restricted to GERENTE

Public endpoints: `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`.
`LoginResponse` carries `cargo` so the SPA knows the role without a probe request.
Static SPA paths are also public (GET `/`, `/index.html`, `/login`, `/app/**`, `/assets/**`).

### Error Handling

`GlobalExceptionHandler` maps:
- `RecursoNaoEncontradoException` → 404
- `ConflitoException` → 409
- `TokenInvalidoException` → 401
- `MethodArgumentNotValidException` → 400

Requisição não autenticada a rota protegida responde 401 (`security/PontoDeEntradaNaoAutenticado`); papel insuficiente responde 403 (`AuthorizationDeniedException` no `GlobalExceptionHandler`, ou `security/ManipuladorDeAcessoNegado` quando a recusa acontece na cadeia de filtros).

All error responses return JSON `{ "error": "<message>" }`.

## Documentação do estágio (`documentosDoEstagio/`)

Pasta **intencionalmente untracked**, mas é onde vive metade do trabalho. Não é lixo: contém a
entrega acadêmica do estágio supervisionado. Dois scripts sustentam tudo, ambos só com
biblioteca padrão do Python (`zipfile`, `struct`, `re`, `xml.etree`). **Não instalar
python-docx, Pillow nem nada.**

Só fica na raiz da pasta o que está em uso hoje: os dois geradores ativos, o `.docx` de
entrada e o de saída. O resto está separado por papel.

| Caminho | Papel |
|---|---|
| `gerar_diagramas_drawio.py` | Gera os 15 `.drawio` em `diagramas-drawio/`. Ver `/diagrama` |
| `atualizar_relatorio_v2.py` | Monta o `.docx` final a partir do `- CORRIGIDO.docx`. Ver `/relatorio` |
| `Relatório ... - CORRIGIDO.docx` | Entrada do `atualizar_relatorio_v2.py` |
| `Relatório ... - 3o bimestre.docx` | Saída atual, o documento da entrega |
| `diagramas-drawio/LEIA-ME.md` | Mapa figura para arquivo e o que cada correção resolveu |
| `telas/` | As 10 capturas reais da SPA, figuras 16 a 25 (lidas pelo atalho `tela()`) |
| `suc/` | Especificações de Caso de Uso: os `gerar_suc_*.py` e os `.docx` que eles geram. Os geradores usam `sucGerenciarCardapio.docx` como template, e os três ficam juntos aqui |
| `modelos/` | Material de referência que não é gerado: exemplo de relatório, modelo da banca, enunciado |
| `historico/` | Rodadas anteriores, mantidas só como registro: `corrigir_relatorio_estagio.py`, `gerar_plano_estagio.py`, os `inspecionar_docx*.py` e os `.docx` já superados. Nada aqui é para rodar de novo |

Os PNGs dos diagramas **não** ficam nesta pasta: `atualizar_relatorio_v2.py` os lê da pasta
da constante `IMAGENS` (fora do repositório, no OneDrive do autor), que é para onde o passo
manual de exportar do draw.io aponta.

Fluxo, e ele tem um passo manual no meio:

```
editar gerar_diagramas_drawio.py
  -> python gerar_diagramas_drawio.py
  -> validar o XML de cada .drawio com ET.parse
  -> [MANUAL] usuário exporta PNG do draw.io para
              a pasta da constante IMAGENS
  -> ler o PNG com o Read e conferir o desenho
  -> python atualizar_relatorio_v2.py
```

O documento tem 26 figuras: 1 a 15 no capítulo 2 (diagramas), 16 a 25 no capítulo 3 (telas,
capturas reais da SPA em `documentosDoEstagio/telas/`) e 26 no capítulo 4 (workflow BPMN). O capítulo 2 fica em três seções, com 2.3 a 2.6 em paisagem
para os diagramas largos caberem legíveis.

Os diagramas precisam refletir o código de hoje. Antes de mexer em qualquer um, leia as classes
envolvidas: já aconteceu de o diagrama de estado do Pedido mostrar um modelo anterior ao
`StatusPedido` e de o diagrama de classe listar entidade que não existe mais.

## Comandos do Claude Code

Em `.claude/commands/`:

| Comando | O que faz |
|---------|-----------|
| `/gen-feature <nome>` | Gera a fatia vertical completa da feature no Java (entity, repository, service + impl, controller, DTOs) e o espelho no TypeScript (tipos em `api/types.ts` + client em `api/index.ts`) |
| `/revisar-arquitetura` | Revisa o diff contra as regras de isolamento package-by-feature e as regras de Spring Security/JWT deste arquivo |
| `/gen-tests <alvo>` | Gera testes unitários — JUnit Jupiter + Mockito no Java, Vitest no TypeScript |
| `/diagrama <alvo>` | Cria ou ajusta os diagramas do estágio, sempre pelo gerador `documentosDoEstagio/gerar_diagramas_drawio.py` |
| `/relatorio <o quê>` | Atualiza o Relatório de Estágio (.docx) pelo `documentosDoEstagio/atualizar_relatorio_v2.py` |
| `/migracao <o quê>` | Cria migration Flyway com o DDL correto para MariaDB e o ajuste de entidade correspondente |
| `/auditar [área]` | Varredura do projeto inteiro — despacha os auditores em paralelo e consolida por severidade × esforço |

`/revisar-arquitetura` é complementar ao `/code-review` embutido: aquele caça bugs de
correção, este verifica conformidade arquitetural.

Em `.claude/agents/`:

| Subagente | O que faz |
|---|---|
| `revisor-de-entrega` | Confere o .docx contra as figuras e contra o código e devolve só as divergências. Lê artefatos pesados (docx, PNG) fora do contexto principal. Rodar antes de entregar o relatório |
| `auditor-seguranca` | Caça vulnerabilidades — autorização, JWT, refresh token, exposição de dados, configuração. Rodar antes de expor o sistema e ao mexer em `security/` ou `autenticacao/` |
| `auditor-persistencia` | Caça N+1, paginação ausente, índice faltando, transação mal delimitada, algoritmo quadrático e aritmética de dinheiro em ponto flutuante |
