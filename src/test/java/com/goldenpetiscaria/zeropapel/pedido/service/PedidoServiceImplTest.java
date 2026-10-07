package com.goldenpetiscaria.zeropapel.pedido.service;

import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.formadepagamento.entity.FormaDePagamento;
import com.goldenpetiscaria.zeropapel.formadepagamento.repository.FormaDePagamentoRepository;
import com.goldenpetiscaria.zeropapel.item.entity.Item;
import com.goldenpetiscaria.zeropapel.item.repository.ItemRepository;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AdicionarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.ItemPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.PedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.entity.ItemPedido;
import com.goldenpetiscaria.zeropapel.pedido.entity.Pedido;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;
import com.goldenpetiscaria.zeropapel.pedido.repository.PedidoRepository;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import com.goldenpetiscaria.zeropapel.plataforma.repository.PlataformaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PlataformaRepository plataformaRepository;

    @Mock
    private FormaDePagamentoRepository formaDePagamentoRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private PedidoServiceImpl service;

    @Nested
    class TaxasCongeladas {

        @Test
        @DisplayName("registrar congela a taxa da plataforma e do pagamento vigentes na venda")
        void registrarPedido_congelaAsTaxasVigentes() {
            Plataforma ifood = plataforma(1L, "iFood", "12.00");
            FormaDePagamento credito = formaDePagamento(2L, "Credito", "2.99");
            Item cerveja = item(3L, "Cerveja", "9.00");
            when(plataformaRepository.findById(1L)).thenReturn(Optional.of(ifood));
            when(formaDePagamentoRepository.findById(2L)).thenReturn(Optional.of(credito));
            when(itemRepository.findById(3L)).thenReturn(Optional.of(cerveja));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.registrarPedido(new AdicionarPedidoRequest(1L, "Pedro", 2L,
                    List.of(new ItemPedidoRequest(3L, 2))));

            Pedido salvo = capturarSalvo();
            assertThat(salvo.getTaxaPlataformaPercentual()).isEqualByComparingTo("12.00");
            assertThat(salvo.getTaxaPagamentoPercentual()).isEqualByComparingTo("2.99");
        }

        @Test
        @DisplayName("taxa congelada nao muda quando a plataforma e reajustada depois")
        void registrarPedido_naoAcompanhaReajusteDaPlataforma() {
            Plataforma ifood = plataforma(1L, "iFood", "12.00");
            FormaDePagamento pix = formaDePagamento(2L, "Pix", "0.00");
            Item cerveja = item(3L, "Cerveja", "9.00");
            when(plataformaRepository.findById(1L)).thenReturn(Optional.of(ifood));
            when(formaDePagamentoRepository.findById(2L)).thenReturn(Optional.of(pix));
            when(itemRepository.findById(3L)).thenReturn(Optional.of(cerveja));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.registrarPedido(new AdicionarPedidoRequest(1L, null, 2L,
                    List.of(new ItemPedidoRequest(3L, 1))));
            Pedido salvo = capturarSalvo();

            ifood.setTaxaPercentual(new BigDecimal("14.00"));

            assertThat(salvo.getTaxaPlataformaPercentual()).isEqualByComparingTo("12.00");
        }

        @Test
        @DisplayName("trocar a plataforma de um pedido recongela a taxa com a da nova")
        void atualizarPedido_recongelaATaxa_quandoAPlataformaMuda() {
            Pedido existente = pedidoExistente(plataforma(1L, "Balcao", "0.00"),
                    formaDePagamento(2L, "Pix", "0.00"));
            Plataforma ifood = plataforma(9L, "iFood", "12.00");
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(plataformaRepository.findById(9L)).thenReturn(Optional.of(ifood));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarPedido(1L, new AtualizarPedidoRequest(9L, null, null, null));

            assertThat(existente.getTaxaPlataformaPercentual()).isEqualByComparingTo("12.00");
        }

        @Test
        void atualizarPedido_recongelaATaxa_quandoAFormaDePagamentoMuda() {
            Pedido existente = pedidoExistente(plataforma(1L, "Balcao", "0.00"),
                    formaDePagamento(2L, "Pix", "0.00"));
            FormaDePagamento credito = formaDePagamento(8L, "Credito", "2.99");
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(formaDePagamentoRepository.findById(8L)).thenReturn(Optional.of(credito));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarPedido(1L, new AtualizarPedidoRequest(null, null, 8L, null));

            assertThat(existente.getTaxaPagamentoPercentual()).isEqualByComparingTo("2.99");
        }

        @Test
        @DisplayName("mexer so no nome do cliente nao recongela taxa nenhuma")
        void atualizarPedido_preservaAsTaxas_quandoSoONomeMuda() {
            Pedido existente = pedidoExistente(plataforma(1L, "iFood", "12.00"),
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
    }

    @Nested
    class Registro {

        @Test
        void registrarPedido_travaOPrecoDoItemEComputaOValor() {
            Plataforma balcao = plataforma(1L, "Balcao", "0.00");
            FormaDePagamento pix = formaDePagamento(2L, "Pix", "0.00");
            Item cerveja = item(3L, "Cerveja", "9.50");
            when(plataformaRepository.findById(1L)).thenReturn(Optional.of(balcao));
            when(formaDePagamentoRepository.findById(2L)).thenReturn(Optional.of(pix));
            when(itemRepository.findById(3L)).thenReturn(Optional.of(cerveja));
            when(pedidoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            PedidoResponseDTO resposta = service.registrarPedido(new AdicionarPedidoRequest(
                    1L, "Pedro", 2L, List.of(new ItemPedidoRequest(3L, 3))));

            Pedido salvo = capturarSalvo();
            assertThat(salvo.getItens()).hasSize(1);
            assertThat(salvo.getItens().getFirst().getPrecoUnitario()).isEqualByComparingTo("9.50");
            assertThat(salvo.getValor()).isEqualByComparingTo("28.50");
            assertThat(salvo.getStatus()).isEqualTo(StatusPedido.EM_ABERTO);
            assertThat(resposta.valor()).isEqualByComparingTo("28.50");
        }

        @Test
        void registrarPedido_lancaRecursoNaoEncontrado_quandoPlataformaNaoExiste() {
            when(plataformaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrarPedido(new AdicionarPedidoRequest(
                    99L, null, 2L, List.of(new ItemPedidoRequest(3L, 1)))))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        void registrarPedido_lancaRecursoNaoEncontrado_quandoItemNaoExiste() {
            when(plataformaRepository.findById(1L)).thenReturn(Optional.of(plataforma(1L, "Balcao", "0.00")));
            when(formaDePagamentoRepository.findById(2L)).thenReturn(Optional.of(formaDePagamento(2L, "Pix", "0.00")));
            when(itemRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrarPedido(new AdicionarPedidoRequest(
                    1L, null, 2L, List.of(new ItemPedidoRequest(99L, 1)))))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            verify(pedidoRepository, never()).save(any());
        }
    }

    @Nested
    class Status {

        @Test
        void atualizarStatus_gravaONovoStatus() {
            Pedido existente = pedidoExistente(plataforma(1L, "Balcao", "0.00"),
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

    private Pedido pedidoExistente(Plataforma plataforma, FormaDePagamento formaDePagamento) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setPlataforma(plataforma);
        pedido.setTaxaPlataformaPercentual(plataforma.getTaxaPercentual());
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

    private Plataforma plataforma(Long id, String nome, String taxaPercentual) {
        Plataforma plataforma = new Plataforma();
        plataforma.setId(id);
        plataforma.setNome(nome);
        plataforma.setTaxaPercentual(new BigDecimal(taxaPercentual));
        return plataforma;
    }

    private FormaDePagamento formaDePagamento(Long id, String nome, String taxaPercentual) {
        FormaDePagamento formaDePagamento = new FormaDePagamento();
        formaDePagamento.setId(id);
        formaDePagamento.setNome(nome);
        formaDePagamento.setTaxaPercentual(new BigDecimal(taxaPercentual));
        return formaDePagamento;
    }
}
