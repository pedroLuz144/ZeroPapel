package com.goldenpetiscaria.zeropapel.plataforma.dto.request;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record AtualizarPlataformaRequest(
        String nome,

        @DecimalMin(value = "0.0", message = "A taxa não pode ser negativa")
        BigDecimal taxaPercentual
) {}
