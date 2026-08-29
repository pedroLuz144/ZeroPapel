# Integração iFood — Decisões de Arquitetura

> Documento vivo. Registra **o que** foi decidido e **por quê**, para servir de âncora
> conforme a implementação avança. Branch: `feat/ifood`.

## Objetivo

Centralizar as plataformas de delivery (iFood primeiro) para (1) facilitar a leitura de
métricas e o fechamento de caixa pelo gerente e (2) escalar o produto para múltiplas lojas.
Início com **duas lojas distintas**, ambas no iFood.

---

## Decisões

### 1. Escopo: Order primeiro, Financial depois
- **Order (agora):** captura o pedido em ~tempo real. Valor **bruto**; a taxa do iFood é
  deduzida por nós (via `CanalDeVenda.taxaPercentual`).
- **Financial (depois):** conciliação com o valor **líquido real** repassado pelo iFood.
  Entra como *prova real* para validar que os cálculos do Order batem. Fora de escopo agora.

### 2. Somente leitura
Não gerenciamos o ciclo de vida do pedido pelo ZeroPapel (não confirmamos/despachamos).
Apenas ingerimos para métricas e fechamento.

> ⚠️ **Risco a validar com o iFood:** confirmar que um consumidor read-only do módulo Order
> convive com a operação principal (tablet/Gestor de Pedidos) sem que o iFood espere que
> *nós* respondamos o ciclo de vida do pedido. Validar antes de aprofundar a ingestão.

### 3. Transporte: polling (não webhook)
- Um **único poller por app** (`GET /events:polling`, ~30s) — traz eventos das duas lojas juntos.
- **Acknowledgment obrigatório** de cada evento, mesmo sendo read-only. Sem ack, o iFood reentrega.
- Evento é magro (`orderId`, `code`, `merchantId`); o pedido completo vem de `GET /orders/{orderId}`.
- **Idempotência é estrutural**, não opcional (por causa da reentrega) — ver decisão 8.

### 4. Item do pedido: uma tabela, FK opcional ("Leitura A")
Existe **um único** `ItemPedido`:
- FK **opcional** para o `Item` de domínio — preenchida no PDV/balcão, **nula** no iFood.
- `nomeItem`, `precoUnitario`, `quantidade` **sempre** preenchidos (snapshot no momento do pedido).

Consequências:
- **Nada do cardápio do iFood é espelhado no banco por agora.** O item do iFood vive solto
  dentro do pedido (nome + preço + quantidade vindos do payload).
- O preço vem do próprio pedido (já com promoção/preço de horário aplicados) — mais fiel que catálogo.
- Mesma tabela, mesma query de fechamento para os dois mundos. Sem `UNION`, sem caminho duplicado.
- Guardar nome/preço mesmo no PDV protege o pedido antigo de futuras mudanças de preço do `Item`.

> Espelhar o Catalog do iFood (com FK do iFood) fica para uma fatia **futura e independente**,
> só se for necessário agregar métricas por produto ao longo do tempo. Antes de fazê-la, inspecionar
> um `GET /orders/{id}` real para confirmar se o identificador do item bate com o do Catalog.

### 5. Multi-loja: entidade `Loja`
Toda venda pertence a uma loja. `Pedido` (inclusive **balcão**) ganha FK para `Loja`.
Não existe venda órfã de loja — é o que torna o sistema de fato multi-loja.

### 6. `CanalDeVenda` — vínculo Loja × Plataforma
Modela a operação de uma loja num canal, com id externo e taxa próprios.

```
Loja ──< CanalDeVenda >── Plataforma
```

Campos:
- FK `Loja`, FK `Plataforma`
- `merchantId` — id da loja no iFood (nulo para Balcão)
- `taxaPercentual` — comissão **desta loja neste canal** (contratos diferem entre lojas)
- `ativo`
- **unique `(loja, plataforma)`** — uma loja tem um canal por plataforma
- **unique `merchantId`** — chave de **roteamento**: o evento do iFood traz o `merchantId`;
  achamos o `CanalDeVenda` por ele → descobrimos a loja.

