package com.goldenpetiscaria.zeropapel.formadepagamento.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record AtualizarFormaDePagamentoRequest(
        String nome,

        @DecimalMin(value = "0.0", message = "A taxa não pode ser negativa")
        @DecimalMax(value = "100.0", message = "A taxa não pode passar de 100%")
        BigDecimal taxaPercentual
) {}
