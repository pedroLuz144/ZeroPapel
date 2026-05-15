package com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response;

import java.math.BigDecimal;

public record ItemRankingDTO(
        Long itemId,
        String itemNome,
        Integer quantidadeTotal,
        BigDecimal receitaTotal
) {}
