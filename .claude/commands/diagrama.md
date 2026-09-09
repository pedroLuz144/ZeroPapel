---
description: Cria ou ajusta os diagramas do estágio em .drawio, pelo gerador em Python
argument-hint: <diagrama a criar ou ajustar>
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(python*), Bash(ls*), Bash(find*)
---

Crie ou ajuste **$ARGUMENTS** nos diagramas do relatório de estágio.

Se o argumento vier vazio, pergunte qual diagrama antes de mexer em qualquer coisa.

## Onde tudo mora

| O quê | Caminho |
|---|---|
| Gerador | `documentosDoEstagio/gerar_diagramas_drawio.py` |
| Saída `.drawio` | `documentosDoEstagio/diagramas-drawio/` (15 arquivos) |
| Mapa figura → arquivo | `documentosDoEstagio/diagramas-drawio/LEIA-ME.md` |
| PNG exportados pelo usuário | `C:\Users\phrsl\OneDrive\Documentos\Estágio\Diagramas` |

**Nunca desenhe um `.drawio` na mão.** Todos saem do gerador, para que um ajuste de coordenada
seja uma edição e não um retrabalho. Só biblioteca padrão: `os`, `struct`, `re`,
`xml.etree.ElementTree`. Não instale nada.

## Helpers que já existem no gerador

- `lbl(t)`: escapa o texto. Aplique em todo `value`.
- `v(cid, valor, style, x, y, w, h, parent="1")`: vértice
- `e(cid, valor, style, src, tgt, pontos=None, rotulo=None)`: aresta ligada a dois ids;
  `rotulo` é a posição do label ao longo da aresta, de -1 (origem) a 1 (destino)
- `e_livre(cid, valor, style, x1, y1, x2, y2)`: aresta solta por coordenada
- `rot(cid, aresta, texto, pos)`: rótulo de multiplicidade preso a uma aresta
- `classe(cid, nome, estereotipo, atributos, metodos, x, y, w, fill, stroke)`: classe UML
  com compartimentos; devolve **lista** de células, use `c += classe(...)`
- `estado_inicial(cid, x, y)` / `estado_final(cid, x, y)`: o final é círculo dentro de círculo,
  montado com duas células porque `shape=endState` não é confiável entre versões do draw.io
- `sequencia(...)`: sequência de 5 lifelines com 4 idas e 4 voltas por fragmento
- `sequencia_livre(nome, titulo, lifelines, fragmentos)`: número livre de lifelines,
  mensagens `(de, para, texto, tipo)` com tipo `"c"` chamada, `"r"` retorno, `"s"` a si mesmo
- `gravar(nome, celulas, largura, altura)`: escreve o `.drawio`

Constantes de estilo prontas: `ESTADO`, `CORPO`, `TRANS`, `TRANS_RETA`, `CLS`, `LINHA`,
`DIVISOR`, `ASSOC`, `NAVEG`, `COMPOS`, `REALIZA`, `HERANCA`, `DEP`, `NOTA_CLS`.

## Escape: a parte que quebra em silêncio

Os labels usam `html=1`, então o valor precisa de **escape duplo**: o texto vira entidade HTML
e depois entidade XML. `<` precisa chegar no arquivo como `&amp;lt;`. Se você escapar só uma
vez, `List<Item>` some da figura sem erro nenhum. `lbl()` já faz isso, incluindo aspas para
`&quot;` e `\n` para `<br>`. Use sempre.

## Ao editar o gerador pelo Bash

Heredoc do bash embaralha `\n` dentro de string Python e o arquivo sai com quebra de linha
literal no meio do literal. Ao escrever uma função nova com `\n` em label, monte a string com
um marcador e troque depois:

```python
novo = '''... "Pedido recebido@(Balcão, iFood)" ...'''.replace("@", chr(92) + "n")
```

## Ritual obrigatório antes de dizer que está pronto

1. Rode `python gerar_diagramas_drawio.py`
2. Valide o XML de todos: `ET.parse(f)` em cada `.drawio`
3. Peça ao usuário para exportar o PNG (draw.io: Arquivo > Exportar como > PNG, zoom 200%)
4. **Abra o PNG com o Read e olhe.** Não pule este passo.

O passo 4 não é zelo excessivo. Numa única rodada ele pegou cinco defeitos que o XML válido
não denunciava: cabeça de lifeline deformada por `participant=umlActor` numa caixa larga e
baixa; aresta atravessando por dentro de outra caixa; os dois braços de um gateway BPMN saindo
sobrepostos, sumindo com um rótulo; laço colidindo com o texto de um elemento; e uma seta de
enum saindo da classe errada, que era erro de conteúdo, não de layout.

## Armadilhas conhecidas do draw.io

- `participant=umlActor` desenha o boneco esticado se a lifeline for larga. Ator com `w=80` e
  `size=64`; as outras lifelines são retângulo simples, sem `participant=`.
- Braço de gateway BPMN precisa de `exitX`/`exitY` explícito em cada saída, senão os dois saem
  pelo mesmo ponto e viram uma linha só.
- `pontos=` em aresta com `parent="1"` são coordenadas **absolutas do canvas**, mesmo quando as
  pontas estão dentro de uma raia com coordenada relativa. Some a origem da raia na conta.
- Rótulo de aresta com fundo branco (`labelBackgroundColor=#ffffff`) resolve colisão com linha.

## Coerência com o código

O diagrama tem que refletir o código de hoje, não o de quando foi desenhado. Antes de mexer em
um diagrama de estado, classe ou sequência, **leia as classes envolvidas**. Já aconteceu de o
diagrama de estado do Pedido mostrar um modelo que o `StatusPedido` tinha substituído, e de o
diagrama de classe listar uma entidade que não existia mais.

## Relatar

Diga quais `.drawio` mudaram, **quais PNG precisam ser reexportados** e o que você conferiu.
Se mexeu na numeração das figuras, atualize o `LEIA-ME.md`. Nada de travessão nos textos.
