package com.goldenpetiscaria.zeropapel.pedido.dto.request;

import jakarta.validation.Valid;

import java.util.List;

public record AtualizarPedidoRequest(
        Long plataformaId,
        String nomeCliente,
        Long formaDePagamentoId,

        @Valid
        List<ItemPedidoRequest> itens
) {}
