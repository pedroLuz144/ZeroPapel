package com.goldenpetiscaria.zeropapel.dto.response;

import java.math.BigDecimal;

public record PlataformaResponseDTO(
        Long id,
        String nome,
        BigDecimal taxaPercentual
) {}
