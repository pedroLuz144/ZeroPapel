package com.goldenpetiscaria.zeropapel.dto.response;

import java.math.BigDecimal;

public record ResumoFechamentoDTO(
        Integer totalPedidos,
        BigDecimal faturamentoBruto,
        BigDecimal totalTaxas,
        BigDecimal faturamentoLiquido,
        BigDecimal ticketMedio
) {}
