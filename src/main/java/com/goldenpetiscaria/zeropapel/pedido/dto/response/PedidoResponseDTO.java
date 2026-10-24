package com.goldenpetiscaria.zeropapel.pedido.dto.response;

import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponseDTO(
        Long id,
        Long lojaId,
        String lojaNome,
        Long plataformaId,
        String plataformaNome,
        String plataformaPedidoId,
        String nomeCliente,
        LocalDateTime horarioPedido,
        Long formaDePagamentoId,
        String formaDePagamentoNome,
        StatusPedido status,
        BigDecimal valor,
        boolean fechado,
        String motivoCancelamento,
        List<ItemPedidoResponseDTO> itens
) {}
