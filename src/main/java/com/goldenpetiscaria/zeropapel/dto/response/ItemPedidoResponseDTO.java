package com.goldenpetiscaria.zeropapel.dto.response;

import java.math.BigDecimal;

public record ItemPedidoResponseDTO(
        Long itemId,
        String itemNome,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {}
