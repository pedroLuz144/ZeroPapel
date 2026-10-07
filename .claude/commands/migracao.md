---
description: Cria uma migration Flyway nova, com o DDL correto para MariaDB e o ajuste de entidade correspondente
argument-hint: <o que a migration precisa fazer>
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(./mvnw *), Bash(git status:*), Bash(git log:*), Bash(ls *)
---

Crie uma migration Flyway para: **$ARGUMENTS**

## Regra que não se quebra

**Migration já aplicada nunca é editada.** Se o arquivo já rodou em qualquer banco — o seu, o
da VM — o Flyway guardou o checksum em `flyway_schema_history` e vai falhar na subida com
`Migration checksum mismatch`. Corrigir um erro de uma migration aplicada se faz com uma
**migration nova** que desfaz ou ajusta.

Antes de escrever qualquer coisa, liste o que existe e descubra o próximo número:

```bash
ls src/main/resources/db/migration/
```

Se o arquivo que você ia mexer ainda não foi aplicado em lugar nenhum (acabou de ser criado,
nesta mesma sessão, e o banco local não subiu desde então), aí sim pode editar no lugar.
Na dúvida, crie uma nova.

## Nomenclatura

```
V<n>__<descricao_em_snake_case>.sql
```

Dois underscores entre o número e a descrição — um só, e o Flyway não reconhece. Numeração
sequencial sem buraco: `V1__baseline.sql`, `V2__adicionar_id_ifood_em_cardapio.sql`.
Descrição em português, snake_case, dizendo o efeito.

Local: `src/main/resources/db/migration/`

## MariaDB — o que o DDL precisa respeitar

- Dinheiro é `decimal(10,2)`. Nunca `float`, nunca `double`
- Data e hora é `datetime(6)` — o Hibernate mapeia `LocalDateTime` com precisão de microssegundo
- Boolean é `bit(1)`
- Enum do Java com `@Enumerated(EnumType.STRING)` vira `varchar(N)`, **não** o tipo `enum()`
  nativo do MariaDB. O tipo nativo não aceita valor novo sem `ALTER` e já causou problema neste
  projeto — veja o comentário em `Pedido.status`
- Engine InnoDB e charset `utf8mb4` são o default; não repita em todo `CREATE TABLE`
- `ALTER TABLE` em MariaDB **não é transacional**. Uma migration com dois `ALTER` que falha no
  segundo deixa o primeiro aplicado. Prefira uma mudança por migration

## Coluna nova em tabela com dados

Adicionar `NOT NULL` sem default numa tabela populada falha. O caminho em três passos:

1. `ALTER TABLE x ADD COLUMN y ... NULL;`
2. `UPDATE x SET y = <valor de backfill> WHERE y IS NULL;`
3. `ALTER TABLE x MODIFY COLUMN y ... NOT NULL;`

Se a coluna tem default natural, `ADD COLUMN y ... NOT NULL DEFAULT <v>` resolve em um passo.

## Índice

Toda FK e toda coluna filtrada em query frequente leva índice. Nome no padrão
`idx_<tabela>_<coluna>`. Em tabela grande, avise o usuário que o `ALTER` bloqueia escrita
durante a criação.

## A entidade precisa acompanhar

`ddl-auto` está em `validate`: a aplicação **não sobe** se a entidade e o schema divergirem.
Então toda migration anda junto com o ajuste da `@Entity` correspondente, no mesmo commit.
Confira tipo, nullability e nome de coluna dos dois lados.

Se a mudança afeta o contrato da API, o espelho em `frontend/src/api/types.ts` também entra —
o `CLAUDE.md` manda que ele seja a única fonte de tipos de API.

## Verificar

Sempre rode e mostre a saída:

```bash
./mvnw spring-boot:run
```

A subida limpa é a prova: o Flyway aplica a migration e o Hibernate valida o schema contra as
entidades. Se aparecer `Schema-validation: wrong column type` ou `missing column`, o DDL e a
entidade discordam — conserte antes de considerar pronto, e diga qual dos dois lados estava
errado.

Para conferir o que o Flyway registrou:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

## Relatar

O arquivo criado, o DDL em si, o que mudou na entidade, e a saída da subida. Se a migration
for destrutiva (DROP de coluna ou tabela, mudança de tipo que trunca), **diga isso em
destaque e pergunte antes de rodar** — não existe desfazer.
