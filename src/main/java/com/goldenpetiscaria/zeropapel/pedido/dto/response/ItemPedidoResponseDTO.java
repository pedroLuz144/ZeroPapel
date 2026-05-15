package com.goldenpetiscaria.zeropapel.pedido.dto.response;

import java.math.BigDecimal;

public record ItemPedidoResponseDTO(
        Long itemId,
        String itemNome,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {}
