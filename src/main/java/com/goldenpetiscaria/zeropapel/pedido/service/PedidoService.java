package com.goldenpetiscaria.zeropapel.pedido.service;

import com.goldenpetiscaria.zeropapel.pedido.dto.request.AdicionarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.PedidoResponseDTO;

import java.util.List;

public interface PedidoService {
    PedidoResponseDTO registrarPedido(AdicionarPedidoRequest request);
    List<PedidoResponseDTO> listarPedidos();
    PedidoResponseDTO buscarPedidoPorId(Long id);
    PedidoResponseDTO atualizarPedido(Long id, AtualizarPedidoRequest request);
    void excluirPedido(Long id);
}
