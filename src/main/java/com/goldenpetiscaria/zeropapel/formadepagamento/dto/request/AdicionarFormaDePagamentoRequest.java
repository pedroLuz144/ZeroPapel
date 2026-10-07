package com.goldenpetiscaria.zeropapel.formadepagamento.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdicionarFormaDePagamentoRequest(
        @NotBlank(message = "O nome da forma de pagamento é obrigatório")
        String nome,

        @NotNull(message = "A taxa percentual é obrigatória")
        @DecimalMin(value = "0.0", message = "A taxa não pode ser negativa")
        @DecimalMax(value = "100.0", message = "A taxa não pode passar de 100%")
        BigDecimal taxaPercentual
) {}
