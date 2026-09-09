---
description: Atualiza o Relatório de Estágio (.docx) pelo script de cirurgia de XML
argument-hint: "[o que mudar no relatório]"
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(python*), Bash(ls*), Bash(find*)
---

Atualize o relatório de estágio: **$ARGUMENTS**

Se o argumento vier vazio, pergunte o que mudar. **Nunca reescreva o documento inteiro** para
resolver um ajuste pontual.

## O pipeline

| O quê | Caminho |
|---|---|
| Base (formatação já corrigida) | `documentosDoEstagio/Relatório de Estagio Pedro Luz - CORRIGIDO.docx` |
| Script | `documentosDoEstagio/atualizar_relatorio_v2.py` |
| Saída | `documentosDoEstagio/Relatório de Estagio Pedro Luz - 3o bimestre.docx` |
| Imagens dos diagramas | `C:\Users\phrsl\OneDrive\Documentos\Estágio\Diagramas` |
| Capturas de tela | `documentosDoEstagio/telas/` (.jpg, lidas pelo atalho `tela()`) |

O script lê a base, aplica as mudanças e grava a saída. **A base nunca é alterada**, então
rodar de novo é idempotente: mudou o texto, roda outra vez e pronto. Se faltar algum PNG, o
script põe um aviso em negrito no lugar da figura e acrescenta `(PREVIA)` ao nome do arquivo,
para nada passar batido.

Técnica: `zipfile` + `xml.etree.ElementTree`, direto no `word/document.xml`. **Sem python-docx**,
que não está instalado e não deve ser instalado. Imagem nova entra como parte em `word/media/`
mais uma linha em `word/_rels/document.xml.rels`; o tamanho vem do cabeçalho IHDR do PNG lido
com `struct`, sem Pillow.

## Estrutura atual do documento

```
1 INTRODUÇÃO            1.1 proposta e objetivos, 1.2 justificativa
2 DIAGRAMAS             Figuras 1 a 15
  2.1 caso de uso       Fig 1
  2.2 DER               Fig 2                      (DBeaver)
  2.3 classe            Fig 3 arquitetura, 4 domínio, 5 camadas de Pedido
  2.4 sequência         Fig 6 a 11, um por caso de uso
  2.5 estado            Fig 12 Pedido, 13 Item, 14 Usuário
  2.6 implantação       Fig 15
3 TELAS                 Figuras 16 a 25, capturas reais da SPA
  3.1 login             Fig 16
  3.2 PDV               Fig 17
  3.3 painel de pedidos Fig 18
  3.4 dashboard         Fig 19 consolidado, 20 filtro e fechamentos gravados
  3.5 configurações     Fig 21 cardápio, 22 categorias, 23 plataformas,
                            24 formas de pagamento, 25 usuários
4 WORKFLOW AS IS        Figura 26
5 RECURSOS E AMBIENTE
6 CRONOGRAMA            Tabela 1
7 CONCLUSÃO
8 TRABALHOS FUTUROS
REFERÊNCIAS
```

O mapa figura → arquivo `.png` está em `documentosDoEstagio/diagramas-drawio/LEIA-ME.md`.
Se acrescentar ou remover figura, **renumere daí para a frente em ordem decrescente**, senão
"Figura 1" come "Figura 15" no meio da substituição.

## Seções e orientação

Três seções, e é de propósito:

1. retrato, da capa até o fim de 2.2 (carrega `titlePg` e `pgNumType` da capa)
2. **paisagem**, de 2.3 a 2.6, onde estão os diagramas largos
3. retrato, do capítulo 3 em diante

Sem a seção em paisagem a Figura 3 sai com 16 cm de largura e fonte de aproximadamente 1 mm.
Ao criar uma `sectPr` nova, **copie uma que o documento já tem** (`primeiro_sectPr`), para não
perder os `headerReference`. Cada imagem é reduzida no fim para caber na área útil da própria
seção, largura e altura.

## Regras de ABNT que o documento segue

- Legenda **em cima** da figura, fonte **embaixo**, ambas centralizadas em 10 pt
- Referência: alinhada à esquerda, entrelinha simples, espaço entre entradas, título da obra em
  negrito, sem `<>` na URL, acesso como `15 mar. 2026`
- **Só entra na lista o que é citado no texto.** Se acrescentar referência, acrescente a citação
  junto, na lista `CITACOES`. Se remover citação, remova a referência
- Corpo: Arial 12, justificado, entrelinha 1,5, recuo de primeira linha de 1,25 cm

## Antes de dizer que está pronto

1. Rode o script e leia a saída, inclusive a seção de pendências
2. Extraia o texto do `.docx` gerado e confira a estrutura: numeração das figuras contínua,
   nenhuma legenda órfã, capítulos na ordem
3. Confira que toda referência tem citação e toda citação tem referência
4. **Confira que nenhuma afirmação do texto contradiz o código.** Já foi escrito no relatório
   que as regras de negócio estavam cobertas por testes unitários quando existia um único
   arquivo de teste no projeto. Antes de afirmar que algo está pronto, olhe o repositório
5. Lembre o usuário de abrir o documento, clicar no sumário e mandar **Atualizar campo**

## Estilo do texto

Português, terceira pessoa, sem primeira pessoa do singular. **Nada de travessão**: use vírgula,
dois pontos ou parênteses. Nada de adjetivo de marketing ("robusto", "poderoso"). Cada afirmação
técnica precisa ser verificável no código ou nos diagramas.
