package com.goldenpetiscaria.zeropapel.plataforma.dto.response;

import java.math.BigDecimal;

public record PlataformaResponseDTO(
        Long id,
        String nome,
        BigDecimal taxaPercentual
) {}
