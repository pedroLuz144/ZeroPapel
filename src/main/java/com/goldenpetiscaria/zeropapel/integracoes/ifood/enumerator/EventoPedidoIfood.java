package com.goldenpetiscaria.zeropapel.integracoes.ifood.enumerator;

import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;

import java.util.Optional;

public enum EventoPedidoIfood {
    PLACED("PLC", StatusPedido.EM_ABERTO),
    CONFIRMED("CFM", StatusPedido.ACEITO),
    DISPATCHED("DSP", StatusPedido.EM_ROTA),
    CONCLUDED("CON", StatusPedido.CONCLUIDO),
    CANCELLED("CAN", StatusPedido.CANCELADO);

    private final String codigoCurto;
    private final StatusPedido statusPedido;

    EventoPedidoIfood(String codigoCurto, StatusPedido statusPedido) {
        this.codigoCurto = codigoCurto;
        this.statusPedido = statusPedido;
    }

    public static Optional<StatusPedido> paraStatusPedido(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        String normalizado = codigo.trim();
        for (EventoPedidoIfood evento : values()) {
            if (evento.codigoCurto.equalsIgnoreCase(normalizado) || evento.name().equalsIgnoreCase(normalizado)) {
                return Optional.of(evento.statusPedido);
            }
        }
        return Optional.empty();
    }
}
