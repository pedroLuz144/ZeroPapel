package com.goldenpetiscaria.zeropapel.pedido.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdicionarPedidoRequest(
        @NotNull(message = "A plataforma é obrigatória")
        Long plataformaId,

        String nomeCliente,

        @NotNull(message = "A forma de pagamento é obrigatória")
        Long formaDePagamentoId,

        @NotNull
        @NotEmpty(message = "O pedido precisa ter pelo menos um item")
        @Valid
        List<ItemPedidoRequest> itens
) {}
