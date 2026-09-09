---
description: Revisa o diff contra as regras de isolamento package-by-feature e de Spring Security/JWT
argument-hint: "[branch|commit para comparar — padrão: main]"
allowed-tools: Read, Grep, Glob, Bash(git diff:*), Bash(git status:*), Bash(git log:*), Bash(git merge-base:*)
---

Revise as mudanças desta branch procurando **violações arquiteturais** deste projeto.

Escopo do diff: compare com `$ARGUMENTS` se algo foi passado; senão use
`git merge-base HEAD main` e revise daí para cá, incluindo o que está no working tree
(`git status` + `git diff`). Arquivos untracked relevantes também contam.

Este comando é **complementar** ao `/code-review` embutido: aquele procura bugs de correção,
este verifica se o código respeita as regras do `CLAUDE.md`. Não duplique o trabalho dele —
não reporte bug de lógica aqui a menos que seja consequência direta de uma violação estrutural.

## Linha de base conhecida (leia antes de reportar isolamento)

O projeto **já nasce** com um conjunto de violações de isolamento. Elas são anteriores a
qualquer branch em revisão e reportá-las de novo a cada execução enterra o achado que importa.

Injeção de repositório de outra feature que já existe hoje:

| Feature | Repositórios de fora que ela injeta |
|---|---|
| `item` | `CategoriaRepository` |
| `categoria` | `ItemRepository` |
| `pedido` | `PlataformaRepository`, `FormaDePagamentoRepository`, `ItemRepository` |
| `plataforma` | `PedidoRepository` |
| `formadepagamento` | `PedidoRepository` |
| `fechamentodecaixa` | `PedidoRepository` |
| `security` | `UsuarioRepository` |

Disso decorrem três ciclos entre features: `item` ↔ `categoria`, `pedido` ↔ `plataforma` e
`pedido` ↔ `formadepagamento`.

Como tratar:

- Se o diff **não** mexe nessas classes, **não reporte** nada disso.
- Se o diff **aumenta** a bagunça (uma feature passa a injetar mais um repositório de fora, ou
  aparece um par circular novo), reporte como achado novo, com severidade alta.
- Se o diff **reduz** (algum acoplamento desses foi trocado por chamada ao service), diga isso:
  é melhoria e merece ser registrada.
- Se o usuário pedir explicitamente um raio-x do débito arquitetural, aí sim liste a tabela
  inteira e proponha a ordem de refatoração.

Mantenha esta tabela atualizada: se uma refatoração eliminar uma linha, apague a linha.

## O que verificar

### 1. Isolamento package-by-feature

Para cada arquivo Java alterado, olhe os **imports**:

- ❌ Import de `<outra_feature>.repository.*` — repositório de outra feature só é acessível
  através do `Service` dela. Não há exceção legítima: quando parece necessária, é sinal de que
  falta um método na interface do service vizinho. **Mas veja a linha de base abaixo antes de
  reportar.**
- ❌ Import de `<outra_feature>.service.*ServiceImpl` — deve ser a interface.
- ❌ Import de `<outra_feature>.dto.*` — cada feature define os próprios DTOs.
- ✅ Import de `<outra_feature>.entity.*` é aceitável quando há relação JPA real.
- ✅ Import de `common.*` e `security.*` é sempre livre.
- ❌ Dependência circular: se a feature A importa de B e B importa de A, reporte.

Verificação rápida do lado repositório:

```bash
git diff --name-only $(git merge-base HEAD main)... -- '*.java'
```

e para cada arquivo, cheque se o pacote do import bate com o pacote do próprio arquivo.

### 2. Camadas

- ❌ Controller com lógica de negócio (`if` de regra, cálculo, laço sobre dados de domínio,
  montagem de DTO). Controller só delega e devolve.
- ❌ Controller injetando `Repository` direto.
- ❌ `@Entity` aparecendo em assinatura de método de controller — retorno ou `@RequestBody`.
  Sempre ResponseDTO / Request record.
- ❌ Mapeamento entity→DTO fora do Service (não pode estar no controller nem na entity).
- ❌ Campo injetado com `@Autowired` em vez de construtor com campo `final`.
- ❌ `@Data` do Lombok em `@Entity` (usar `@Getter`/`@Setter`; `@Data` só em DTO não-record).

### 3. Spring Security / JWT

