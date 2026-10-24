package com.goldenpetiscaria.zeropapel.pedido.service;

import com.goldenpetiscaria.zeropapel.pedido.dto.request.AdicionarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.RegistrarPedidoExternoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.HistoricoPedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.PedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.entity.Pedido;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.ResultadoStatusExterno;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoService {
    PedidoResponseDTO registrarPedido(AdicionarPedidoRequest request);
    PedidoResponseDTO registrarPedidoExterno(RegistrarPedidoExternoRequest request);
    List<PedidoResponseDTO> listarPedidos(LocalDateTime de, LocalDateTime ate);
    PedidoResponseDTO buscarPedidoPorId(Long id);
    PedidoResponseDTO atualizarPedido(Long id, AtualizarPedidoRequest request, Long usuarioId);
    PedidoResponseDTO atualizarStatus(Long id, StatusPedido status, String motivo, Long usuarioId);
    void excluirPedido(Long id, Long usuarioId);
    List<HistoricoPedidoResponseDTO> listarHistorico(Long pedidoId);
    ResultadoStatusExterno aplicarStatusExterno(Long plataformaId, String plataformaPedidoId, StatusPedido status);
    List<Pedido> buscarFaturaveisDoPeriodo(LocalDateTime de, LocalDateTime ate);
    int marcarComoFechados(LocalDateTime de, LocalDateTime ate);
}
