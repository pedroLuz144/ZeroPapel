package com.goldenpetiscaria.zeropapel.plataforma.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdicionarPlataformaRequest(
        @NotBlank(message = "O nome da plataforma é obrigatório")
        String nome,

        @NotNull(message = "A taxa percentual é obrigatória")
        @DecimalMin(value = "0.0", message = "A taxa não pode ser negativa")
        BigDecimal taxaPercentual
) {}
