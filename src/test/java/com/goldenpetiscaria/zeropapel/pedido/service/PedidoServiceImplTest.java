package com.goldenpetiscaria.zeropapel.pedido.service;

import com.goldenpetiscaria.zeropapel.canaldevenda.entity.CanalDeVenda;
import com.goldenpetiscaria.zeropapel.canaldevenda.enumerator.TratamentoTaxaEntrega;
import com.goldenpetiscaria.zeropapel.canaldevenda.service.CanalDeVendaService;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.common.exception.RequisicaoInvalidaException;
import com.goldenpetiscaria.zeropapel.formadepagamento.entity.FormaDePagamento;
import com.goldenpetiscaria.zeropapel.formadepagamento.service.FormaDePagamentoService;
import com.goldenpetiscaria.zeropapel.item.entity.Item;
import com.goldenpetiscaria.zeropapel.item.service.ItemService;
import com.goldenpetiscaria.zeropapel.loja.entity.Loja;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AdicionarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.ItemPedidoExternoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.ItemPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.RegistrarPedidoExternoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.PedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.entity.ItemPedido;
import com.goldenpetiscaria.zeropapel.pedido.entity.Pedido;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.ResultadoStatusExterno;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;
import com.goldenpetiscaria.zeropapel.pedido.repository.PedidoRepository;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    private static final Long LOJA_ID = 7L;
    private static final Long IFOOD_ID = 5L;
    private static final String ID_NO_IFOOD = "a1b2c3d4-0001";

    private static ValidatorFactory fabricaDeValidador;
    private static Validator validator;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private CanalDeVendaService canalDeVendaService;

    @Mock
    private FormaDePagamentoService formaDePagamentoService;

    @Mock
    private ItemService itemService;

    private PedidoServiceImpl service;

    @BeforeAll
    static void abrirValidador() {
        fabricaDeValidador = Validation.buildDefaultValidatorFactory();
        validator = fabricaDeValidador.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        fabricaDeValidador.close();
    }

    @BeforeEach
    void montarService() {
        service = new PedidoServiceImpl(pedidoRepository, canalDeVendaService,
                formaDePagamentoService, itemService, validator);
    }

    @Nested
    class TaxasCongeladas {

        @Test
        @DisplayName("registrar congela a taxa da plataforma e do pagamento vigentes na venda")
        void registrarPedido_congelaAsTaxasVigentes() {
            CanalDeVenda ifood = canal(1L, "iFood", "12.00");
            FormaDePagamento credito = formaDePagamento(2L, "Credito", "2.99");
            Item cerveja = item(3L, "Cerveja", "9.00");
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L)).thenReturn(ifood);
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenReturn(credito);
            when(itemService.buscarEntidadePorId(3L)).thenReturn(cerveja);
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.registrarPedido(new AdicionarPedidoRequest(LOJA_ID, 1L, "Pedro", 2L,
                    List.of(new ItemPedidoRequest(3L, 2))));

            Pedido salvo = capturarSalvo();
            assertThat(salvo.getTaxaPlataformaPercentual()).isEqualByComparingTo("12.00");
            assertThat(salvo.getTaxaPagamentoPercentual()).isEqualByComparingTo("2.99");
        }

        @Test
        @DisplayName("taxa congelada nao muda quando a plataforma e reajustada depois")
        void registrarPedido_naoAcompanhaReajusteDaPlataforma() {
            CanalDeVenda ifood = canal(1L, "iFood", "12.00");
            FormaDePagamento pix = formaDePagamento(2L, "Pix", "0.00");
            Item cerveja = item(3L, "Cerveja", "9.00");
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L)).thenReturn(ifood);
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenReturn(pix);
            when(itemService.buscarEntidadePorId(3L)).thenReturn(cerveja);
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.registrarPedido(new AdicionarPedidoRequest(LOJA_ID, 1L, null, 2L,
                    List.of(new ItemPedidoRequest(3L, 1))));
            Pedido salvo = capturarSalvo();

            ifood.setTaxaPercentual(new BigDecimal("14.00"));

            assertThat(salvo.getTaxaPlataformaPercentual()).isEqualByComparingTo("12.00");
        }

        @Test
        @DisplayName("trocar a plataforma de um pedido recongela a taxa com a da nova")
        void atualizarPedido_recongelaATaxa_quandoAPlataformaMuda() {
            Pedido existente = pedidoExistente(canal(1L, "Balcao", "0.00"),
                    formaDePagamento(2L, "Pix", "0.00"));
            CanalDeVenda ifood = canal(9L, "iFood", "12.00");
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 9L)).thenReturn(ifood);
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarPedido(1L, new AtualizarPedidoRequest(9L, null, null, null));

            assertThat(existente.getTaxaPlataformaPercentual()).isEqualByComparingTo("12.00");
        }

        @Test
        void atualizarPedido_recongelaATaxa_quandoAFormaDePagamentoMuda() {
            Pedido existente = pedidoExistente(canal(1L, "Balcao", "0.00"),
                    formaDePagamento(2L, "Pix", "0.00"));
            FormaDePagamento credito = formaDePagamento(8L, "Credito", "2.99");
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(formaDePagamentoService.buscarEntidadePorId(8L)).thenReturn(credito);
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarPedido(1L, new AtualizarPedidoRequest(null, null, 8L, null));

            assertThat(existente.getTaxaPagamentoPercentual()).isEqualByComparingTo("2.99");
        }

        @Test
        @DisplayName("mexer so no nome do cliente nao recongela taxa nenhuma")
        void atualizarPedido_preservaAsTaxas_quandoSoONomeMuda() {
            Pedido existente = pedidoExistente(canal(1L, "iFood", "12.00"),
                    formaDePagamento(2L, "Credito", "2.99"));
            existente.setTaxaPlataformaPercentual(new BigDecimal("10.00"));
            existente.setTaxaPagamentoPercentual(new BigDecimal("1.50"));
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarPedido(1L, new AtualizarPedidoRequest(null, "Outro Nome", null, null));

            assertThat(existente.getTaxaPlataformaPercentual()).isEqualByComparingTo("10.00");
            assertThat(existente.getTaxaPagamentoPercentual()).isEqualByComparingTo("1.50");
            assertThat(existente.getNomeCliente()).isEqualTo("Outro Nome");
        }

        @Test
        @DisplayName("a SPA reenvia a mesma plataforma e a mesma forma de pagamento em toda edicao")
        void atualizarPedido_preservaAsTaxas_quandoPlataformaEFormaDePagamentoSaoAsMesmas() {
            Pedido existente = pedidoExistente(canal(1L, "iFood", "12.00"),
                    formaDePagamento(2L, "Credito", "2.99"));
            existente.setTaxaPlataformaPercentual(new BigDecimal("10.00"));
            existente.setTaxaPagamentoPercentual(new BigDecimal("1.50"));
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            PedidoResponseDTO resposta = service.atualizarPedido(1L,
                    new AtualizarPedidoRequest(1L, "Outro Nome", 2L, null));

            verifyNoInteractions(canalDeVendaService, formaDePagamentoService);
            assertThat(existente.getTaxaPlataformaPercentual()).isEqualByComparingTo("10.00");
            assertThat(existente.getTaxaPagamentoPercentual()).isEqualByComparingTo("1.50");
            assertThat(resposta.nomeCliente()).isEqualTo("Outro Nome");
            assertThat(resposta.plataformaId()).isEqualTo(1L);
            assertThat(resposta.formaDePagamentoId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("pedido de canal desativado ainda aceita edicao dos itens")
        void atualizarPedido_trocaOsItens_quandoOCanalDoPedidoFoiDesativado() {
            Pedido existente = pedidoExistente(canal(1L, "iFood", "12.00"),
                    formaDePagamento(2L, "Credito", "2.99"));
            lenient().when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L))
                    .thenThrow(new ConflitoException("O canal esta inativo"));
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(itemService.buscarEntidadePorId(4L)).thenReturn(item(4L, "Porcao", "30.00"));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            PedidoResponseDTO resposta = service.atualizarPedido(1L,
                    new AtualizarPedidoRequest(1L, null, 2L, List.of(new ItemPedidoRequest(4L, 2))));

            verify(canalDeVendaService, never()).buscarCanalAtivo(any(), any());
            assertThat(existente.getItens()).hasSize(1);
            assertThat(existente.getItens().getFirst().getItem().getId()).isEqualTo(4L);
            assertThat(resposta.valor()).isEqualByComparingTo("60.00");
            assertThat(existente.getTaxaPlataformaPercentual()).isEqualByComparingTo("12.00");
        }
    }

    @Nested
    class Registro {

        @Test
        void registrarPedido_travaOPrecoDoItemEComputaOValor() {
            CanalDeVenda balcao = canal(1L, "Balcao", "0.00");
            FormaDePagamento pix = formaDePagamento(2L, "Pix", "0.00");
            Item cerveja = item(3L, "Cerveja", "9.50");
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L)).thenReturn(balcao);
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenReturn(pix);
            when(itemService.buscarEntidadePorId(3L)).thenReturn(cerveja);
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            PedidoResponseDTO resposta = service.registrarPedido(new AdicionarPedidoRequest(
                    LOJA_ID, 1L, "Pedro", 2L, List.of(new ItemPedidoRequest(3L, 3))));

            Pedido salvo = capturarSalvo();
            assertThat(salvo.getItens()).hasSize(1);
            assertThat(salvo.getItens().getFirst().getPrecoUnitario()).isEqualByComparingTo("9.50");
            assertThat(salvo.getValor()).isEqualByComparingTo("28.50");
            assertThat(salvo.getStatus()).isEqualTo(StatusPedido.EM_ABERTO);
            assertThat(resposta.valor()).isEqualByComparingTo("28.50");
        }

        @Test
        @DisplayName("o nome do item fica gravado no pedido e nao acompanha renomeacao do cardapio")
        void registrarPedido_gravaONomeDoItem_quandoOItemERenomeadoDepois() {
            Item cerveja = item(3L, "Cerveja", "9.50");
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L)).thenReturn(canal(1L, "Balcao", "0.00"));
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenReturn(formaDePagamento(2L, "Pix", "0.00"));
            when(itemService.buscarEntidadePorId(3L)).thenReturn(cerveja);
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.registrarPedido(new AdicionarPedidoRequest(
                    LOJA_ID, 1L, null, 2L, List.of(new ItemPedidoRequest(3L, 1))));
            Pedido salvo = capturarSalvo();

            cerveja.setNome("Cerveja Long Neck");

            assertThat(salvo.getItens().getFirst().getItem()).isSameAs(cerveja);
            assertThat(salvo.getItens().getFirst().getNomeItem()).isEqualTo("Cerveja");
            assertThat(salvo.getPlataformaPedidoId()).isNull();
        }

        @Test
        void registrarPedido_devolveItemIdENomeGravado() {
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L)).thenReturn(canal(1L, "Balcao", "0.00"));
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenReturn(formaDePagamento(2L, "Pix", "0.00"));
            when(itemService.buscarEntidadePorId(3L)).thenReturn(item(3L, "Cerveja", "9.50"));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            PedidoResponseDTO resposta = service.registrarPedido(new AdicionarPedidoRequest(
                    LOJA_ID, 1L, null, 2L, List.of(new ItemPedidoRequest(3L, 1))));

            assertThat(resposta.plataformaPedidoId()).isNull();
            assertThat(resposta.itens().getFirst().itemId()).isEqualTo(3L);
            assertThat(resposta.itens().getFirst().itemNome()).isEqualTo("Cerveja");
        }

        @Test
        void registrarPedido_vinculaOPedidoALojaEAPlataformaDoCanal() {
            CanalDeVenda balcao = canal(1L, "Balcao", "0.00");
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L)).thenReturn(balcao);
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenReturn(formaDePagamento(2L, "Pix", "0.00"));
            when(itemService.buscarEntidadePorId(3L)).thenReturn(item(3L, "Cerveja", "9.00"));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            PedidoResponseDTO resposta = service.registrarPedido(new AdicionarPedidoRequest(
                    LOJA_ID, 1L, null, 2L, List.of(new ItemPedidoRequest(3L, 1))));

            Pedido salvo = capturarSalvo();
            assertThat(salvo.getLoja()).isSameAs(balcao.getLoja());
            assertThat(salvo.getPlataforma()).isSameAs(balcao.getPlataforma());
            assertThat(resposta.lojaId()).isEqualTo(LOJA_ID);
        }

        @Test
        @DisplayName("loja ou canal inativo nao recebe pedido novo")
        void registrarPedido_propagaConflito_quandoOCanalEstaInativo() {
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L))
                    .thenThrow(new ConflitoException("O canal esta inativo"));

            assertThatThrownBy(() -> service.registrarPedido(new AdicionarPedidoRequest(
                    LOJA_ID, 1L, null, 2L, List.of(new ItemPedidoRequest(3L, 1)))))
                    .isInstanceOf(ConflitoException.class);

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        void registrarPedido_lancaRecursoNaoEncontrado_quandoOCanalNaoExiste() {
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 99L))
                    .thenThrow(new RecursoNaoEncontradoException("Canal de venda não encontrado"));

            assertThatThrownBy(() -> service.registrarPedido(new AdicionarPedidoRequest(
                    LOJA_ID, 99L, null, 2L, List.of(new ItemPedidoRequest(3L, 1)))))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        void registrarPedido_lancaRecursoNaoEncontrado_quandoItemNaoExiste() {
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L)).thenReturn(canal(1L, "Balcao", "0.00"));
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenReturn(formaDePagamento(2L, "Pix", "0.00"));
            when(itemService.buscarEntidadePorId(99L)).thenThrow(new RecursoNaoEncontradoException("não encontrado"));

            assertThatThrownBy(() -> service.registrarPedido(new AdicionarPedidoRequest(
                    LOJA_ID, 1L, null, 2L, List.of(new ItemPedidoRequest(99L, 1)))))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(pedidoRepository, never()).save(any());
        }
    }

    @Nested
    class PedidoExterno {

        @Test
        @DisplayName("pedido externo entra com itens livres, sem consultar o cardapio")
        void registrarPedidoExterno_criaPedidoComItensLivres() {
            CanalDeVenda ifood = canal(IFOOD_ID, "iFood", "12.00");
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.empty());
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, IFOOD_ID)).thenReturn(ifood);
            when(formaDePagamentoService.buscarEntidadePorId(2L))
                    .thenReturn(formaDePagamento(2L, "Online", "0.00"));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
            LocalDateTime horarioNoIfood = LocalDateTime.of(2026, 10, 7, 20, 15);

            PedidoResponseDTO resposta = service.registrarPedidoExterno(new RegistrarPedidoExternoRequest(
                    LOJA_ID, IFOOD_ID, ID_NO_IFOOD, "Maria", 2L, horarioNoIfood, null, false,
                    List.of(new ItemPedidoExternoRequest("X-Burger Promo", 2, new BigDecimal("24.90")),
                            new ItemPedidoExternoRequest("Refrigerante Lata", 1, new BigDecimal("6.00")))));

            Pedido salvo = capturarSalvo();
            verifyNoInteractions(itemService);
            assertThat(salvo.getPlataformaPedidoId()).isEqualTo(ID_NO_IFOOD);
            assertThat(salvo.getLoja()).isSameAs(ifood.getLoja());
            assertThat(salvo.getPlataforma()).isSameAs(ifood.getPlataforma());
            assertThat(salvo.getNomeCliente()).isEqualTo("Maria");
            assertThat(salvo.getHorarioPedido()).isEqualTo(horarioNoIfood);
            assertThat(salvo.getStatus()).isEqualTo(StatusPedido.EM_ABERTO);
            assertThat(salvo.isFechado()).isFalse();
            assertThat(salvo.getValor()).isEqualByComparingTo("55.80");
            assertThat(salvo.getItens()).hasSize(2);
            ItemPedido primeiro = salvo.getItens().getFirst();
            assertThat(primeiro.getItem()).isNull();
            assertThat(primeiro.getPedido()).isSameAs(salvo);
            assertThat(primeiro.getNomeItem()).isEqualTo("X-Burger Promo");
            assertThat(primeiro.getQuantidade()).isEqualTo(2);
            assertThat(primeiro.getPrecoUnitario()).isEqualByComparingTo("24.90");
            assertThat(resposta.plataformaPedidoId()).isEqualTo(ID_NO_IFOOD);
            assertThat(resposta.valor()).isEqualByComparingTo("55.80");
            assertThat(resposta.itens().getFirst().itemId()).isNull();
            assertThat(resposta.itens().getFirst().itemNome()).isEqualTo("X-Burger Promo");
            assertThat(resposta.itens().getFirst().subtotal()).isEqualByComparingTo("49.80");
        }

        @Test
        void registrarPedidoExterno_congelaAsTaxasVigentes() {
            CanalDeVenda ifood = canal(IFOOD_ID, "iFood", "12.00");
            FormaDePagamento credito = formaDePagamento(2L, "Credito", "2.99");
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.empty());
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, IFOOD_ID)).thenReturn(ifood);
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenReturn(credito);
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.registrarPedidoExterno(pedidoExternoValido());
            Pedido salvo = capturarSalvo();

            ifood.setTaxaPercentual(new BigDecimal("14.00"));
            credito.setTaxaPercentual(new BigDecimal("4.50"));

            assertThat(salvo.getTaxaPlataformaPercentual()).isEqualByComparingTo("12.00");
            assertThat(salvo.getTaxaPagamentoPercentual()).isEqualByComparingTo("2.99");
        }

        @Test
        void registrarPedidoExterno_usaOHorarioAtual_quandoOHorarioNaoVem() {
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.empty());
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, IFOOD_ID)).thenReturn(canal(IFOOD_ID, "iFood", "12.00"));
            when(formaDePagamentoService.buscarEntidadePorId(2L))
                    .thenReturn(formaDePagamento(2L, "Online", "0.00"));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
            LocalDateTime antes = LocalDateTime.now();

            service.registrarPedidoExterno(new RegistrarPedidoExternoRequest(
                    LOJA_ID, IFOOD_ID, ID_NO_IFOOD, null, 2L, null, null, false,
                    List.of(new ItemPedidoExternoRequest("X-Burger", 1, new BigDecimal("20.00")))));

            assertThat(capturarSalvo().getHorarioPedido()).isBetween(antes, LocalDateTime.now());
        }

        @Test
        @DisplayName("reentrega do mesmo pedido devolve o existente, sem gravar nem alterar nada")
        void registrarPedidoExterno_devolveOExistente_quandoOPedidoJaFoiRegistrado() {
            Pedido existente = pedidoExterno();
            existente.setStatus(StatusPedido.EM_PREPARO);
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.of(existente));

            PedidoResponseDTO resposta = service.registrarPedidoExterno(pedidoExternoValido());

            verify(pedidoRepository, never()).save(any());
            verifyNoInteractions(canalDeVendaService, formaDePagamentoService, itemService);
            assertThat(resposta.id()).isEqualTo(1L);
            assertThat(resposta.status()).isEqualTo(StatusPedido.EM_PREPARO);
            assertThat(resposta.valor()).isEqualByComparingTo("19.00");
            assertThat(existente.getItens()).hasSize(1);
            assertThat(existente.getItens().getFirst().getNomeItem()).isEqualTo("X-Burger");
        }

        @Test
        void registrarPedidoExterno_lancaRequisicaoInvalida_quandoOIdNaPlataformaEstaEmBranco() {
            assertThatThrownBy(() -> service.registrarPedidoExterno(new RegistrarPedidoExternoRequest(
                    LOJA_ID, IFOOD_ID, "  ", null, 2L, null, null, false,
                    List.of(new ItemPedidoExternoRequest("X-Burger", 1, new BigDecimal("20.00"))))))
                    .isInstanceOf(RequisicaoInvalidaException.class)
                    .hasMessageContaining("identificador do pedido na plataforma");

            verifyNoInteractions(pedidoRepository, canalDeVendaService);
        }

        @Test
        void registrarPedidoExterno_lancaRequisicaoInvalida_quandoNaoHaItens() {
            assertThatThrownBy(() -> service.registrarPedidoExterno(new RegistrarPedidoExternoRequest(
                    LOJA_ID, IFOOD_ID, ID_NO_IFOOD, null, 2L, null, null, false, List.of())))
                    .isInstanceOf(RequisicaoInvalidaException.class)
                    .hasMessageContaining("pelo menos um item");

            verifyNoInteractions(pedidoRepository, canalDeVendaService);
        }

        @Test
        @DisplayName("a validacao desce ate o item: quantidade zero, preco negativo e nome em branco")
        void registrarPedidoExterno_lancaRequisicaoInvalida_quandoUmItemEInvalido() {
            assertThatThrownBy(() -> service.registrarPedidoExterno(new RegistrarPedidoExternoRequest(
                    LOJA_ID, IFOOD_ID, ID_NO_IFOOD, null, 2L, null, null, false,
                    List.of(new ItemPedidoExternoRequest(" ", 0, new BigDecimal("-1.00"))))))
                    .isInstanceOf(RequisicaoInvalidaException.class)
                    .hasMessageContaining("nome do item")
                    .hasMessageContaining("pelo menos 1")
                    .hasMessageContaining("negativo");

            verifyNoInteractions(pedidoRepository, canalDeVendaService);
        }

        @Test
        void registrarPedidoExterno_lancaRequisicaoInvalida_quandoLojaPlataformaOuPagamentoFaltam() {
            assertThatThrownBy(() -> service.registrarPedidoExterno(new RegistrarPedidoExternoRequest(
                    null, null, ID_NO_IFOOD, null, null, null, null, false,
                    List.of(new ItemPedidoExternoRequest("X-Burger", 1, new BigDecimal("20.00"))))))
                    .isInstanceOf(RequisicaoInvalidaException.class)
                    .hasMessageContaining("loja")
                    .hasMessageContaining("plataforma")
                    .hasMessageContaining("forma de pagamento");

            verifyNoInteractions(pedidoRepository, canalDeVendaService);
        }

        @Test
        void registrarPedidoExterno_lancaRequisicaoInvalida_quandoORequestENulo() {
            assertThatThrownBy(() -> service.registrarPedidoExterno(null))
                    .isInstanceOf(RequisicaoInvalidaException.class);

            verifyNoInteractions(pedidoRepository, canalDeVendaService);
        }

        @Test
        @DisplayName("loja ou canal inativo nao recebe pedido externo")
        void registrarPedidoExterno_propagaConflito_quandoOCanalEstaInativo() {
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.empty());
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, IFOOD_ID))
                    .thenThrow(new ConflitoException("O canal esta inativo"));

            assertThatThrownBy(() -> service.registrarPedidoExterno(pedidoExternoValido()))
                    .isInstanceOf(ConflitoException.class);

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        void registrarPedidoExterno_lancaRecursoNaoEncontrado_quandoAFormaDePagamentoNaoExiste() {
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.empty());
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, IFOOD_ID)).thenReturn(canal(IFOOD_ID, "iFood", "12.00"));
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenThrow(new RecursoNaoEncontradoException("não encontrado"));

            assertThatThrownBy(() -> service.registrarPedidoExterno(pedidoExternoValido()))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("pedido vindo de plataforma externa nao aceita edicao de conteudo")
        void atualizarPedido_lancaConflito_quandoOPedidoEExterno() {
            Pedido externo = pedidoExterno();
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(externo));

            assertThatThrownBy(() -> service.atualizarPedido(1L,
                    new AtualizarPedidoRequest(null, "Outro Nome", null, List.of(new ItemPedidoRequest(3L, 1)))))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("plataforma externa");

            assertThat(externo.getNomeCliente()).isEqualTo("Maria");
            assertThat(externo.getItens().getFirst().getNomeItem()).isEqualTo("X-Burger");
            verifyNoInteractions(itemService);
            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("o status do pedido externo vem da plataforma, nao do kanban")
        void atualizarStatus_lancaConflito_quandoOPedidoEExterno() {
            Pedido externo = pedidoExterno();
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(externo));

            assertThatThrownBy(() -> service.atualizarStatus(1L, StatusPedido.CANCELADO))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("plataforma externa");

            assertThat(externo.getStatus()).isEqualTo(StatusPedido.EM_ABERTO);
            verify(pedidoRepository, never()).save(any());
        }

        @Test
        void excluirPedido_lancaConflito_quandoOPedidoEExterno() {
            Pedido externo = pedidoExterno();
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(externo));

            assertThatThrownBy(() -> service.excluirPedido(1L))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("plataforma externa");

            verify(pedidoRepository, never()).delete(any());
        }

        @Test
        @DisplayName("pedido externo ja fechado responde pela regra do fechamento, que vem antes")
        void atualizarStatus_recusaPeloFechamento_quandoOPedidoExternoJaFoiFechado() {
            Pedido externo = pedidoExterno();
            externo.setFechado(true);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(externo));

            assertThatThrownBy(() -> service.atualizarStatus(1L, StatusPedido.CANCELADO))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("fechamento de caixa");
        }
    }

    @Nested
    class TaxaDeEntrega {

        @Test
        void registrarPedidoExterno_congelaORegraDoCanal_quandoAPlataformaEntregou() {
            CanalDeVenda ifood = canal(IFOOD_ID, "iFood", "12.00");
            ifood.setTratamentoTaxaEntrega(TratamentoTaxaEntrega.DESCONTAR);

            Pedido salvo = registrarComEntrega(ifood, new BigDecimal("7.99"), true);
            ifood.setTratamentoTaxaEntrega(TratamentoTaxaEntrega.IGNORAR);

            assertThat(salvo.getTaxaEntrega()).isEqualByComparingTo("7.99");
            assertThat(salvo.getTratamentoTaxaEntrega()).isEqualTo(TratamentoTaxaEntrega.DESCONTAR);
            assertThat(salvo.getValor()).isEqualByComparingTo("19.00");
        }

        @Test
        void registrarPedidoExterno_usaARegraIgnorarDoCanal_quandoAPlataformaEntregou() {
            CanalDeVenda ifood = canal(IFOOD_ID, "iFood", "12.00");
            ifood.setTratamentoTaxaEntrega(TratamentoTaxaEntrega.IGNORAR);

            Pedido salvo = registrarComEntrega(ifood, new BigDecimal("5.00"), true);

            assertThat(salvo.getTaxaEntrega()).isEqualByComparingTo("5.00");
            assertThat(salvo.getTratamentoTaxaEntrega()).isEqualTo(TratamentoTaxaEntrega.IGNORAR);
        }

        @Test
        void registrarPedidoExterno_somaAEntrega_quandoALojaEntregou() {
            CanalDeVenda ifood = canal(IFOOD_ID, "iFood", "12.00");
            ifood.setTratamentoTaxaEntrega(TratamentoTaxaEntrega.DESCONTAR);

            Pedido salvo = registrarComEntrega(ifood, new BigDecimal("5.00"), false);

            assertThat(salvo.getTaxaEntrega()).isEqualByComparingTo("5.00");
            assertThat(salvo.getTratamentoTaxaEntrega()).isEqualTo(TratamentoTaxaEntrega.SOMAR);
        }

        @Test
        void registrarPedidoExterno_ignoraAEntrega_quandoNaoHaTaxa() {
            CanalDeVenda ifood = canal(IFOOD_ID, "iFood", "12.00");

            Pedido semTaxa = registrarComEntrega(ifood, null, true);

            assertThat(semTaxa.getTaxaEntrega()).isEqualByComparingTo("0");
            assertThat(semTaxa.getTratamentoTaxaEntrega()).isEqualTo(TratamentoTaxaEntrega.IGNORAR);
        }

        @Test
        void registrarPedidoExterno_lancaRequisicaoInvalida_quandoATaxaDeEntregaENegativa() {
            assertThatThrownBy(() -> service.registrarPedidoExterno(new RegistrarPedidoExternoRequest(
                    LOJA_ID, IFOOD_ID, ID_NO_IFOOD, null, 2L, null, new BigDecimal("-1.00"), true,
                    List.of(new ItemPedidoExternoRequest("X-Burger", 1, new BigDecimal("20.00"))))))
                    .isInstanceOf(RequisicaoInvalidaException.class)
                    .hasMessageContaining("taxa de entrega");
            verify(pedidoRepository, never()).save(any());
        }

        @Test
        void registrarPedido_naoTemTaxaDeEntrega() {
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L)).thenReturn(canal(1L, "Balcao", "0.00"));
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenReturn(formaDePagamento(2L, "Pix", "0.00"));
            when(itemService.buscarEntidadePorId(3L)).thenReturn(item(3L, "Cerveja", "9.50"));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.registrarPedido(new AdicionarPedidoRequest(
                    LOJA_ID, 1L, "Pedro", 2L, List.of(new ItemPedidoRequest(3L, 1))));

            Pedido salvo = capturarSalvo();
            assertThat(salvo.getTaxaEntrega()).isEqualByComparingTo("0");
            assertThat(salvo.getTratamentoTaxaEntrega()).isEqualTo(TratamentoTaxaEntrega.IGNORAR);
        }

        private Pedido registrarComEntrega(CanalDeVenda canal, BigDecimal taxaEntrega, boolean entregaPelaPlataforma) {
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.empty());
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, IFOOD_ID)).thenReturn(canal);
            when(formaDePagamentoService.buscarEntidadePorId(2L))
                    .thenReturn(formaDePagamento(2L, "Online", "0.00"));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.registrarPedidoExterno(new RegistrarPedidoExternoRequest(
                    LOJA_ID, IFOOD_ID, ID_NO_IFOOD, null, 2L, null, taxaEntrega, entregaPelaPlataforma,
                    List.of(new ItemPedidoExternoRequest("X-Burger", 2, new BigDecimal("9.50")))));
            return capturarSalvo();
        }
    }

    @Nested
    class StatusExterno {

        @Test
        @DisplayName("o evento de status pode chegar antes do pedido")
        void aplicarStatusExterno_devolveNaoEncontrado_quandoOPedidoAindaNaoExiste() {
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.empty());

            ResultadoStatusExterno resultado = service.aplicarStatusExterno(IFOOD_ID, ID_NO_IFOOD, StatusPedido.ACEITO);

            assertThat(resultado).isEqualTo(ResultadoStatusExterno.PEDIDO_NAO_ENCONTRADO);
            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @ExtendWith(OutputCaptureExtension.class)
        @DisplayName("evento tardio em pedido fechado nao altera nada, nao lanca e so gera WARN")
        void aplicarStatusExterno_naoAlteraELoga_quandoOPedidoJaFoiFechado(CapturedOutput saida) {
            Pedido fechado = pedidoExternoCom(StatusPedido.EM_ROTA);
            fechado.setFechado(true);
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.of(fechado));

            ResultadoStatusExterno resultado = service.aplicarStatusExterno(IFOOD_ID, ID_NO_IFOOD, StatusPedido.CANCELADO);

            assertThat(resultado).isEqualTo(ResultadoStatusExterno.PEDIDO_FECHADO);
            assertThat(fechado.getStatus()).isEqualTo(StatusPedido.EM_ROTA);
            verify(pedidoRepository, never()).save(any());
            assertThat(saida.getOut())
                    .contains("WARN")
                    .contains("plataforma=" + IFOOD_ID)
                    .contains("pedidoNaPlataforma=" + ID_NO_IFOOD)
                    .contains("statusAtual=EM_ROTA")
                    .contains("statusRecebido=CANCELADO");
        }

        @Test
        void aplicarStatusExterno_aplica_quandoOStatusEstaAFrenteDoAtual() {
            Pedido externo = pedidoExternoCom(StatusPedido.EM_ABERTO);
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.of(externo));

            ResultadoStatusExterno resultado = service.aplicarStatusExterno(IFOOD_ID, ID_NO_IFOOD, StatusPedido.ACEITO);

            assertThat(resultado).isEqualTo(ResultadoStatusExterno.APLICADO);
            assertThat(capturarSalvo().getStatus()).isEqualTo(StatusPedido.ACEITO);
        }

        @Test
        @DisplayName("o iFood nao informa todas as etapas: de ACEITO pode ir direto para EM_ROTA")
        void aplicarStatusExterno_aplica_quandoOStatusPulaEtapas() {
            Pedido externo = pedidoExternoCom(StatusPedido.ACEITO);
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.of(externo));

            ResultadoStatusExterno resultado = service.aplicarStatusExterno(IFOOD_ID, ID_NO_IFOOD, StatusPedido.EM_ROTA);

            assertThat(resultado).isEqualTo(ResultadoStatusExterno.APLICADO);
            assertThat(externo.getStatus()).isEqualTo(StatusPedido.EM_ROTA);
        }

        @Test
        @DisplayName("evento fora de ordem nao faz o pedido andar para tras")
        void aplicarStatusExterno_ignora_quandoOStatusEstaAtrasDoAtual() {
            Pedido externo = pedidoExternoCom(StatusPedido.EM_ROTA);
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.of(externo));

            ResultadoStatusExterno resultado = service.aplicarStatusExterno(IFOOD_ID, ID_NO_IFOOD, StatusPedido.ACEITO);

            assertThat(resultado).isEqualTo(ResultadoStatusExterno.IGNORADO);
            assertThat(externo.getStatus()).isEqualTo(StatusPedido.EM_ROTA);
            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("evento repetido nao regrava o mesmo status")
        void aplicarStatusExterno_ignora_quandoOStatusEOMesmo() {
            Pedido externo = pedidoExternoCom(StatusPedido.ACEITO);
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.of(externo));

            ResultadoStatusExterno resultado = service.aplicarStatusExterno(IFOOD_ID, ID_NO_IFOOD, StatusPedido.ACEITO);

            assertThat(resultado).isEqualTo(ResultadoStatusExterno.IGNORADO);
            verify(pedidoRepository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = StatusPedido.class, names = {"CONCLUIDO", "CANCELADO"})
        @DisplayName("CONCLUIDO e CANCELADO sao terminais: nem cancelamento nem conclusao os alteram")
        void aplicarStatusExterno_ignora_quandoOPedidoEstaEmStatusTerminal(StatusPedido terminal) {
            Pedido externo = pedidoExternoCom(terminal);
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.of(externo));

            assertThat(service.aplicarStatusExterno(IFOOD_ID, ID_NO_IFOOD, StatusPedido.CANCELADO))
                    .isEqualTo(ResultadoStatusExterno.IGNORADO);
            assertThat(service.aplicarStatusExterno(IFOOD_ID, ID_NO_IFOOD, StatusPedido.CONCLUIDO))
                    .isEqualTo(ResultadoStatusExterno.IGNORADO);

            assertThat(externo.getStatus()).isEqualTo(terminal);
            verify(pedidoRepository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = StatusPedido.class, names = {"EM_ABERTO", "ACEITO", "EM_PREPARO", "PRONTO", "EM_ROTA"})
        void aplicarStatusExterno_cancela_aPartirDeQualquerEtapaNaoTerminal(StatusPedido etapa) {
            Pedido externo = pedidoExternoCom(etapa);
            when(pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(IFOOD_ID, ID_NO_IFOOD))
                    .thenReturn(Optional.of(externo));

            ResultadoStatusExterno resultado = service.aplicarStatusExterno(IFOOD_ID, ID_NO_IFOOD, StatusPedido.CANCELADO);

            assertThat(resultado).isEqualTo(ResultadoStatusExterno.APLICADO);
            assertThat(capturarSalvo().getStatus()).isEqualTo(StatusPedido.CANCELADO);
        }

        @Test
        void aplicarStatusExterno_lancaRequisicaoInvalida_quandoFaltaIdentificacaoOuStatus() {
            assertThatThrownBy(() -> service.aplicarStatusExterno(null, ID_NO_IFOOD, StatusPedido.ACEITO))
                    .isInstanceOf(RequisicaoInvalidaException.class);
            assertThatThrownBy(() -> service.aplicarStatusExterno(IFOOD_ID, " ", StatusPedido.ACEITO))
                    .isInstanceOf(RequisicaoInvalidaException.class);
            assertThatThrownBy(() -> service.aplicarStatusExterno(IFOOD_ID, ID_NO_IFOOD, null))
                    .isInstanceOf(RequisicaoInvalidaException.class);

            verifyNoInteractions(pedidoRepository);
        }
    }

    @Nested
    class PedidoFechado {

        @Test
        @DisplayName("pedido dentro de um fechamento nao pode ser editado")
        void atualizarPedido_lancaConflito_quandoOPedidoJaFoiFechado() {
            Pedido fechado = pedidoFechado();
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(fechado));

            assertThatThrownBy(() -> service.atualizarPedido(1L,
                    new AtualizarPedidoRequest(null, "Outro Nome", null, null)))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("fechamento de caixa");

            assertThat(fechado.getNomeCliente()).isNotEqualTo("Outro Nome");
            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("pedido dentro de um fechamento nao pode ter o status mudado")
        void atualizarStatus_lancaConflito_quandoOPedidoJaFoiFechado() {
            Pedido fechado = pedidoFechado();
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(fechado));

            assertThatThrownBy(() -> service.atualizarStatus(1L, StatusPedido.CANCELADO))
                    .isInstanceOf(ConflitoException.class);

            assertThat(fechado.getStatus()).isEqualTo(StatusPedido.CONCLUIDO);
            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("pedido dentro de um fechamento nao pode ser excluido")
        void excluirPedido_lancaConflito_quandoOPedidoJaFoiFechado() {
            Pedido fechado = pedidoFechado();
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(fechado));

            assertThatThrownBy(() -> service.excluirPedido(1L))
                    .isInstanceOf(ConflitoException.class);

            verify(pedidoRepository, never()).delete(any());
        }

        @Test
        void excluirPedido_excluiNormalmente_quandoOPedidoNaoFoiFechado() {
            Pedido aberto = pedidoExistente(canal(1L, "Balcao", "0.00"),
                    formaDePagamento(2L, "Pix", "0.00"));
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(aberto));

            service.excluirPedido(1L);

            verify(pedidoRepository).delete(aberto);
        }

        @Test
        void registrarPedido_criaPedidoAberto() {
            CanalDeVenda balcao = canal(1L, "Balcao", "0.00");
            FormaDePagamento pix = formaDePagamento(2L, "Pix", "0.00");
            when(canalDeVendaService.buscarCanalAtivo(LOJA_ID, 1L)).thenReturn(balcao);
            when(formaDePagamentoService.buscarEntidadePorId(2L)).thenReturn(pix);
            when(itemService.buscarEntidadePorId(3L)).thenReturn(item(3L, "Cerveja", "9.00"));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.registrarPedido(new AdicionarPedidoRequest(LOJA_ID, 1L, null, 2L,
                    List.of(new ItemPedidoRequest(3L, 1))));

            assertThat(capturarSalvo().isFechado()).isFalse();
        }
    }

    @Nested
    class Status {

        @Test
        void atualizarStatus_gravaONovoStatus() {
            Pedido existente = pedidoExistente(canal(1L, "Balcao", "0.00"),
                    formaDePagamento(2L, "Pix", "0.00"));
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            PedidoResponseDTO resposta = service.atualizarStatus(1L, StatusPedido.PRONTO);

            assertThat(existente.getStatus()).isEqualTo(StatusPedido.PRONTO);
            assertThat(resposta.status()).isEqualTo(StatusPedido.PRONTO);
        }

        @Test
        void buscarPedidoPorId_lancaRecursoNaoEncontrado_quandoIdNaoExiste() {
            when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPedidoPorId(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    private Pedido capturarSalvo() {
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        return captor.getValue();
    }

    private Pedido pedidoFechado() {
        Pedido pedido = pedidoExistente(canal(1L, "iFood", "12.00"),
                formaDePagamento(2L, "Credito", "2.99"));
        pedido.setStatus(StatusPedido.CONCLUIDO);
        pedido.setFechado(true);
        return pedido;
    }

    private RegistrarPedidoExternoRequest pedidoExternoValido() {
        return new RegistrarPedidoExternoRequest(LOJA_ID, IFOOD_ID, ID_NO_IFOOD, "Maria", 2L,
                LocalDateTime.of(2026, 10, 7, 20, 15), null, false,
                List.of(new ItemPedidoExternoRequest("X-Burger", 2, new BigDecimal("9.50"))));
    }

    private Pedido pedidoExternoCom(StatusPedido status) {
        Pedido pedido = pedidoExterno();
        pedido.setStatus(status);
        return pedido;
    }

    private Pedido pedidoExterno() {
        Pedido pedido = pedidoExistente(canal(IFOOD_ID, "iFood", "12.00"),
                formaDePagamento(2L, "Online", "0.00"));
        pedido.setPlataformaPedidoId(ID_NO_IFOOD);
        pedido.setNomeCliente("Maria");
        ItemPedido itemExterno = new ItemPedido();
        itemExterno.setNomeItem("X-Burger");
        itemExterno.setQuantidade(2);
        itemExterno.setPrecoUnitario(new BigDecimal("9.50"));
        pedido.setItens(new ArrayList<>(List.of(itemExterno)));
        return pedido;
    }

    private Pedido pedidoExistente(CanalDeVenda canal, FormaDePagamento formaDePagamento) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setLoja(canal.getLoja());
        pedido.setPlataforma(canal.getPlataforma());
        pedido.setTaxaPlataformaPercentual(canal.getTaxaPercentual());
        pedido.setFormaDePagamento(formaDePagamento);
        pedido.setTaxaPagamentoPercentual(formaDePagamento.getTaxaPercentual());
        pedido.setHorarioPedido(LocalDateTime.of(2026, 10, 6, 20, 30));
        pedido.setStatus(StatusPedido.EM_ABERTO);
        pedido.setValor(new BigDecimal("19.00"));
        pedido.setItens(new ArrayList<>(List.of(itemPedido(item(3L, "Cerveja", "9.50"), 2, "9.50"))));
        return pedido;
    }

    private ItemPedido itemPedido(Item item, int quantidade, String precoUnitario) {
        ItemPedido itemPedido = new ItemPedido();
        itemPedido.setItem(item);
        itemPedido.setNomeItem(item.getNome());
        itemPedido.setQuantidade(quantidade);
        itemPedido.setPrecoUnitario(new BigDecimal(precoUnitario));
        return itemPedido;
    }

    private Item item(Long id, String nome, String preco) {
        Item item = new Item();
        item.setId(id);
        item.setNome(nome);
        item.setPreco(new BigDecimal(preco));
        return item;
    }

    private CanalDeVenda canal(Long plataformaId, String nomeDaPlataforma, String taxaPercentual) {
        Loja loja = new Loja();
        loja.setId(LOJA_ID);
        loja.setNome("Loja principal");
        Plataforma plataforma = new Plataforma();
        plataforma.setId(plataformaId);
        plataforma.setNome(nomeDaPlataforma);
        CanalDeVenda canal = new CanalDeVenda();
        canal.setLoja(loja);
        canal.setPlataforma(plataforma);
        canal.setTaxaPercentual(new BigDecimal(taxaPercentual));
        return canal;
    }

    private FormaDePagamento formaDePagamento(Long id, String nome, String taxaPercentual) {
        FormaDePagamento formaDePagamento = new FormaDePagamento();
        formaDePagamento.setId(id);
        formaDePagamento.setNome(nome);
        formaDePagamento.setTaxaPercentual(new BigDecimal(taxaPercentual));
        return formaDePagamento;
    }
}
