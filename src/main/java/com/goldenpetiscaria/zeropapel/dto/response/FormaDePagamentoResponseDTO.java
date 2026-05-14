package com.goldenpetiscaria.zeropapel.dto.response;

import java.math.BigDecimal;

public record FormaDePagamentoResponseDTO(
        Long id,
        String nome,
        BigDecimal taxaPercentual
) {}
