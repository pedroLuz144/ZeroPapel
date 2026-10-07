package com.goldenpetiscaria.zeropapel.pedido.service;

import com.goldenpetiscaria.zeropapel.pedido.dto.request.AdicionarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.PedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoService {
    PedidoResponseDTO registrarPedido(AdicionarPedidoRequest request);
    List<PedidoResponseDTO> listarPedidos(LocalDateTime de, LocalDateTime ate);
    PedidoResponseDTO buscarPedidoPorId(Long id);
    PedidoResponseDTO atualizarPedido(Long id, AtualizarPedidoRequest request);
    PedidoResponseDTO atualizarStatus(Long id, StatusPedido status);
    void excluirPedido(Long id);
}
