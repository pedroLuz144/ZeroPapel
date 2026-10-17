package com.goldenpetiscaria.zeropapel.pedido.listener;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.formadepagamento.evento.ExclusaoDeFormaDePagamentoSolicitadaEvent;
import com.goldenpetiscaria.zeropapel.pedido.repository.PedidoRepository;
import com.goldenpetiscaria.zeropapel.plataforma.evento.ExclusaoDePlataformaSolicitadaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CadastroVinculadoAPedidoListener {

    private final PedidoRepository pedidoRepository;

    public CadastroVinculadoAPedidoListener(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @EventListener
    public void aoSolicitarExclusaoDePlataforma(ExclusaoDePlataformaSolicitadaEvent evento) {
        if (pedidoRepository.existsByPlataformaId(evento.plataformaId())) {
            throw new ConflitoException("Não é possível excluir a plataforma pois existem pedidos vinculados a ela");
        }
    }

    @EventListener
    public void aoSolicitarExclusaoDeFormaDePagamento(ExclusaoDeFormaDePagamentoSolicitadaEvent evento) {
        if (pedidoRepository.existsByFormaDePagamentoId(evento.formaDePagamentoId())) {
            throw new ConflitoException("Não é possível excluir a forma de pagamento pois existem pedidos vinculados a ela");
        }
    }
}
