package com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response;

import java.math.BigDecimal;

public record ResumoFechamentoDTO(
        Integer totalPedidos,
        BigDecimal faturamentoBruto,
        BigDecimal totalTaxas,
        BigDecimal faturamentoLiquido,
        BigDecimal ticketMedio
) {}
