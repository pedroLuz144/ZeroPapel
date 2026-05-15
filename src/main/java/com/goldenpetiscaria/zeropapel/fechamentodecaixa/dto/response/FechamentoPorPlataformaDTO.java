package com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response;

import java.math.BigDecimal;

public record FechamentoPorPlataformaDTO(
        String plataforma,
        Integer qtdPedidos,
        BigDecimal bruto,
        BigDecimal taxaPlataforma,
        BigDecimal taxaPagamento,
        BigDecimal liquido
) {}
