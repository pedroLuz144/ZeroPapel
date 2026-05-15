package com.goldenpetiscaria.zeropapel.formadepagamento.dto.response;

import java.math.BigDecimal;

public record FormaDePagamentoResponseDTO(
        Long id,
        String nome,
        BigDecimal taxaPercentual
) {}
