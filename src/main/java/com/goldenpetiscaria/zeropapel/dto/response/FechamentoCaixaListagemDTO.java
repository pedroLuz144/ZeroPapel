package com.goldenpetiscaria.zeropapel.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FechamentoCaixaListagemDTO(
        Long id,
        LocalDateTime de,
        LocalDateTime ate,
        LocalDateTime geradoEm,
        String geradoPorNome,
        Integer totalPedidos,
        BigDecimal faturamentoBruto,
        BigDecimal totalTaxas,
        BigDecimal faturamentoLiquido,
        BigDecimal ticketMedio
) {}
