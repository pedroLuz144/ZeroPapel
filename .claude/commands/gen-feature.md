---
description: Gera a fatia vertical completa de uma feature (Java package-by-feature + client TypeScript)
argument-hint: <nome-da-feature> [campos e regras em linguagem natural]
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(./mvnw *), Bash(git status:*)
---

Gere a fatia vertical completa da feature **$ARGUMENTS** neste projeto.

Se o argumento vier vazio, pergunte qual é a feature antes de criar qualquer arquivo.

## Passo 0 — Espelhar uma feature existente, não inventar

Antes de escrever, **leia a feature `item/` inteira** como molde:

- `src/main/java/com/goldenpetiscaria/zeropapel/item/entity/Item.java`
- `.../item/repository/ItemRepository.java`
- `.../item/service/ItemService.java` e `ItemServiceImpl.java`
- `.../item/controller/ItemController.java`
- `.../item/dto/request/AdicionarItemRequest.java` e `.../dto/response/ItemResponseDTO.java`

Se a feature nova tiver relação com outra entidade, leia também `categoria/` (é o lado "pai"
da relação em `Item`). O código gerado deve ser indistinguível do que já existe: mesmas
importações, mesma ordem de membros, mesmo estilo de mensagem de erro em português.

Confirme com o usuário os campos da entidade e as regras de acesso antes de gerar, **a menos que**
já tenham sido descritos no argumento.

## Passo 1 — Java

Pacote base: `com.goldenpetiscaria.zeropapel.<feature>` (tudo minúsculo, sem camelCase —
`formadepagamento`, não `formaDePagamento`). Estrutura:

```
<feature>/
  controller/     → <Feature>Controller.java
  dto/request/    → Adicionar<Feature>Request.java, Atualizar<Feature>Request.java
  dto/response/   → <Feature>ResponseDTO.java
  entity/         → <Feature>.java
  enumerator/     → só se a feature tiver enum próprio
  repository/     → <Feature>Repository.java
  service/        → <Feature>Service.java + <Feature>ServiceImpl.java
```

**Entity** — `@Entity`, `@Table(name = "<plural_snake_case>")`, `@Getter @Setter` do Lombok
(nunca `@Data` em entidade), `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`,
colunas em snake_case português. Valores monetários e taxas são `BigDecimal`, nunca `double`.
Enum persistido segue o padrão de `Pedido.status`: `@JdbcTypeCode(SqlTypes.VARCHAR)` +
`@ColumnDefault`, para não cair no `enum()` nativo do MariaDB que o `ddl-auto=update` não sincroniza.

**Repository** — `@Repository`, `extends JpaRepository<Feature, Long>`. Só adicione query
methods que a feature realmente usa agora.

**DTOs** — `record`, sempre. O request de criação leva Bean Validation com mensagem em
português (`@NotBlank(message = "O item precisa de um nome")`). O request de atualização tem
todos os campos opcionais (PATCH parcial, sem `@NotNull`). O ResponseDTO achata as relações
em id + nome (`categoriaId`, `categoriaNome`), nunca aninha outra entidade.

**Service** — interface `<Feature>Service` com os métodos nomeados pela ação de negócio em
português (`adicionarItemAoCardapio`, `visualizarCardapio`), e `<Feature>ServiceImpl` com
`@Service`, dependências `final` injetadas por construtor, sem `@Autowired`. O mapeamento
entity→DTO é um método `private <Feature>ResponseDTO toDTO(...)` no fim do Impl — manual,
sem MapStruct nem ModelMapper. Recurso ausente lança
`RecursoNaoEncontradoException("<Entidade> não encontrado(a) para o ID informado")`;
violação de unicidade ou de regra de negócio lança `ConflitoException`.

**Controller** — `@RestController`, `@RequestMapping("/<plural>")`, injeta a **interface** do
service por construtor. Retorna o ResponseDTO diretamente (o `GlobalExceptionHandler` cuida
dos erros); `@ResponseStatus(HttpStatus.CREATED)` no POST e `NO_CONTENT` no DELETE.
Zero lógica de negócio aqui. `@Valid` em todo `@RequestBody`.

**Autorização** — `@PreAuthorize("hasRole('GERENTE')")` no método do controller, nunca no
service. O padrão do projeto: escrita é de GERENTE e leitura é de qualquer autenticado. A
exceção é `pedido/`, onde a escrita é aberta e só o DELETE exige GERENTE — se a feature nova
for operada pelo atendente durante o expediente, siga o padrão de `pedido/`; se for cadastro
ou configuração, siga o padrão de `item/`. **Explicite qual dos dois você escolheu e por quê.**

Nenhum endpoint novo precisa de ajuste no `SecurityConfig` — `anyRequest().authenticated()`
já cobre. Só mexa lá se a feature exigir rota pública, e nesse caso pergunte antes.

## Passo 2 — TypeScript (frontend/)

**`frontend/src/api/types.ts`** — adicione as interfaces no mesmo bloco comentado do resto
(`// ── <Feature> ─────`), com os nomes de campo **exatamente** como o Jackson serializa.
Enums do backend viram union de string literal. Campos opcionais de PATCH usam `?`; campos
nullable da response usam `| null`. Sem `any`.

**`frontend/src/api/index.ts`** — adicione o objeto `<feature>Api` seguindo o formato dos
existentes:

```ts
export const <feature>Api = {
  listar: () => http.get<XResponse[]>('/<plural>'),
  adicionar: (body: AdicionarXRequest) => http.post<XResponse>('/<plural>', body),
  atualizar: (id: number, body: AtualizarXRequest) => http.patch<XResponse>(`/<plural>/${id}`, body),
  excluir: (id: number) => http.del(`/<plural>/${id}`),
}
```

Só inclua os métodos que existem no controller. Mantenha os imports de tipo em ordem alfabética.

**`frontend/vite.config.ts`** — acrescente o prefixo da rota nova em `PREFIXOS_API`, senão o
dev server na :5173 não encaminha para o Spring na :8080. **Passo fácil de esquecer.**

**Não** gere `.vue` a menos que o usuário peça. O client tipado é a entrega; a tela é decisão
de UX separada. Se gerar, siga `views/config/AbaCardapio.vue` como molde e use
`useToast`/`useConfirm` para feedback.

## Passo 3 — Verificar

Rode e mostre o resultado:

```bash
./mvnw -q compile
cd frontend && npm run type-check
```

Não declare a feature pronta enquanto os dois não passarem.

## Passo 4 — Relatar

Liste os arquivos criados, os arquivos existentes alterados, a decisão de autorização tomada,
e o que ficou de fora (tela, testes, migração de dados). Não commite — a menos que o usuário peça.
