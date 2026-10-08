package com.goldenpetiscaria.zeropapel.canaldevenda.service;

import com.goldenpetiscaria.zeropapel.canaldevenda.dto.request.AdicionarCanalDeVendaRequest;
import com.goldenpetiscaria.zeropapel.canaldevenda.dto.request.AtualizarCanalDeVendaRequest;
import com.goldenpetiscaria.zeropapel.canaldevenda.dto.response.CanalDeVendaResponseDTO;
import com.goldenpetiscaria.zeropapel.canaldevenda.entity.CanalDeVenda;
import com.goldenpetiscaria.zeropapel.canaldevenda.repository.CanalDeVendaRepository;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.loja.entity.Loja;
import com.goldenpetiscaria.zeropapel.loja.service.LojaService;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import com.goldenpetiscaria.zeropapel.plataforma.service.PlataformaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanalDeVendaServiceImplTest {

    private static final Long LOJA_ID = 7L;
    private static final Long PLATAFORMA_ID = 3L;

    @Mock
    private CanalDeVendaRepository canalDeVendaRepository;

    @Mock
    private LojaService lojaService;

    @Mock
    private PlataformaService plataformaService;

    @InjectMocks
    private CanalDeVendaServiceImpl service;

    @Nested
    class Cadastro {

        @Test
        void adicionarCanal_criaCanalAtivoComTaxaEMerchantId() {
            when(lojaService.buscarEntidadePorId(LOJA_ID)).thenReturn(loja(true));
            when(plataformaService.buscarEntidadePorId(PLATAFORMA_ID)).thenReturn(plataforma());
            when(canalDeVendaRepository.findByLojaIdAndPlataformaId(LOJA_ID, PLATAFORMA_ID)).thenReturn(Optional.empty());
            when(canalDeVendaRepository.findByMerchantId("abc-123")).thenReturn(Optional.empty());
            when(canalDeVendaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            CanalDeVendaResponseDTO resposta = service.adicionarCanal(new AdicionarCanalDeVendaRequest(
                    LOJA_ID, PLATAFORMA_ID, new BigDecimal("12.00"), "  abc-123  "));

            assertThat(resposta.lojaNome()).isEqualTo("Loja principal");
            assertThat(resposta.plataformaNome()).isEqualTo("iFood");
            assertThat(resposta.taxaPercentual()).isEqualByComparingTo("12.00");
            assertThat(resposta.merchantId()).isEqualTo("abc-123");
            assertThat(resposta.ativo()).isTrue();
        }

        @Test
        @DisplayName("uma loja tem no maximo um canal por plataforma")
        void adicionarCanal_lancaConflito_quandoALojaJaTemCanalNaPlataforma() {
            when(lojaService.buscarEntidadePorId(LOJA_ID)).thenReturn(loja(true));
            when(plataformaService.buscarEntidadePorId(PLATAFORMA_ID)).thenReturn(plataforma());
            when(canalDeVendaRepository.findByLojaIdAndPlataformaId(LOJA_ID, PLATAFORMA_ID))
                    .thenReturn(Optional.of(canal(1L, true, true)));

            assertThatThrownBy(() -> service.adicionarCanal(new AdicionarCanalDeVendaRequest(
                    LOJA_ID, PLATAFORMA_ID, new BigDecimal("12.00"), null)))
                    .isInstanceOf(ConflitoException.class);

            verify(canalDeVendaRepository, never()).save(any());
        }

        @Test
        @DisplayName("merchantId roteia o evento para uma unica loja")
        void adicionarCanal_lancaConflito_quandoOMerchantIdJaEDeOutroCanal() {
            when(lojaService.buscarEntidadePorId(LOJA_ID)).thenReturn(loja(true));
            when(plataformaService.buscarEntidadePorId(PLATAFORMA_ID)).thenReturn(plataforma());
            when(canalDeVendaRepository.findByLojaIdAndPlataformaId(LOJA_ID, PLATAFORMA_ID)).thenReturn(Optional.empty());
            when(canalDeVendaRepository.findByMerchantId("abc-123")).thenReturn(Optional.of(canal(99L, true, true)));

            assertThatThrownBy(() -> service.adicionarCanal(new AdicionarCanalDeVendaRequest(
                    LOJA_ID, PLATAFORMA_ID, new BigDecimal("12.00"), "abc-123")))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("merchantId");

            verify(canalDeVendaRepository, never()).save(any());
        }

        @Test
        void adicionarCanal_gravaMerchantIdNulo_quandoVemEmBranco() {
            when(lojaService.buscarEntidadePorId(LOJA_ID)).thenReturn(loja(true));
            when(plataformaService.buscarEntidadePorId(PLATAFORMA_ID)).thenReturn(plataforma());
            when(canalDeVendaRepository.findByLojaIdAndPlataformaId(LOJA_ID, PLATAFORMA_ID)).thenReturn(Optional.empty());
            when(canalDeVendaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            CanalDeVendaResponseDTO resposta = service.adicionarCanal(new AdicionarCanalDeVendaRequest(
                    LOJA_ID, PLATAFORMA_ID, BigDecimal.ZERO, "   "));

            assertThat(resposta.merchantId()).isNull();
        }
    }

    @Nested
    class Atualizacao {

        @Test
        void atualizarCanal_alteraSoOQueVeioNoRequest() {
            CanalDeVenda existente = canal(1L, true, true);
            existente.setMerchantId("abc-123");
            when(canalDeVendaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(canalDeVendaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarCanal(1L, new AtualizarCanalDeVendaRequest(new BigDecimal("14.50"), null, null));

            assertThat(existente.getTaxaPercentual()).isEqualByComparingTo("14.50");
            assertThat(existente.getMerchantId()).isEqualTo("abc-123");
            assertThat(existente.isAtivo()).isTrue();
        }

        @Test
        void atualizarCanal_aceitaOProprioMerchantId() {
            CanalDeVenda existente = canal(1L, true, true);
            existente.setMerchantId("abc-123");
            when(canalDeVendaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(canalDeVendaRepository.findByMerchantId("abc-123")).thenReturn(Optional.of(existente));
            when(canalDeVendaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarCanal(1L, new AtualizarCanalDeVendaRequest(null, "abc-123", null));

            assertThat(existente.getMerchantId()).isEqualTo("abc-123");
        }

        @Test
        void atualizarCanal_lancaConflito_quandoOMerchantIdJaEDeOutroCanal() {
            CanalDeVenda existente = canal(1L, true, true);
            when(canalDeVendaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(canalDeVendaRepository.findByMerchantId("abc-123")).thenReturn(Optional.of(canal(99L, true, true)));

            assertThatThrownBy(() -> service.atualizarCanal(1L,
                    new AtualizarCanalDeVendaRequest(null, "abc-123", null)))
                    .isInstanceOf(ConflitoException.class);

            assertThat(existente.getMerchantId()).isNull();
            verify(canalDeVendaRepository, never()).save(any());
        }

        @Test
        void atualizarCanal_limpaOMerchantId_quandoVemVazio() {
            CanalDeVenda existente = canal(1L, true, true);
            existente.setMerchantId("abc-123");
            when(canalDeVendaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(canalDeVendaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarCanal(1L, new AtualizarCanalDeVendaRequest(null, "", null));

            assertThat(existente.getMerchantId()).isNull();
        }

        @Test
        void atualizarCanal_desativaSemMexerNaTaxaNemNoMerchantId() {
            CanalDeVenda existente = canal(1L, true, true);
            existente.setMerchantId("abc-123");
            when(canalDeVendaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(canalDeVendaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            CanalDeVendaResponseDTO resposta = service.atualizarCanal(1L,
                    new AtualizarCanalDeVendaRequest(null, null, false));

            assertThat(resposta.ativo()).isFalse();
            assertThat(existente.isAtivo()).isFalse();
            assertThat(existente.getTaxaPercentual()).isEqualByComparingTo("12.00");
            assertThat(existente.getMerchantId()).isEqualTo("abc-123");
        }
    }

    @Nested
    class Exclusao {

        @Test
        void excluirCanal_excluiOCanalEncontrado() {
            CanalDeVenda existente = canal(1L, true, true);
            when(canalDeVendaRepository.findById(1L)).thenReturn(Optional.of(existente));

            service.excluirCanal(1L);

            verify(canalDeVendaRepository).delete(existente);
        }

        @Test
        void excluirCanal_lancaRecursoNaoEncontrado_quandoIdNaoExiste() {
            when(canalDeVendaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.excluirCanal(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(canalDeVendaRepository, never()).delete(any());
        }
    }

    @Nested
    class CanalAtivo {

        @Test
        void buscarCanalAtivo_devolveOCanal_quandoCanalELojaEstaoAtivos() {
            CanalDeVenda ativo = canal(1L, true, true);
            when(canalDeVendaRepository.findByLojaIdAndPlataformaId(LOJA_ID, PLATAFORMA_ID)).thenReturn(Optional.of(ativo));

            assertThat(service.buscarCanalAtivo(LOJA_ID, PLATAFORMA_ID)).isSameAs(ativo);
        }

        @Test
        @DisplayName("loja que nao opera na plataforma nao tem canal")
        void buscarCanalAtivo_lancaRecursoNaoEncontrado_quandoNaoHaCanal() {
            when(canalDeVendaRepository.findByLojaIdAndPlataformaId(LOJA_ID, PLATAFORMA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarCanalAtivo(LOJA_ID, PLATAFORMA_ID))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @Test
        void buscarCanalAtivo_lancaConflito_quandoOCanalEstaInativo() {
            when(canalDeVendaRepository.findByLojaIdAndPlataformaId(LOJA_ID, PLATAFORMA_ID))
                    .thenReturn(Optional.of(canal(1L, false, true)));

            assertThatThrownBy(() -> service.buscarCanalAtivo(LOJA_ID, PLATAFORMA_ID))
                    .isInstanceOf(ConflitoException.class);
        }

        @Test
        void buscarCanalAtivo_lancaConflito_quandoALojaEstaInativa() {
            when(canalDeVendaRepository.findByLojaIdAndPlataformaId(LOJA_ID, PLATAFORMA_ID))
                    .thenReturn(Optional.of(canal(1L, true, false)));

            assertThatThrownBy(() -> service.buscarCanalAtivo(LOJA_ID, PLATAFORMA_ID))
                    .isInstanceOf(ConflitoException.class);
        }
    }

    private CanalDeVenda canal(Long id, boolean canalAtivo, boolean lojaAtiva) {
        CanalDeVenda canal = new CanalDeVenda();
        canal.setId(id);
        canal.setLoja(loja(lojaAtiva));
        canal.setPlataforma(plataforma());
        canal.setTaxaPercentual(new BigDecimal("12.00"));
        canal.setAtivo(canalAtivo);
        return canal;
    }

    private Loja loja(boolean ativo) {
        Loja loja = new Loja();
        loja.setId(LOJA_ID);
        loja.setNome("Loja principal");
        loja.setAtivo(ativo);
        return loja;
    }

    private Plataforma plataforma() {
        Plataforma plataforma = new Plataforma();
        plataforma.setId(PLATAFORMA_ID);
        plataforma.setNome("iFood");
        return plataforma;
    }
}
