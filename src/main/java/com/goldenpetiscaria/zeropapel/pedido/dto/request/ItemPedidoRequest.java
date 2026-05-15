package com.goldenpetiscaria.zeropapel.pedido.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequest(
        @NotNull(message = "O item é obrigatório")
        Long itemId,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser pelo menos 1")
        Integer quantidade
) {}
