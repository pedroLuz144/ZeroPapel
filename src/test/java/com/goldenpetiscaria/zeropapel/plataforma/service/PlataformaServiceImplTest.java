package com.goldenpetiscaria.zeropapel.plataforma.service;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.pedido.repository.PedidoRepository;
import com.goldenpetiscaria.zeropapel.plataforma.dto.request.AtualizarPlataformaRequest;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import com.goldenpetiscaria.zeropapel.plataforma.repository.PlataformaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlataformaServiceImplTest {

    @Mock
    private PlataformaRepository plataformaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private PlataformaServiceImpl service;

    @Test
    void excluirPlataforma_lancaConflito_quandoEAUnicaPlataformaDeBalcao() {
        when(plataformaRepository.findById(1L)).thenReturn(Optional.of(plataforma(1L, "Balcão", false)));
        when(plataformaRepository.countByEntregaFalse()).thenReturn(1L);

        assertThatThrownBy(() -> service.excluirPlataforma(1L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("balcão");

        verify(plataformaRepository, never()).delete(any());
    }

    @Test
    void excluirPlataforma_excluiOBalcao_quandoHaOutraPlataformaDeBalcao() {
        Plataforma balcao = plataforma(1L, "Balcão", false);
        when(plataformaRepository.findById(1L)).thenReturn(Optional.of(balcao));
        when(plataformaRepository.countByEntregaFalse()).thenReturn(2L);
        when(pedidoRepository.existsByPlataformaId(1L)).thenReturn(false);

        service.excluirPlataforma(1L);

        verify(plataformaRepository).delete(balcao);
    }

    @Test
    void excluirPlataforma_naoConsultaOBalcao_quandoAPlataformaEDeEntrega() {
        Plataforma ifood = plataforma(2L, "iFood", true);
        when(plataformaRepository.findById(2L)).thenReturn(Optional.of(ifood));
        when(pedidoRepository.existsByPlataformaId(2L)).thenReturn(false);

        service.excluirPlataforma(2L);

        verify(plataformaRepository, never()).countByEntregaFalse();
        verify(plataformaRepository).delete(ifood);
    }

    @Test
    void atualizarPlataforma_lancaConflito_quandoMarcaComoEntregaAUnicaPlataformaDeBalcao() {
        Plataforma balcao = plataforma(1L, "Balcão", false);
        when(plataformaRepository.findById(1L)).thenReturn(Optional.of(balcao));
        when(plataformaRepository.countByEntregaFalse()).thenReturn(1L);

        assertThatThrownBy(() -> service.atualizarPlataforma(1L, new AtualizarPlataformaRequest(null, true)))
                .isInstanceOf(ConflitoException.class);

        assertThat(balcao.isEntrega()).isFalse();
        verify(plataformaRepository, never()).save(any());
    }

    @Test
    void buscarPlataformaDeBalcao_devolveAPrimeiraPlataformaSemEntrega() {
        Plataforma balcao = plataforma(1L, "Balcão", false);
        when(plataformaRepository.findFirstByEntregaFalseOrderByIdAsc()).thenReturn(Optional.of(balcao));

        assertThat(service.buscarPlataformaDeBalcao()).containsSame(balcao);
    }

    private Plataforma plataforma(Long id, String nome, boolean entrega) {
        Plataforma plataforma = new Plataforma();
        plataforma.setId(id);
        plataforma.setNome(nome);
        plataforma.setEntrega(entrega);
        return plataforma;
    }
}
