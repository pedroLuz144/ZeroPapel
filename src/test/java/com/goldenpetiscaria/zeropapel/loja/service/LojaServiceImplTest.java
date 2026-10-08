package com.goldenpetiscaria.zeropapel.loja.service;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.common.exception.RequisicaoInvalidaException;
import com.goldenpetiscaria.zeropapel.loja.dto.request.AdicionarLojaRequest;
import com.goldenpetiscaria.zeropapel.loja.dto.request.AtualizarLojaRequest;
import com.goldenpetiscaria.zeropapel.loja.dto.response.LojaResponseDTO;
import com.goldenpetiscaria.zeropapel.loja.entity.Loja;
import com.goldenpetiscaria.zeropapel.loja.repository.LojaRepository;
import org.junit.jupiter.api.DisplayName;
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
class LojaServiceImplTest {

    @Mock
    private LojaRepository lojaRepository;

    @InjectMocks
    private LojaServiceImpl service;

    @Test
    void adicionarLoja_criaLojaAtivaComNomeSemEspacosNasPontas() {
        when(lojaRepository.findByNome("Centro")).thenReturn(Optional.empty());
        when(lojaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        LojaResponseDTO resposta = service.adicionarLoja(new AdicionarLojaRequest("  Centro  "));

        assertThat(resposta.nome()).isEqualTo("Centro");
        assertThat(resposta.ativo()).isTrue();
    }

    @Test
    void adicionarLoja_lancaConflito_quandoONomeJaExiste() {
        when(lojaRepository.findByNome("Centro")).thenReturn(Optional.of(loja(1L, "Centro", true)));

        assertThatThrownBy(() -> service.adicionarLoja(new AdicionarLojaRequest("Centro")))
                .isInstanceOf(ConflitoException.class);

        verify(lojaRepository, never()).save(any());
    }

    @Test
    void atualizarLoja_lancaConflito_quandoONomeEDeOutraLoja() {
        Loja centro = loja(1L, "Centro", true);
        when(lojaRepository.findById(1L)).thenReturn(Optional.of(centro));
        when(lojaRepository.findByNome("Praia")).thenReturn(Optional.of(loja(2L, "Praia", true)));

        assertThatThrownBy(() -> service.atualizarLoja(1L, new AtualizarLojaRequest("Praia", null)))
                .isInstanceOf(ConflitoException.class);

        assertThat(centro.getNome()).isEqualTo("Centro");
    }

    @Test
    void atualizarLoja_lancaRequisicaoInvalida_quandoONomeVemEmBranco() {
        Loja centro = loja(1L, "Centro", true);
        when(lojaRepository.findById(1L)).thenReturn(Optional.of(centro));

        assertThatThrownBy(() -> service.atualizarLoja(1L, new AtualizarLojaRequest("   ", null)))
                .isInstanceOf(RequisicaoInvalidaException.class);

        assertThat(centro.getNome()).isEqualTo("Centro");
        verify(lojaRepository, never()).save(any());
    }

    @Test
    @DisplayName("sem loja ativa o PDV nao tem onde registrar venda")
    void atualizarLoja_lancaConflito_quandoDesativariaAUnicaLojaAtiva() {
        Loja centro = loja(1L, "Centro", true);
        when(lojaRepository.findById(1L)).thenReturn(Optional.of(centro));
        when(lojaRepository.countByAtivoTrue()).thenReturn(1L);

        assertThatThrownBy(() -> service.atualizarLoja(1L, new AtualizarLojaRequest(null, false)))
                .isInstanceOf(ConflitoException.class);

        assertThat(centro.isAtivo()).isTrue();
        verify(lojaRepository, never()).save(any());
    }

    @Test
    void atualizarLoja_desativa_quandoHaOutraLojaAtiva() {
        Loja centro = loja(1L, "Centro", true);
        when(lojaRepository.findById(1L)).thenReturn(Optional.of(centro));
        when(lojaRepository.countByAtivoTrue()).thenReturn(2L);
        when(lojaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        LojaResponseDTO resposta = service.atualizarLoja(1L, new AtualizarLojaRequest(null, false));

        assertThat(resposta.ativo()).isFalse();
    }

    @Test
    void buscarEntidadePorId_lancaRecursoNaoEncontrado_quandoIdNaoExiste() {
        when(lojaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarEntidadePorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Loja loja(Long id, String nome, boolean ativo) {
        Loja loja = new Loja();
        loja.setId(id);
        loja.setNome(nome);
        loja.setAtivo(ativo);
        return loja;
    }
}