- ❌ `@PreAuthorize` no **service** em vez do controller. É regra explícita do projeto.
- ⚠️ Endpoint de **escrita** novo (`@PostMapping`, `@PatchMapping`, `@PutMapping`,
  `@DeleteMapping`) **sem** `@PreAuthorize` — confira se é intencional. O padrão do projeto é
  escrita restrita a `hasRole('GERENTE')`, exceto em `pedido/`, onde escrita é aberta a
  autenticados e só o DELETE exige GERENTE. Aponte quando o novo endpoint destoar do padrão
  da feature em que está.
- ⚠️ Endpoint de **leitura** com `@PreAuthorize` mais frouxo que o resto da feature.
- ❌ `hasAuthority('GERENTE')` em vez de `hasRole('GERENTE')` — o projeto usa `hasRole`
  em todo lugar; misturar quebra pelo prefixo `ROLE_`.
- ❌ Papel escrito com string diferente das do enum `Cargo` (`GERENTE`, `OPERADOR`) —
  erro de digitação aqui falha aberto ou fecha tudo, silenciosamente.
- 🚨 Qualquer `permitAll()` novo no `SecurityConfig`. Só devem ser públicos `/auth/**` e os
  GETs do shell da SPA (`/`, `/index.html`, `/login`, `/app/**`, `/assets/**`, `/favicon.ico`).
  Um `permitAll()` em rota de dados é vazamento — reporte como severidade alta.
- 🚨 `csrf().disable()` combinado com sessão que não seja `STATELESS`.
- 🚨 Segredo, senha, token ou string de conexão hardcoded. Devem vir do `.env` via `@Value`.
  Confira também se algo do `.env` vazou para arquivo versionado.
- ❌ Senha manipulada sem `PasswordEncoder`, ou comparada com `equals`.
- ❌ `UsuarioResponseDTO` (ou qualquer response) carregando hash de senha, token ou
  `refreshToken` que não devesse sair.
- ⚠️ Endpoint que recebe um id de usuário e não valida se o solicitante pode acessá-lo
  (IDOR) — em endpoint que não seja de GERENTE.

### 4. Integrações (`integracoes/`)

- ❌ Classe de integração com `implements PedidoService`. Deve **compor**: injetar
  `PedidoService`, traduzir o payload externo para `AdicionarPedidoRequest` e delegar.
- ❌ Regra de negócio (cálculo de taxa, decisão de status) duplicada dentro do client.

`IfoodClient` já viola isso hoje e é stub WIP conhecido — **mencione uma vez, sem inflar**,
e só se o diff mexeu nele.

### 5. TypeScript (`frontend/`)

- ❌ `any`, `@ts-ignore`, `@ts-expect-error`.
- ❌ `as` fora da fronteira HTTP de `api/http.ts`.
- ❌ `fetch` direto em componente ou view — tem que passar por `api/index.ts`.
- ❌ Tipo de resposta de API declarado localmente numa view em vez de `api/types.ts`.
- ❌ Import relativo profundo (`../../`) onde cabe o alias `@/`.
- ⚠️ Campo em `api/types.ts` cujo nome não bate com o DTO Java correspondente — cheque contra
  o record de verdade; divergência aqui vira `undefined` silencioso em runtime.
- ⚠️ Endpoint novo no backend sem o prefixo correspondente em `PREFIXOS_API` de
  `frontend/vite.config.ts` (quebra só no dev server, o que torna fácil não perceber).

### 6. Convenções

- ❌ Nome de pacote com camelCase.
- ❌ Identificador de domínio em inglês onde o resto está em português.
- ⚠️ `double`/`float` para dinheiro ou taxa percentual — deve ser `BigDecimal`.
- ⚠️ Enum novo persistido sem `@JdbcTypeCode(SqlTypes.VARCHAR)` — com `ddl-auto=update` o
  MariaDB não sincroniza a coluna `enum()` depois.

## Como reportar

Antes de reportar, **abra o arquivo e confirme**. Import parecido não é violação; um
`@PreAuthorize` pode estar numa classe-pai. Descarte o que não conseguir confirmar lendo o código.

Agrupe por severidade, mais grave primeiro:

- 🚨 **Alta** — brecha de segurança, dado exposto, quebra de isolamento que vai doer para desfazer
- ⚠️ **Média** — desvio de convenção que gera bug ou retrabalho
- 💡 **Baixa** — inconsistência de estilo

Para cada achado: `arquivo:linha`, a regra violada (cite a seção do `CLAUDE.md`), **por que
importa neste código concreto**, e a correção. Se não achar nada, diga isso e liste o que
foi verificado — não invente achado para parecer produtivo.

Não altere arquivo nenhum. Este comando só relata.
