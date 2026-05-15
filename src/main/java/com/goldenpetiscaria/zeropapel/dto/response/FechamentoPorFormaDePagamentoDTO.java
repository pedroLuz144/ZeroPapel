package com.goldenpetiscaria.zeropapel.dto.response;

import java.math.BigDecimal;

public record FechamentoPorFormaDePagamentoDTO(
        String formaDePagamento,
        Integer qtdPedidos,
        BigDecimal bruto,
        BigDecimal taxa,
        BigDecimal liquido
) {}
