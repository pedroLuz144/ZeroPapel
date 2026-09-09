---
description: Gera testes unitários — JUnit Jupiter + Mockito no Java, Vitest no TypeScript
argument-hint: <classe, arquivo ou feature a testar>
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(./mvnw *), Bash(cd frontend*), Bash(npm *), Bash(git status:*)
---

Gere testes unitários para **$ARGUMENTS**.

Se o argumento vier vazio, teste o que está no diff atual (`git status` + `git diff`) e diga o
que escolheu cobrir.

## Stack disponível (não adicionar dependências)

**Java** — vem dos starters modulares `spring-boot-starter-*-test` do Spring Boot 4:
- JUnit **Jupiter 6.0.3** — a API é a do JUnit 5 (`org.junit.jupiter.api.*`); o salto de versão
  é do Boot 4, o código de teste é o mesmo
- Mockito 5.20 (`mockito-core` + `mockito-junit-jupiter`)
- AssertJ 3.27

**TypeScript** — `frontend/`:
- Vitest 5, `@vue/test-utils` 2, jsdom
- Config no bloco `test` de `frontend/vite.config.ts`; padrão `src/**/*.spec.ts`

Se um teste parecer exigir biblioteca nova (Testcontainers, MSW, jest-dom), **pare e pergunte** —
`CLAUDE.md` proíbe adicionar dependência sem autorização.

## Java

Local: `src/test/java/com/goldenpetiscaria/zeropapel/<feature>/service/<Feature>ServiceImplTest.java`
— espelhando o pacote da classe testada. Hoje só existe `CdpApplicationTests`; use
`ItemServiceImpl` como referência do que a camada faz.

### Teste de Service (o caso principal)

Unitário puro, **sem contexto Spring e sem banco**:

```java
@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock private ItemRepository repository;
    @Mock private CategoriaRepository categoriaRepository;
    @InjectMocks private ItemServiceImpl service;
```

Nada de `@SpringBootTest` para testar service — sobe o contexto inteiro e exige MariaDB no ar
(o projeto lê `.env` e não tem banco em memória configurado). Também nada de `@DataJpaTest`
sem perguntar antes, pelo mesmo motivo.

Cubra, para cada método público da interface:

1. **Caminho feliz** — verifique o DTO retornado campo a campo e, quando o método persiste,
   capture o argumento com `ArgumentCaptor<Item>` para conferir o que foi montado antes do
   `save`. Só verificar `verify(repository).save(any())` não prova quase nada.
2. **Recurso ausente** — `when(repository.findById(1L)).thenReturn(Optional.empty())` e
   `assertThatThrownBy(...).isInstanceOf(RecursoNaoEncontradoException.class)`. Cheque também
   que nada foi persistido: `verify(repository, never()).save(any())`.
3. **Dependência ausente** — quando o service resolve FK (categoria, plataforma, forma de
   pagamento), teste o ramo em que ela não existe.
4. **Atualização parcial** — os `Atualizar*Request` têm campos opcionais. Teste que um campo
   `null` **preserva** o valor anterior em vez de apagá-lo. É onde esse padrão costuma quebrar.
5. **Regra de negócio própria da feature** — cálculo de taxa em `fechamentodecaixa`, transição
   de `StatusPedido`, `Plataforma.entrega` fazendo pular `EM_ROTA`. Aqui é onde o teste paga:
   priorize sobre CRUD trivial.

### Dinheiro

Valores são `BigDecimal`. Compare com `isEqualByComparingTo`, **não** `isEqualTo` —
`new BigDecimal("10.00")` e `new BigDecimal("10.0")` não são `equals`. Esse é o erro mais comum
ao testar `FechamentoServiceImpl`.

```java
assertThat(resultado.valorLiquido()).isEqualByComparingTo("87.50");
```

### Estilo

- Nome do método: `metodo_comportamentoEsperado_quandoCondicao`
  (`atualizarItem_lancaRecursoNaoEncontrado_quandoIdNaoExiste`)
- `@DisplayName` em português quando o nome não for autoexplicativo
- Estrutura arrange / act / assert, separada por linha em branco
- AssertJ (`assertThat`) em vez de `Assertions.assertEquals`
- `@Nested` para agrupar por método quando a classe passar de ~6 testes
- Um comportamento por teste; sem `if` nem laço na asserção
- Fixtures como métodos `private` no fim da classe, não campos compartilhados mutáveis

### Controller

Só teste controller quando houver algo próprio para verificar — status HTTP, `@PreAuthorize`,
validação de request. Use `@WebMvcTest(XController.class)` com `@MockitoBean XService`, e
`@WithMockUser(roles = "GERENTE")` / `roles = "OPERADOR"` para provar que a restrição de papel
funciona nos dois sentidos: o autorizado passa (200) e o não-autorizado leva 403. Sem isso o
`@PreAuthorize` nunca é realmente exercitado.

Não teste controller que só delega — o teste vira espelho do código.

## TypeScript

Local: ao lado do arquivo testado — `src/utils/formato.spec.ts` para `src/utils/formato.ts`.
Ver `frontend/src/utils/formato.spec.ts`, que já está no repositório como referência.

Imports explícitos de `vitest` (o projeto **não** usa `globals: true`, para o `vue-tsc` continuar
limpo):

```ts
import { describe, expect, it, vi } from 'vitest'
```

Prioridade do que testar:

1. **`utils/`** — funções puras, melhor retorno por esforço
2. **`api/http.ts`** — o fluxo de refresh no 401 é a lógica mais delicada do frontend:
   token renovado e requisição repetida; refresh falhando → sessão limpa e redirect; 401 sem
   refreshToken. Mocke `globalThis.fetch` com `vi.fn()` e `vi.stubGlobal`
3. **`stores/auth.ts`** e composables
4. **Componentes** — só quando houver lógica de verdade (`CarrinhoEditor.vue`). Use
   `mount` do `@vue/test-utils` e teste comportamento observável, não estrutura de DOM

### Armadilhas conhecidas

- **`Intl` usa espaço não-quebrável.** `moeda(18.5)` devolve `R$ 18,50`, não `R$ 18,50`.
  Comparação direta com string digitada falha com uma mensagem enganosa
  (`expected 'R$ 0,00' to be 'R$ 0,00'`). Normalize antes de comparar — `formato.spec.ts` tem
  o helper `semNbsp`.
- **Data**: use `new Date(2026, 7, 30)` (mês 0-based, hora local), não string ISO, ao testar
  `dataLocalISO` / `paraInputDateTime` — senão o teste testa o fuso, não a função.
- Sem `any` também no teste. Mocks tipados: `vi.fn<typeof fetch>()`.
- Restaure globals mockados em `afterEach` (`vi.unstubAllGlobals()`), senão um teste contamina o outro.

## Rodar

Sempre execute e mostre a saída:

```bash
./mvnw test -Dtest=<Classe>Test
cd frontend && npm run test:run
```

Teste que não roda não conta. Se falhar, decida qual lado está errado — teste ou código de
produção — e diga qual, em vez de ajustar a asserção até passar. Se o teste expôs um bug real,
**reporte antes de corrigir**.

## Relatar

Liste os arquivos criados, o que cobrem, a saída da execução, e o que ficou descoberto e por quê.
Não commite — a menos que o usuário peça.
