## O que muda

<!-- Uma ou duas frases. O porquê importa mais que o o quê. -->

## Como verificar

<!-- Comando, endpoint ou tela. O que o revisor roda para ver funcionando. -->

## Checklist

- [ ] `./mvnw test` passa
- [ ] `cd frontend && npm run type-check && npm run test:run` passa
- [ ] Mudança de schema veio como migration nova em `db/migration/` (nunca editando uma aplicada)
- [ ] Entidade e migration conferem (`ddl-auto=validate` sobe)
- [ ] Regra de isolamento package-by-feature respeitada (sem repository de outra feature)
- [ ] Sem comentário dentro do código
- [ ] Sem credencial, token ou `.env` no diff
