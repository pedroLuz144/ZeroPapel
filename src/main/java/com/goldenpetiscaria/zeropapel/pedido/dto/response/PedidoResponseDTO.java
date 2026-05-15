package com.goldenpetiscaria.zeropapel.pedido.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponseDTO(
        Long id,
        Long plataformaId,
        String plataformaNome,
        String nomeCliente,
        LocalDateTime horarioPedido,
        Long formaDePagamentoId,
        String formaDePagamentoNome,
        BigDecimal valor,
        List<ItemPedidoResponseDTO> itens
) {}
