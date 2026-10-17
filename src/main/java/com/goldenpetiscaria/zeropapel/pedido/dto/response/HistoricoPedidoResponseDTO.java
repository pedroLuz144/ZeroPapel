package com.goldenpetiscaria.zeropapel.pedido.dto.response;

import com.goldenpetiscaria.zeropapel.pedido.enumerator.AcaoHistoricoPedido;

import java.time.LocalDateTime;

public record HistoricoPedidoResponseDTO(
        Long id,
        AcaoHistoricoPedido acao,
        LocalDateTime dataHora,
        String usuarioNome,
        String usuarioLogin,
        String detalhe
) {}
