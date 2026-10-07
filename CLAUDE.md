# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexto do produto

Sistema de gestão de caixa para uma petiscaria. O objetivo central é automatizar dois processos:
1. **Registro de vendas** — o atendente lança vendas de balcão pelo sistema; vendas de plataformas (iFood, etc.) serão capturadas via integração futura
2. **Fechamento de caixa** — dashboard para o gerente consultar o consolidado da noite, já com deduções de taxas de plataforma e forma de pagamento calculadas automaticamente

Implementado: Usuario, Item, Categoria, Pedido, ItemPedido, Plataforma, FormaDePagamento,
FechamentoCaixa (`POST /fechamento` persiste) e o consolidado em tempo real (`GET /dashboard`),
além da SPA em `frontend/`.
Ainda por vir: Entregador; integração iFood/AnotaAi (branch `feat/ifood`, em andamento);
aba Integrações em Configurações.

As classes de integração devem depender de `PedidoService` — elas traduzem o formato externo para `AdicionarPedidoRequest` e delegam para o service, sem duplicar lógica de negócio.

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

`integracoes/` (iFood, AnotaAi) **compõe** `PedidoService` — traduz o formato externo para
`AdicionarPedidoRequest` e delega. Nunca `implements PedidoService`, nunca duplica regra de negócio.
(`IfoodClient` hoje viola isso; é stub WIP a ser refatorado.)

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
| `docs` | Documentação (README, CLAUDE.md, `docs/plano-integracao-ifood.md`) |
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

**Estado atual (06/10/2026):** 58 testes no Java, todos passando (`./mvnw test`).

| Classe de teste | Cobre |
|---|---|
| `FechamentoServiceImplTest` (19) | cálculo de taxa, taxa congelada, ticket médio, ranking, agrupamentos, persistência |
| `PedidoServiceImplTest` (10) | congelamento das taxas no registro e na troca de plataforma, preço travado, status |
| `UsuarioServiceImplTest` (15) | invariante do último gerente, cadastro, atualização parcial, ativação |
| `RefreshTokenServiceImplTest` (8) | rotação, expiração, recusa de usuário inativo, revogação |
| `JwtFilterTest` (5) | token de usuário desativado não autentica; assinatura inválida; header ausente |
| `CdpApplicationTests` (1) | smoke test de subida do contexto (exige MariaDB no ar) |

Sem teste ainda: `ItemServiceImpl`, `CategoriaServiceImpl`, `PlataformaServiceImpl`,
`FormaDePagamentoServiceImpl`, e os controllers. O frontend tem só
`frontend/src/utils/formato.spec.ts`. Não afirme cobertura além do que a tabela acima lista.

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

Requires a running **MariaDB** instance.

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
  enumerator/          → StatusPedido (etapas do kanban)
plataforma/           → plataformas de venda (Balcão, iFood, etc.)
formadepagamento/     → formas de pagamento
fechamentodecaixa/    → fechamento de caixa com cálculo de taxas (persiste um registro)
dashboard/            → GET /dashboard: mesmo consolidado, em tempo real e só-leitura
                        (delega para FechamentoService.calcularPrevia; não persiste)

common/
  exception/          → exceções customizadas + GlobalExceptionHandler
  web/                → SpaForwardController (encaminha rotas da SPA para index.html)

security/             → JwtFilter, JwtService, SecurityConfig, UsuarioDetailsServiceImpl

integracoes/          → integrações externas (iFood em andamento, AnotaAi planejado)
  ifood/
  anotaai/
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
- **Plataforma** — origin platform (e.g. Balcão, iFood) with configurable `taxaPercentual` and `entrega` flag (true = delivery channel like iFood/AnotaAi, has an `EM_ROTA` step; false = counter, used by the PDV); table `plataforma`
- **FormaDePagamento** — payment method with configurable `taxaPercentual`; table `forma_de_pagamento`
- **Pedido** — order with `nomeCliente` (nullable), `horarioPedido`, `valor` (stored at time of order), `status` (`StatusPedido` enum), `taxaPlataformaPercentual` e `taxaPagamentoPercentual` (congeladas na venda), FK to `Plataforma` and `FormaDePagamento`; table `pedidos`. New orders start `EM_ABERTO`; lifecycle `EM_ABERTO → ACEITO → EM_PREPARO → PRONTO → EM_ROTA → CONCLUIDO` (balcão skips `EM_ROTA`), plus `CANCELADO`. Advance via `PATCH /pedidos/{id}/status`.
- **ItemPedido** — join entity between `Pedido` and `Item`; stores `quantidade` and `precoUnitario` (price locked at order time); table `itens_pedido`

### Regra de faturamento

`PedidoRepository.findFaturaveisDoPeriodoComItens` é a **única** porta de entrada do consolidado
(fechamento e dashboard) e exclui `CANCELADO`. Pedido cancelado não entra em faturamento, taxa,
ticket médio nem ranking. Se precisar de outro recorte, crie outra query — não afrouxe essa.

Todo cálculo de dinheiro é `BigDecimal` do início ao fim, sem passar por `double`. Taxa é
`valor.multiply(percentual).divide(CEM, 2, HALF_UP)`; a taxa é arredondada **por pedido** e só
depois somada, porque é assim que a plataforma e a adquirente cobram — não troque para somar e
arredondar no fim, isso quebra a reconciliação com o extrato.

As taxas são **congeladas no pedido** no momento da venda, em
`Pedido.taxaPlataformaPercentual` e `Pedido.taxaPagamentoPercentual`, exatamente como
`ItemPedido.precoUnitario`. `FechamentoServiceImpl` lê essas colunas e **nunca** navega para
`pedido.getPlataforma().getTaxaPercentual()` — fazer isso faz um reajuste de taxa reescrever
todo o histórico já fechado. `Plataforma.taxaPercentual` e `FormaDePagamento.taxaPercentual`
são editáveis na tela de Configurações e valem só para vendas novas.

### Security

Stateless JWT auth via `JwtFilter`. Access tokens expire in 24 h; refresh tokens allow renewal without re-login. Endpoint-level authorization uses `@PreAuthorize("hasRole('GERENTE')")`. Access control per resource:

- **Pedido** — write operations (register, update, delete) open to all authenticated users; only `DELETE` requires GERENTE
- **Plataforma / FormaDePagamento** — all write operations restricted to GERENTE; reads open to all authenticated
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

Os PNGs dos diagramas **não** ficam nesta pasta: `atualizar_relatorio_v2.py` os lê de
`C:\Users\phrsl\OneDrive\Documentos\Estágio\Diagramas` (constante `IMAGENS`), que é para onde
o passo manual de exportar do draw.io aponta.

Fluxo, e ele tem um passo manual no meio:

```
editar gerar_diagramas_drawio.py
  -> python gerar_diagramas_drawio.py
  -> validar o XML de cada .drawio com ET.parse
  -> [MANUAL] usuário exporta PNG do draw.io para
              C:\Users\phrsl\OneDrive\Documentos\Estágio\Diagramas
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
