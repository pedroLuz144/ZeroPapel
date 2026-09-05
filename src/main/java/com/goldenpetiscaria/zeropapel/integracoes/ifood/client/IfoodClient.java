package com.goldenpetiscaria.zeropapel.integracoes.ifood.client;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.request.ConfirmarEventoRequest;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.ConfirmarEventoResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.DetalhesPedidoResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.EventoPedidoResponse;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AdicionarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.PedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;
import com.goldenpetiscaria.zeropapel.pedido.service.PedidoService;

import java.util.List;

public class IfoodClient implements PedidoService {

    @Override
    public PedidoResponseDTO registrarPedido(AdicionarPedidoRequest request) {
        return null;
    }

    @Override
    public List<PedidoResponseDTO> listarPedidos() {
        return List.of();
    }

    @Override
    public PedidoResponseDTO buscarPedidoPorId(Long id) {
        return null;
    }

    @Override
    public PedidoResponseDTO atualizarPedido(Long id, AtualizarPedidoRequest request) {
        return null;
    }

    @Override
    public PedidoResponseDTO atualizarStatus(Long id, StatusPedido status) {
        return null;
    }

    @Override
    public void excluirPedido(Long id) {

    }

    private EventoPedidoResponse buscarEventoPedido() {
        EventoPedidoResponse eventoPedidoResponse = new EventoPedidoResponse();
        return eventoPedidoResponse;
    }

    private ConfirmarEventoResponse confirmarEvento(ConfirmarEventoRequest request) {
        ConfirmarEventoResponse confirmarEventoResponse = new ConfirmarEventoResponse();
        return confirmarEventoResponse;
    }

    private DetalhesPedidoResponse detalharPedido() {
        DetalhesPedidoResponse detalhesPedidoResponse = new DetalhesPedidoResponse();
        return detalhesPedidoResponse;
    }
}
