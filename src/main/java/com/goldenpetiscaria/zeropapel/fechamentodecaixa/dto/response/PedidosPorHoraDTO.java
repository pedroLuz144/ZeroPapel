package com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response;

import java.math.BigDecimal;

public record PedidosPorHoraDTO(
        Integer hora,
        Integer qtdPedidos,
        BigDecimal faturamentoBruto
) {}
