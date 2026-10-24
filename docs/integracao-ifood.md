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

Consequência no painel (decidido em 08/10/2026): **pedido externo não é operado pelo kanban**.
`atualizarPedido`, `atualizarStatus` e `excluirPedido` recusam pedido com `plataformaPedidoId`
(409), e a SPA esconde Editar, Avançar, Mudar etapa e Excluir. O status só muda pelos eventos
da plataforma, via `PedidoService.aplicarStatusExterno`.

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
- `status` — o `StatusPedido` que já existe, alimentado pelos eventos do iFood. Mapeamento
  decidido em 08/10/2026 (`integracoes/ifood/enumerator/EventoPedidoIfood`):

  | Evento iFood (`fullCode` / `code`) | `StatusPedido` |
  |---|---|
  | `PLACED` / `PLC` | `EM_ABERTO` (é o registro do pedido) |
  | `CONFIRMED` / `CFM` | `ACEITO` |
  | `DISPATCHED` / `DSP` | `EM_ROTA` |
  | `CONCLUDED` / `CON` | `CONCLUIDO` |
  | `CANCELLED` / `CAN` | `CANCELADO` |

  Qualquer outro evento fica sem equivalente e é ignorado pelo mapeamento. Em aberto, para
  decidir: `PREPARATION_STARTED` (candidato a `EM_PREPARO`), `READY_TO_PICKUP` (candidato a
  `PRONTO`), `SEPARATION_STARTED` / `SEPARATION_ENDED` (mercado) e os eventos que não são de
  ciclo de vida (`CANCELLATION_REQUEST_FAILED`, `DELIVERY_GROUP_ASSIGNED`,
  `RECOMENDED_START_PREPARATION`).
- **O status do `Pedido` muda pela ingestão**, por `PedidoService.aplicarStatusExterno`,
  identificado por `(plataforma, plataformaPedidoId)`. Eventos chegam repetidos e fora de ordem:
  só é aplicado status à frente do atual no ciclo de vida, ou `CANCELADO`; `CONCLUIDO` e
  `CANCELADO` são terminais. Evento de pedido que ainda não foi registrado devolve
  "não encontrado" sem lançar.
- **Evento tardio em pedido já fechado** (decidido em 08/10/2026): não altera o pedido e não
  lança; só gera um WARN no log com plataforma, id externo, status atual e status recebido. O
  histórico fechado continua imutável, inclusive para um `CANCELLED` que chegue depois do
  fechamento.
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

### 11. Taxa de entrega: regra no canal, congelada no pedido
Decidido em 15/10/2026. A loja que vai usar o sistema tem entrega própria e não cobra entrega do
cliente; quando um pedido do iFood traz `total.deliveryFee`, o mais provável é que a entrega tenha
sido feita pelo iFood e o valor seja descontado da loja. Mas o pedido de teste do Developer Portal
veio com `delivery.deliveredBy = MERCHANT` **e** R$ 5 de entrega, então o campo `deliveredBy` é
quem decide, não a presença da taxa.

- `CanalDeVenda.tratamentoTaxaEntrega` (`DESCONTAR` | `SOMAR` | `IGNORAR`, padrão `DESCONTAR`,
  editável em Configurações) vale quando **a plataforma** entregou.
- Quando **a loja** entregou (`deliveredBy = MERCHANT`), a taxa paga pelo cliente é da loja:
  sempre `SOMAR`. Sem `delivery` ou com `deliveredBy` desconhecido, conta como entrega da
  plataforma (a regra do canal decide).
- O pedido guarda `taxaEntrega` e o tratamento **resolvido na venda** (V12), como as outras taxas:
  trocar a regra do canal depois não reescreve o histórico. Sem taxa, fica `IGNORAR` com zero.
- A decisão mora em `PedidoServiceImpl.registrarPedidoExterno`; o translator só repassa
  `total.deliveryFee` e se a entrega foi da plataforma.
- Fechamento: `SOMAR` entra no bruto e na base da taxa de pagamento (o cliente pagou a entrega
  pelo mesmo meio), mas **não** na base da comissão do canal; `DESCONTAR` entra em `totalTaxas` e,
  no agrupamento por plataforma, em `taxaPlataforma`. `Pedido.valor` continua sendo só os itens,
  e `fechamentos_caixa` não ganhou coluna.

---

## Arquitetura de ingestão (camadas)

```
IfoodPollingJob (@Scheduled ~30s)
  └─ IfoodEventClient.poll() + acknowledge()
       └─ persiste evento CRU (inbox/staging)        ← desacopla "receber" de "processar"
            └─ IfoodOrderClient.buscarPedido(orderId)
                 └─ IfoodPedidoTranslator (Anti-Corruption Layer)   ← todo o JSON feio morre aqui
                      └─ monta RegistrarPedidoExternoRequest
                           └─ PedidoService.registrarPedidoExterno(...)   ← reusa regra existente, não duplica
```

Princípios:
- **Anti-Corruption Layer:** uma classe traduz `IfoodOrder → RegistrarPedidoExternoRequest`. Mudança de
  contrato do iFood se resolve em um só arquivo. Não é o `AdicionarPedidoRequest` do `POST /pedidos`:
  abrir preço e nome livres nele deixaria o operador registrar venda de balcão com preço arbitrário.
- **Inbox/staging cru:** salvar o payload bruto antes de traduzir permite replay/reprocesso e debug
  sem re-pollar. Separa "recebi" de "entendi".
- **Idempotência por identidade externa:** ingestão é *upsert* por `(plataforma, plataformaPedidoId)`.
- As classes de integração **dependem de `PedidoService`** e delegam — sem duplicar regra de negócio
  (alinhado ao CLAUDE.md).

---

