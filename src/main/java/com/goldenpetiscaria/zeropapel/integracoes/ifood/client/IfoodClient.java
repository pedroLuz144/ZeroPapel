package com.goldenpetiscaria.zeropapel.integracoes.ifood.client;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.request.ConfirmarEventoRequest;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.ConfirmarEventoResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.DetalhesPedidoResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.EventoPedidoResponse;

public class IfoodClient {

    EventoPedidoResponse buscarEventoPedido() {
        return new EventoPedidoResponse();
    }

    ConfirmarEventoResponse confirmarEvento(ConfirmarEventoRequest request) {
        return new ConfirmarEventoResponse();
    }

    DetalhesPedidoResponse detalharPedido() {
        return new DetalhesPedidoResponse();
    }
}
