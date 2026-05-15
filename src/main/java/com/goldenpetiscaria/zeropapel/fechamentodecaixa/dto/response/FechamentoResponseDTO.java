package com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FechamentoResponseDTO(
        Long id,
        LocalDateTime de,
        LocalDateTime ate,
        ResumoFechamentoDTO resumo,
        List<FechamentoPorPlataformaDTO> porPlataforma,
        List<FechamentoPorFormaDePagamentoDTO> porFormaDePagamento,
        List<ItemRankingDTO> itensMaisVendidos,
        List<PedidosPorHoraDTO> pedidosPorHora
) {}
