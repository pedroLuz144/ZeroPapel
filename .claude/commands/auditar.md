---
description: Varredura completa do projeto — segurança, persistência/desempenho e arquitetura — despachando os auditores em paralelo
argument-hint: "[área: seguranca | persistencia | arquitetura | tudo (padrão)]"
allowed-tools: Task, Read, Grep, Glob, Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(./mvnw *), Bash(cd frontend*), Bash(npm *)
---

Faça a varredura de qualidade do ZeroPapel. Área: **$ARGUMENTS** (vazio = tudo).

Isto é uma revisão de **estado do projeto inteiro**, diferente do `/code-review`, que olha o
diff, e do `/revisar-arquitetura`, que olha só conformidade de pacote.

## Despache em paralelo

Numa única mensagem, lance os subagentes da área pedida — eles leem muito arquivo e o ganho
de mantê-los fora do contexto principal é justamente esse:

| Área | Subagente |
|---|---|
| Segurança | `auditor-seguranca` |
| Persistência e desempenho | `auditor-persistencia` |

Para arquitetura, rode `/revisar-arquitetura` você mesmo — ele compara contra as regras de
isolamento do `CLAUDE.md` e não precisa de contexto isolado.

Se um subagente não estiver no registro (foi criado depois do início da sessão), lance um
`general-purpose` mandando ler `.claude/agents/<nome>.md` e seguir aquela definição.

Enquanto os agentes rodam, **não fique esperando**. Verifique você mesmo o que é rápido e não
colide com o escopo deles:

```bash
./mvnw -q clean test
cd frontend && npm run type-check && npm run test:run
```

Cobertura de teste real: conte os arquivos em `src/test/` e em `frontend/src/**/*.spec.ts`
contra o número de services. O `CLAUDE.md` é explícito — **não afirme que as regras de negócio
estão cobertas** sem ter contado.

## Consolidar

Junte tudo numa lista só, ordenada por **severidade × esforço**, não por área. O usuário quer
saber o que atacar primeiro, não ler três relatórios paralelos.

Para cada achado:

1. Severidade e área
2. `arquivo:linha`
3. O mecanismo da falha, em uma frase — não o rótulo
4. Esforço estimado de correção: **P** (minutos), **M** (horas), **G** (dia ou mais)

Deduplique: o mesmo problema visto pelos dois auditores por ângulos diferentes vira um achado
com as duas evidências.

Feche com uma **ordem de ataque sugerida** — a sequência em que corrigir, respeitando
dependência entre as correções (migration antes do ajuste de entidade que depende dela, teste
antes do refactor que ele protege).

## O que não fazer

- Não corrija nada nesta passada. A varredura é diagnóstico; a correção é outra tarefa, e
  misturar as duas faz perder a visão de conjunto
- Não reporte achado não confirmado. Se os auditores devolverem suspeita sem evidência,
  verifique você mesmo ou marque explicitamente como "não confirmado"
- Não repita achado que o usuário já decidiu não corrigir, a menos que a situação tenha mudado
