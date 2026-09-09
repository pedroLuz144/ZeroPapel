package com.goldenpetiscaria.zeropapel.pedido.dto.request;

import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusPedidoRequest(
        @NotNull(message = "O status é obrigatório")
        StatusPedido status
) {}
