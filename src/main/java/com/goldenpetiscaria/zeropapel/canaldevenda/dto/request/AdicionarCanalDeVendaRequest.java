package com.goldenpetiscaria.zeropapel.canaldevenda.dto.request;

import com.goldenpetiscaria.zeropapel.canaldevenda.enumerator.TratamentoTaxaEntrega;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdicionarCanalDeVendaRequest(
        @NotNull(message = "A loja é obrigatória")
        Long lojaId,

        @NotNull(message = "A plataforma é obrigatória")
        Long plataformaId,

        @NotNull(message = "A taxa percentual é obrigatória")
        @DecimalMin(value = "0.0", message = "A taxa não pode ser negativa")
        @DecimalMax(value = "100.0", message = "A taxa não pode passar de 100%")
        BigDecimal taxaPercentual,

        String merchantId,

        TratamentoTaxaEntrega tratamentoTaxaEntrega
) {}