## Pendências / a validar
- [x] Conferir contra a API real os caminhos `events/v1.0/events:polling`,
      `events/v1.0/events/acknowledgment` e `order/v1.0/orders/{id}`, e os campos lidos do pedido
      (`createdAt`, `customer.name`, `items[].name|quantity|unitPrice|optionsPrice|totalPrice`).
      Conferido em 15/10/2026 com um pedido de teste do Developer Portal; ver etapa 4.
- [x] Forma de pagamento do pedido iFood: **uma só**, fixa em `IFOOD_FORMA_DE_PAGAMENTO_ID` (forma
      "iFood online" com taxa 0, o que já entrega a decisão 9 sem mexer no fechamento). Decidido em
      15/10/2026: `payments.methods` não é lido, porque um pedido pode vir pago com mais de um meio
      (o pedido de teste trouxe dois cartões, R$ 17 + R$ 10) e o `Pedido` tem uma forma só.
- [x] Linha cujo total não divide exato pela quantidade (ou quantidade fracionada) entra com
      quantidade 1 e o total como preço. Decidido em 15/10/2026: o dinheiro tem prioridade sobre
      a quantidade do ranking.
- [x] Taxa de entrega (`total.deliveryFee`): decisão 11.
- [ ] Descontos (`total.benefits`) e taxa de serviço do iFood (`additionalFees`, paga pelo cliente
      ao iFood) continuam fora do valor. Confirmar com o extrato do Financial.
- [ ] Evento que vira `FALHOU` (canal não cadastrado, por exemplo) só é reprocessado mexendo no
      banco. Falta uma tela ou endpoint de reprocesso, junto da aba Integrações.
- [ ] Confirmar no Portal do Desenvolvedor quais módulos estão liberados (assumido: Order disponível).
- [ ] Validar convivência do consumidor read-only de Order com a operação principal (decisão 2).
- [ ] Ao considerar o Catalog (fatia futura), inspecionar `GET /orders/{id}` real e verificar se o
      identificador do item bate com o do Catalog (decisão 4).

---

## Ordem de implementação sugerida
1. ✅ **Domínio multi-loja (concluída em 07/10/2026):** `Loja`, `CanalDeVenda`, remoção de
   `taxaPercentual` de `Plataforma` (+ migrações V5 a V8). A taxa congelada no pedido passa a vir
   do canal da loja (`CanalDeVendaService.buscarCanalAtivo`).
2. **`Pedido`:** FK `Loja`, `plataformaPedidoId`, `status`, unique `(plataforma, plataformaPedidoId)`.
   - ✅ Já entrou junto com a etapa 1: FK `Loja` (`pedidos.loja_id` not null, V7), `lojaId` no
     `AdicionarPedidoRequest` e seletor de loja no PDV.
   - ✅ Concluído em 07/10/2026: `plataformaPedidoId` + unique `(plataforma, plataformaPedidoId)`
     (V9); `ItemPedido` com FK de item opcional e `nomeItem` sempre gravado, inclusive no PDV
     (decisão 4, V10); `PedidoService.registrarPedidoExterno(RegistrarPedidoExternoRequest)`,
     idempotente pela identidade externa e sem controller; pedido externo recusa edição de
     conteúdo (decisão 2); ranking de itens agrupa item externo pelo nome gravado.
   - ✅ Concluído em 08/10/2026: mapeamento do status do iFood para `StatusPedido`
     (`EventoPedidoIfood`), `PedidoService.aplicarStatusExterno` e recusa de status e exclusão
     de pedido externo pelo painel (decisões 2 e 8). A reentrega do `PLACED` continua sem
     alterar o status.
3. **Fechamento:** regra condicional de taxa (decisão 9) e filtro por loja — hoje fechamento e
   dashboard consolidam todas as lojas juntas. A exclusão de cancelados já existe (`CANCELADO`).
4. **Ingestão iFood:** auth OAuth (app-level) → polling + ack → inbox → busca pedido → translator → `PedidoService`.
   - ✅ Pronto em 08/10/2026: auth OAuth do aplicativo Centralizado (`IfoodAutenticador`, token em
     memória com renovação antecipada; `IFOOD_CLIENT_ID` / `IFOOD_CLIENT_SECRET`), listagem de
     lojas (`IfoodLojaClient`, para descobrir o Merchant ID) e
     `CanalDeVendaService.buscarCanalAtivoPorMerchantId` para rotear o evento. Conferido contra a
     API real: token com `expiresIn` de 21599 s (6 h) e a loja de teste na listagem.
   - ✅ Escrito em 10/10/2026, coberto por teste unitário e **ainda não rodado contra o iFood
     real**: polling + acknowledgment (`IfoodEventoClient`, `IfoodPollingJob` com `@Scheduled`,
     desligado por padrão em `IFOOD_POLLING_ATIVO`), inbox do evento cru (`ifood_evento`, V11),
     busca do pedido (`IfoodPedidoClient`), `IfoodPedidoTranslator` e `IfoodIngestaoService`,
     que chama `registrarPedidoExterno` / `aplicarStatusExterno`. Status que chega antes do
     pedido faz o pedido ser buscado e registrado na hora.
   - ✅ Validado em 15/10/2026: V11 aplicada e validada pelo Hibernate no MariaDB; contra a loja
     de teste, o polling recebeu `PLACED` e `CANCELLED`, o pedido foi registrado (2 itens, R$ 21,
     opcionais embutidos) e cancelado, e o polling seguinte respondeu 204, confirmando o
     acknowledgment. Taxa de entrega tratada (decisão 11, V12).
   - Falta: decidir as pendências acima e confirmar a convivência com o Gestor de Pedidos antes
     de ligar o polling numa loja real.
5. **Financial (conciliação):** fatia posterior de prova real.
```
