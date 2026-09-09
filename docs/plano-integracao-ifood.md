# Plano de Integração iFood

## Contexto

A integração com o iFood deve seguir a arquitetura definida no CLAUDE.md: as classes de integração traduzem
o formato externo para `AdicionarPedidoRequest` e delegam para `PedidoService`, sem duplicar lógica de negócio.

O objetivo é capturar os pedidos recebidos no iFood automaticamente e registrá-los no sistema.
A loja está configurada com confirmação automática no iFood, portanto não é necessário chamar o endpoint
de confirmação — apenas o acknowledgment dos eventos é obrigatório.

---

## Pré-requisitos

- Conta de desenvolvedor ativa no [Portal iFood](https://developer.ifood.com.br)
- Credenciais de produção: `IFOOD_CLIENT_ID` e `IFOOD_CLIENT_SECRET`
- `Plataforma` "iFood" cadastrada no banco (com `taxaPercentual` correto)
- `FormaDePagamento` para cada método aceito pelo iFood cadastrada no banco

---

## Mudanças no Domínio Existente

Antes de implementar a integração, duas entidades precisam de novos campos.

### 1. Entidade `Item` — campo `idIfood`

O iFood envia cada item com um ID próprio. Sem um mapeamento, é impossível saber qual
item local corresponde ao item do pedido externo.

```java
// item/entity/Item.java
@Column(name = "id_ifood", unique = true)
private String idIfood; // UUID do item no catálogo iFood
```

O gerente cadastra esse ID ao registrar o item no sistema (ou via endpoint de atualização).
Sem o `idIfood` mapeado, o item do pedido é descartado com log de aviso.

### 2. Entidade `Pedido` — campos `idExterno` e `statusExterno`

Necessário para confirmar e rastrear pedidos iFood após o registro.

```java
// pedido/entity/Pedido.java
@Column(name = "id_externo", unique = true)
private String idExterno; // ID do pedido no iFood

@Column(name = "status_externo")
private String statusExterno; // ex: PLACED, CONFIRMED, CANCELLED
```

---

## Estrutura do Pacote `integracao/ifood/`

```
integracao/
  ifood/
    auth/
      IfoodAuthService.java          → obtém e renova o bearer token
      IfoodTokenResponse.java        → DTO da resposta do endpoint de token
    client/
      IfoodApiClient.java            → chamadas HTTP para a API iFood (polling, detalhes, acknowledgment)
    dto/
      IfoodOrderEvent.java           → evento recebido no polling
      IfoodOrderDetails.java         → detalhes completos do pedido
      IfoodOrderItem.java            → item dentro do pedido iFood
      IfoodAcknowledgmentRequest.java
    adapter/
      IfoodPedidoAdapter.java        → traduz IfoodOrderDetails → AdicionarPedidoRequest
    polling/
      IfoodPollingService.java       → agendador que chama o polling a cada 30s
```

---

## Etapas de Implementação

### Etapa 1 — Autenticação OAuth 2.0

**Endpoint:** `POST https://merchant-api.ifood.com.br/authentication/v1.0/oauth/token`

`IfoodAuthService` gerencia o ciclo de vida do token:
- Obtém novo token com `client_credentials`
- Armazena em memória com o timestamp de expiração
- Renova automaticamente antes de expirar (com margem de 60s)
- Todos os outros componentes da integração dependem deste serviço

Variáveis de ambiente a adicionar no `.env`:
```
IFOOD_CLIENT_ID=...
IFOOD_CLIENT_SECRET=...
```

### Etapa 2 — Cliente HTTP (`IfoodApiClient`)

Encapsula todas as chamadas à API iFood usando `RestClient` (Spring 6):

| Método | Endpoint | Uso |
|---|---|---|
| `GET` | `/order/v1.0/orders:polling` | Buscar novos eventos |
| `GET` | `/order/v1.0/orders/{id}` | Detalhes do pedido |
| `POST` | `/order/v1.0/orders:acknowledgment` | Confirmar recebimento dos eventos |

Todos os métodos injetam automaticamente o header `Authorization: Bearer {token}` via `IfoodAuthService`.

### Etapa 3 — Adapter (`IfoodPedidoAdapter`)

Responsável pela tradução do formato iFood para o formato interno. É a peça central da integração.

**Lógica de mapeamento:**

```
IfoodOrderDetails
  ├── id                        → Pedido.idExterno
  ├── customer.name             → AdicionarPedidoRequest.nomeCliente
  ├── payments[0].method        → busca FormaDePagamento por nome no banco
  ├── items[].externalCode      → busca Item por idIfood no banco
  │     └── se não encontrar   → loga aviso, pula o item
  └── (plataforma fixa)         → busca Plataforma "iFood" no banco
```

**Regra de falha:** se nenhum item for mapeado com sucesso (todos sem `idIfood`), o adapter
lança exceção e o pedido não é registrado. O evento é igualmente confirmado (acknowledged)
para não ficar na fila, mas o erro é logado para investigação.

### Etapa 4 — Polling Agendado (`IfoodPollingService`)

`@Scheduled(fixedDelay = 30000)` executa o ciclo completo:

```
1. GET polling → lista de eventos
2. Para cada evento do tipo ORDER_PLACED:
   a. GET /orders/{id} com retry exponencial (até 10 min) para 404
   b. adapter.traduzir(detalhes) → AdicionarPedidoRequest
   c. pedidoService.registrarPedido(request)
3. POST acknowledgment com todos os IDs de eventos processados
```

O acknowledgment é enviado **somente após** o processamento com sucesso para garantir
que falhas causem reentrega do evento.

### Etapa 5 — Tratamento de Outros Eventos

Além de `ORDER_PLACED`, o iFood envia eventos de mudança de status. Por ora, apenas
atualizar `Pedido.statusExterno` no banco sem lógica adicional:

| Evento iFood | Ação |
|---|---|
| `ORDER_PLACED` | Registrar pedido |
| `ORDER_CONFIRMED` | Atualizar `statusExterno` |
| `ORDER_DISPATCHED` | Atualizar `statusExterno` |
| `ORDER_CONCLUDED` | Atualizar `statusExterno` |
| `ORDER_CANCELLED` | Atualizar `statusExterno` (não excluir) |

---

## Dependências Novas (pedir aprovação antes de adicionar)

Nenhuma nova dependência é estritamente necessária — `RestClient` já está disponível no
Spring Boot 3+. Se for usar `@Scheduled`, verificar se `@EnableScheduling` já está na
aplicação.

---

## Ordem de Execução Sugerida

1. Adicionar campos `idIfood` em `Item` e `idExterno`/`statusExterno` em `Pedido`
2. Implementar `IfoodAuthService` + teste manual do token
3. Implementar `IfoodApiClient` com os 3 endpoints
4. Implementar `IfoodPedidoAdapter` com testes unitários de mapeamento
5. Implementar `IfoodPollingService` com `@Scheduled`
6. Tratar eventos de status (atualização de `statusExterno`)
7. Testar end-to-end em ambiente sandbox iFood