### 7. Credenciais OAuth são do app, não do canal
As credenciais OAuth do iFood (client id/secret) são do **app** e leem os eventos das duas lojas.
- Credenciais → configuração/env (nível app).
- `merchantId` → no `CanalDeVenda` (nível loja).

Não guardar client secret por canal.

### 8. Identidade externa e status no `Pedido`
- `plataformaPedidoId` — o `orderId` do iFood (nulo para balcão).
- **unique `(plataforma, plataformaPedidoId)`** — mata a reentrega do polling (upsert, não insert).
- `status` — `PLACED → CONFIRMED → CONCLUDED / CANCELLED`.
- **`Pedido` deixa de ser imutável:** a ingestão atualiza o status conforme os eventos chegam.
- **Pedido `CANCELLED` não entra no caixa.**

### 9. Taxa da forma de pagamento não se aplica ao iFood
No balcão, a taxa da `FormaDePagamento` representa a **maquininha**. No iFood (pagamento online)
não há maquininha nossa — a comissão do iFood já embute isso, e ela é a `taxaPercentual` do canal.

Regra de fechamento (condicional por origem):
- **Pedido iFood** → deduz **apenas** a taxa do `CanalDeVenda`.
- **Pedido balcão** → deduz a taxa da `FormaDePagamento`.

Somar as duas num pedido iFood descontaria taxa a mais.

### 10. `Plataforma` perde `taxaPercentual`
A taxa migra para o `CanalDeVenda`. `Plataforma` vira apenas o **tipo de canal** (Balcão, iFood).
Requer migração dos dados já populados.

---

## Arquitetura de ingestão (camadas)

```
IfoodPollingJob (@Scheduled ~30s)
  └─ IfoodEventClient.poll() + acknowledge()
       └─ persiste evento CRU (inbox/staging)        ← desacopla "receber" de "processar"
            └─ IfoodOrderClient.buscarPedido(orderId)
                 └─ IfoodPedidoTranslator (Anti-Corruption Layer)   ← todo o JSON feio morre aqui
                      └─ monta AdicionarPedidoRequest
                           └─ PedidoService.adicionar(...)   ← reusa regra existente, não duplica
```

Princípios:
- **Anti-Corruption Layer:** uma classe traduz `IfoodOrder → AdicionarPedidoRequest`. Mudança de
  contrato do iFood se resolve em um só arquivo.
- **Inbox/staging cru:** salvar o payload bruto antes de traduzir permite replay/reprocesso e debug
  sem re-pollar. Separa "recebi" de "entendi".
- **Idempotência por identidade externa:** ingestão é *upsert* por `(plataforma, plataformaPedidoId)`.
- As classes de integração **dependem de `PedidoService`** e delegam — sem duplicar regra de negócio
  (alinhado ao CLAUDE.md).

---

## Pendências / a validar
- [ ] Confirmar no Portal do Desenvolvedor quais módulos estão liberados (assumido: Order disponível).
- [ ] Validar convivência do consumidor read-only de Order com a operação principal (decisão 2).
- [ ] Ao considerar o Catalog (fatia futura), inspecionar `GET /orders/{id}` real e verificar se o
      identificador do item bate com o do Catalog (decisão 4).

---

## Ordem de implementação sugerida
1. **Domínio multi-loja:** `Loja`, `CanalDeVenda`, remoção de `taxaPercentual` de `Plataforma` (+ migração).
2. **`Pedido`:** FK `Loja`, `plataformaPedidoId`, `status`, unique `(plataforma, plataformaPedidoId)`.
3. **Fechamento:** regra condicional de taxa (decisão 9) e exclusão de `CANCELLED`.
4. **Ingestão iFood:** auth OAuth (app-level) → polling + ack → inbox → busca pedido → translator → `PedidoService`.
5. **Financial (conciliação):** fatia posterior de prova real.
```
