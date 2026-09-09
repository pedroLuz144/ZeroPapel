package com.goldenpetiscaria.zeropapel.pedido.enumerator;

/**
 * Etapas do ciclo de vida de um pedido (kanban do Painel de Pedidos).
 *
 * Fluxo linear: EM_ABERTO → ACEITO → EM_PREPARO → PRONTO → EM_ROTA → CONCLUIDO.
 * Pedidos de balcão pulam EM_ROTA (vão de PRONTO direto para CONCLUIDO).
 * CANCELADO é um estado terminal alcançável de qualquer etapa.
 */
public enum StatusPedido {
    EM_ABERTO,
    ACEITO,
    EM_PREPARO,
    PRONTO,
    EM_ROTA,
    CONCLUIDO,
    CANCELADO
}
