package com.goldenpetiscaria.zeropapel.pedido.listener;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.formadepagamento.evento.ExclusaoDeFormaDePagamentoSolicitadaEvent;
import com.goldenpetiscaria.zeropapel.item.evento.ExclusaoDeItemSolicitadaEvent;
import com.goldenpetiscaria.zeropapel.pedido.repository.PedidoRepository;
import com.goldenpetiscaria.zeropapel.plataforma.evento.ExclusaoDePlataformaSolicitadaEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadastroVinculadoAPedidoListenerTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private CadastroVinculadoAPedidoListener listener;

    @Test
    void recusaExcluirPlataforma_quandoHaPedidoVinculado() {
        when(pedidoRepository.existsByPlataformaId(2L)).thenReturn(true);

        assertThatThrownBy(() -> listener.aoSolicitarExclusaoDePlataforma(new ExclusaoDePlataformaSolicitadaEvent(2L)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("pedidos vinculados");
    }

    @Test
    void deixaExcluirPlataforma_semPedidoVinculado() {
        when(pedidoRepository.existsByPlataformaId(2L)).thenReturn(false);

        assertThatCode(() -> listener.aoSolicitarExclusaoDePlataforma(new ExclusaoDePlataformaSolicitadaEvent(2L)))
                .doesNotThrowAnyException();
    }

    @Test
    void recusaExcluirFormaDePagamento_quandoHaPedidoVinculado() {
        when(pedidoRepository.existsByFormaDePagamentoId(3L)).thenReturn(true);

        assertThatThrownBy(() -> listener.aoSolicitarExclusaoDeFormaDePagamento(
                new ExclusaoDeFormaDePagamentoSolicitadaEvent(3L)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("pedidos vinculados");
    }

    @Test
    void deixaExcluirFormaDePagamento_semPedidoVinculado() {
        when(pedidoRepository.existsByFormaDePagamentoId(3L)).thenReturn(false);

        assertThatCode(() -> listener.aoSolicitarExclusaoDeFormaDePagamento(
                new ExclusaoDeFormaDePagamentoSolicitadaEvent(3L)))
                .doesNotThrowAnyException();
    }

    @Test
    void recusaExcluirItem_quandoHaPedidoVinculado_sugerindoDesativar() {
        when(pedidoRepository.existsByItensItemId(7L)).thenReturn(true);

        assertThatThrownBy(() -> listener.aoSolicitarExclusaoDeItem(new ExclusaoDeItemSolicitadaEvent(7L)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("pedidos vinculados")
                .hasMessageContaining("Desative");
    }

    @Test
    void deixaExcluirItem_semPedidoVinculado() {
        when(pedidoRepository.existsByItensItemId(7L)).thenReturn(false);

        assertThatCode(() -> listener.aoSolicitarExclusaoDeItem(new ExclusaoDeItemSolicitadaEvent(7L)))
                .doesNotThrowAnyException();
    }
}
