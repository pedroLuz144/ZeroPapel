package com.goldenpetiscaria.zeropapel.fechamentodecaixa.service;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.request.RealizarFechamentoRequest;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response.FechamentoCaixaListagemDTO;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response.FechamentoResponseDTO;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.entity.FechamentoCaixa;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.repository.FechamentoCaixaRepository;
import com.goldenpetiscaria.zeropapel.formadepagamento.entity.FormaDePagamento;
import com.goldenpetiscaria.zeropapel.item.entity.Item;
import com.goldenpetiscaria.zeropapel.pedido.entity.ItemPedido;
import com.goldenpetiscaria.zeropapel.pedido.entity.Pedido;
import com.goldenpetiscaria.zeropapel.pedido.repository.PedidoRepository;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;
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
class FechamentoServiceImplTest {

    private static final LocalDateTime DE = LocalDateTime.of(2026, 10, 6, 18, 0);
    private static final LocalDateTime ATE = LocalDateTime.of(2026, 10, 7, 2, 0);

    @Mock
    private FechamentoCaixaRepository fechamentoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private FechamentoServiceImpl service;

    @Nested
    class Dinheiro {

        @Test
        @DisplayName("taxa da plataforma arredonda para cima no empate de meio centavo")
        void calcularPrevia_arredondaTaxaDaPlataformaParaCima_quandoCaiEmMeioCentavo() {
            Plataforma plataforma = plataforma("iFood", "15.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            darPedidos(pedido("4.10", plataforma, dinheiro, 19));

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.resumo().totalTaxas()).isEqualByComparingTo("0.62");
            assertThat(previa.resumo().faturamentoLiquido()).isEqualByComparingTo("3.48");
        }

        @Test
        @DisplayName("taxa da forma de pagamento arredonda para cima no empate de meio centavo")
        void calcularPrevia_arredondaTaxaDoPagamentoParaCima_quandoCaiEmMeioCentavo() {
            Plataforma balcao = plataforma("Balcao", "0.00");
            FormaDePagamento credito = formaDePagamento("Credito", "22.50");
            darPedidos(pedido("8.20", balcao, credito, 20));

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.resumo().totalTaxas()).isEqualByComparingTo("1.85");
            assertThat(previa.resumo().faturamentoLiquido()).isEqualByComparingTo("6.35");
        }

        @Test
        @DisplayName("ticket medio arredonda para cima no empate de meio centavo")
        void calcularPrevia_arredondaTicketMedioParaCima_quandoCaiEmMeioCentavo() {
            Plataforma balcao = plataforma("Balcao", "0.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            darPedidos(
                    pedido("16.00", balcao, dinheiro, 19),
                    pedido("16.50", balcao, dinheiro, 19),
                    pedido("17.00", balcao, dinheiro, 20),
                    pedido("16.85", balcao, dinheiro, 20),
                    pedido("17.00", balcao, dinheiro, 21),
                    pedido("17.00", balcao, dinheiro, 21)
            );

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.resumo().faturamentoBruto()).isEqualByComparingTo("100.35");
            assertThat(previa.resumo().totalPedidos()).isEqualTo(6);
            assertThat(previa.resumo().ticketMedio()).isEqualByComparingTo("16.73");
        }

        @Test
        void calcularPrevia_retornaTicketMedioZero_quandoNaoHaPedido() {
            darPedidos();

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.resumo().totalPedidos()).isZero();
            assertThat(previa.resumo().faturamentoBruto()).isEqualByComparingTo("0");
            assertThat(previa.resumo().ticketMedio()).isEqualByComparingTo("0");
            assertThat(previa.itensMaisVendidos()).isEmpty();
        }

        @Test
        @DisplayName("mudar a taxa da plataforma depois da venda nao mexe no fechamento")
        void calcularPrevia_usaATaxaCongelada_quandoATaxaDaPlataformaMudaDepoisDaVenda() {
            Plataforma ifood = plataforma("iFood", "10.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            Pedido pedido = pedido("200.00", ifood, dinheiro, 20);
            ifood.setTaxaPercentual(new BigDecimal("14.00"));
            darPedidos(pedido);

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.resumo().totalTaxas()).isEqualByComparingTo("20.00");
            assertThat(previa.porPlataforma().getFirst().taxaPlataforma()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("mudar a taxa do cartao depois da venda nao mexe no fechamento")
        void calcularPrevia_usaATaxaCongelada_quandoATaxaDoPagamentoMudaDepoisDaVenda() {
            Plataforma balcao = plataforma("Balcao", "0.00");
            FormaDePagamento credito = formaDePagamento("Credito", "3.00");
            Pedido pedido = pedido("100.00", balcao, credito, 20);
            credito.setTaxaPercentual(new BigDecimal("4.50"));
            darPedidos(pedido);

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.resumo().totalTaxas()).isEqualByComparingTo("3.00");
            assertThat(previa.porFormaDePagamento().getFirst().taxa()).isEqualByComparingTo("3.00");
        }

        @Test
        void calcularPrevia_descontaAsDuasTaxasDoFaturamentoLiquido() {
            Plataforma ifood = plataforma("iFood", "12.00");
            FormaDePagamento credito = formaDePagamento("Credito", "3.00");
            darPedidos(pedido("100.00", ifood, credito, 20));

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.resumo().faturamentoBruto()).isEqualByComparingTo("100.00");
            assertThat(previa.resumo().totalTaxas()).isEqualByComparingTo("15.00");
            assertThat(previa.resumo().faturamentoLiquido()).isEqualByComparingTo("85.00");
        }
    }

    @Nested
    class RankingDeItens {

        @Test
        @DisplayName("receita do item sai com duas casas, sem ruido de ponto flutuante")
        void calcularPrevia_retornaReceitaComDuasCasas_quandoPrecoGeraDizimaEmBinario() {
            Plataforma balcao = plataforma("Balcao", "0.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            Item batata = item(1L, "Batata Frita", "41.99");
            Pedido pedido = pedido("209.95", balcao, dinheiro, 20);
            pedido.setItens(List.of(itemPedido(batata, 5, "41.99")));
            darPedidos(pedido);

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.itensMaisVendidos()).hasSize(1);
            assertThat(previa.itensMaisVendidos().getFirst().receitaTotal().toPlainString())
                    .isEqualTo("209.95");
        }

        @Test
        void calcularPrevia_agregaOMesmoItemDePedidosDiferentes() {
            Plataforma balcao = plataforma("Balcao", "0.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            Item cerveja = item(2L, "Cerveja", "9.00");

            Pedido primeiro = pedido("18.00", balcao, dinheiro, 19);
            primeiro.setItens(List.of(itemPedido(cerveja, 2, "9.00")));
            Pedido segundo = pedido("27.00", balcao, dinheiro, 20);
            segundo.setItens(List.of(itemPedido(cerveja, 3, "9.00")));
            darPedidos(primeiro, segundo);

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.itensMaisVendidos()).hasSize(1);
            assertThat(previa.itensMaisVendidos().getFirst().quantidadeTotal()).isEqualTo(5);
            assertThat(previa.itensMaisVendidos().getFirst().receitaTotal()).isEqualByComparingTo("45.00");
        }

        @Test
        void calcularPrevia_ordenaRankingPelaQuantidadeDecrescente() {
            Plataforma balcao = plataforma("Balcao", "0.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            Item porcao = item(1L, "Porcao", "30.00");
            Item refrigerante = item(2L, "Refrigerante", "7.00");

            Pedido pedido = pedido("79.00", balcao, dinheiro, 20);
            pedido.setItens(List.of(itemPedido(porcao, 1, "30.00"), itemPedido(refrigerante, 7, "7.00")));
            darPedidos(pedido);

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.itensMaisVendidos()).extracting(dto -> dto.itemNome())
                    .containsExactly("Refrigerante", "Porcao");
        }
    }

    @Nested
    class Agrupamentos {

        @Test
        void calcularPrevia_agrupaPorPlataformaOrdenandoPeloBrutoDecrescente() {
            Plataforma balcao = plataforma("Balcao", "0.00");
            Plataforma ifood = plataforma("iFood", "10.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            darPedidos(
                    pedido("50.00", balcao, dinheiro, 19),
                    pedido("200.00", ifood, dinheiro, 20)
            );

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.porPlataforma()).extracting(dto -> dto.plataforma())
                    .containsExactly("iFood", "Balcao");
            assertThat(previa.porPlataforma().getFirst().taxaPlataforma()).isEqualByComparingTo("20.00");
            assertThat(previa.porPlataforma().getFirst().liquido()).isEqualByComparingTo("180.00");
        }

        @Test
        void calcularPrevia_agrupaPorFormaDePagamento() {
            Plataforma balcao = plataforma("Balcao", "0.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            FormaDePagamento credito = formaDePagamento("Credito", "3.00");
            darPedidos(
                    pedido("100.00", balcao, credito, 20),
                    pedido("40.00", balcao, dinheiro, 20)
            );

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.porFormaDePagamento()).extracting(dto -> dto.formaDePagamento())
                    .containsExactly("Credito", "Dinheiro");
            assertThat(previa.porFormaDePagamento().getFirst().taxa()).isEqualByComparingTo("3.00");
        }

        @Test
        void calcularPrevia_agrupaCurvaHorariaOrdenandoPelaHoraCrescente() {
            Plataforma balcao = plataforma("Balcao", "0.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            darPedidos(
                    pedido("10.00", balcao, dinheiro, 21),
                    pedido("20.00", balcao, dinheiro, 19),
                    pedido("30.00", balcao, dinheiro, 21)
            );

            FechamentoResponseDTO previa = service.calcularPrevia(DE, ATE);

            assertThat(previa.pedidosPorHora()).extracting(dto -> dto.hora()).containsExactly(19, 21);
            assertThat(previa.pedidosPorHora().getLast().qtdPedidos()).isEqualTo(2);
            assertThat(previa.pedidosPorHora().getLast().faturamentoBruto()).isEqualByComparingTo("40.00");
        }
    }

    @Nested
    class ConsultaDePedidos {

        @Test
        @DisplayName("consolidado le somente os pedidos faturaveis, nunca os cancelados")
        void calcularPrevia_consultaSomenteOsPedidosFaturaveis() {
            darPedidos();

            service.calcularPrevia(DE, ATE);

            verify(pedidoRepository).findFaturaveisDoPeriodoComItens(DE, ATE);
            verify(pedidoRepository, never()).findAll();
        }
    }

    @Nested
    class RealizarFechamento {

        @Test
        void realizarFechamento_lancaConflito_quandoPeriodoJaPossuiFechamento() {
            when(fechamentoRepository.countByPeriodoSobreposto(DE, ATE)).thenReturn(1L);

            assertThatThrownBy(() -> service.realizarFechamento(new RealizarFechamentoRequest(DE, ATE), gerente()))
                    .isInstanceOf(ConflitoException.class);

            verify(fechamentoRepository, never()).save(any());
        }

        @Test
        void realizarFechamento_persisteOsTotaisCalculados() {
            Plataforma ifood = plataforma("iFood", "10.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            when(fechamentoRepository.countByPeriodoSobreposto(DE, ATE)).thenReturn(0L);
            darPedidos(pedido("200.00", ifood, dinheiro, 20));
            when(fechamentoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            Usuario gerente = gerente();
            service.realizarFechamento(new RealizarFechamentoRequest(DE, ATE), gerente);

            ArgumentCaptor<FechamentoCaixa> captor = ArgumentCaptor.forClass(FechamentoCaixa.class);
            verify(fechamentoRepository).save(captor.capture());
            FechamentoCaixa salvo = captor.getValue();

            assertThat(salvo.getDe()).isEqualTo(DE);
            assertThat(salvo.getAte()).isEqualTo(ATE);
            assertThat(salvo.getGeradoPor()).isSameAs(gerente);
            assertThat(salvo.getTotalPedidos()).isEqualTo(1);
            assertThat(salvo.getFaturamentoBruto()).isEqualByComparingTo("200.00");
            assertThat(salvo.getTotalTaxas()).isEqualByComparingTo("20.00");
            assertThat(salvo.getFaturamentoLiquido()).isEqualByComparingTo("180.00");
            assertThat(salvo.getTicketMedio()).isEqualByComparingTo("200.00");
        }
    }

    @Nested
    class CongelamentoDoPeriodo {

        @Test
        @DisplayName("fechar o caixa marca os pedidos do periodo como fechados")
        void realizarFechamento_marcaOsPedidosDoPeriodo() {
            Plataforma balcao = plataforma("Balcao", "0.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            when(fechamentoRepository.countByPeriodoSobreposto(DE, ATE)).thenReturn(0L);
            darPedidos(pedido("50.00", balcao, dinheiro, 20));
            when(fechamentoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.realizarFechamento(new RealizarFechamentoRequest(DE, ATE), gerente());

            verify(pedidoRepository).marcarComoFechados(DE, ATE);
        }

        @Test
        @DisplayName("fechamento recusado por sobreposicao nao marca pedido nenhum")
        void realizarFechamento_naoMarcaNada_quandoPeriodoJaTemFechamento() {
            when(fechamentoRepository.countByPeriodoSobreposto(DE, ATE)).thenReturn(1L);

            assertThatThrownBy(() -> service.realizarFechamento(new RealizarFechamentoRequest(DE, ATE), gerente()))
                    .isInstanceOf(ConflitoException.class);

            verify(pedidoRepository, never()).marcarComoFechados(any(), any());
        }

        @Test
        void calcularPrevia_naoMarcaPedido_porqueNaoPersisteNada() {
            darPedidos();

            service.calcularPrevia(DE, ATE);

            verify(pedidoRepository, never()).marcarComoFechados(any(), any());
        }
    }

    @Nested
    class BuscarFechamento {

        @Test
        void buscarFechamentoPorId_lancaRecursoNaoEncontrado_quandoIdNaoExiste() {
            when(fechamentoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarFechamentoPorId(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @Test
        @DisplayName("resumo vem congelado do banco, nao recalculado dos pedidos")
        void buscarFechamentoPorId_usaOResumoCongelado_emVezDeRecalcular() {
            FechamentoCaixa congelado = fechamentoCongelado();
            when(fechamentoRepository.findById(1L)).thenReturn(Optional.of(congelado));
            Plataforma balcao = plataforma("Balcao", "0.00");
            FormaDePagamento dinheiro = formaDePagamento("Dinheiro", "0.00");
            darPedidos(pedido("10.00", balcao, dinheiro, 20));

            FechamentoResponseDTO resposta = service.buscarFechamentoPorId(1L);

            assertThat(resposta.resumo().faturamentoBruto()).isEqualByComparingTo("999.00");
            assertThat(resposta.resumo().totalPedidos()).isEqualTo(42);
        }
    }

    @Nested
    class ListarFechamentos {

        @Test
        void listarFechamentos_ordenaDoMaisRecenteParaOMaisAntigo() {
            FechamentoCaixa antigo = fechamentoCongelado();
            antigo.setId(1L);
            antigo.setGeradoEm(LocalDateTime.of(2026, 10, 1, 3, 0));
            FechamentoCaixa recente = fechamentoCongelado();
            recente.setId(2L);
            recente.setGeradoEm(LocalDateTime.of(2026, 10, 5, 3, 0));
            when(fechamentoRepository.findAll()).thenReturn(List.of(antigo, recente));

            List<FechamentoCaixaListagemDTO> listagem = service.listarFechamentos();

            assertThat(listagem).extracting(dto -> dto.id()).containsExactly(2L, 1L);
        }
    }

    private void darPedidos(Pedido... pedidos) {
        when(pedidoRepository.findFaturaveisDoPeriodoComItens(DE, ATE)).thenReturn(List.of(pedidos));
    }

    private Pedido pedido(String valor, Plataforma plataforma, FormaDePagamento formaDePagamento, int hora) {
        Pedido pedido = new Pedido();
        pedido.setValor(new BigDecimal(valor));
        pedido.setPlataforma(plataforma);
        pedido.setTaxaPlataformaPercentual(plataforma.getTaxaPercentual());
        pedido.setFormaDePagamento(formaDePagamento);
        pedido.setTaxaPagamentoPercentual(formaDePagamento.getTaxaPercentual());
        pedido.setHorarioPedido(LocalDateTime.of(2026, 10, 6, hora, 30));
        pedido.setItens(new ArrayList<>());
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

    private Plataforma plataforma(String nome, String taxaPercentual) {
        Plataforma plataforma = new Plataforma();
        plataforma.setNome(nome);
        plataforma.setTaxaPercentual(new BigDecimal(taxaPercentual));
        return plataforma;
    }

    private FormaDePagamento formaDePagamento(String nome, String taxaPercentual) {
        FormaDePagamento formaDePagamento = new FormaDePagamento();
        formaDePagamento.setNome(nome);
        formaDePagamento.setTaxaPercentual(new BigDecimal(taxaPercentual));
        return formaDePagamento;
    }

    private Usuario gerente() {
        return new Usuario("Eliane", "eliane", "hash", Cargo.GERENTE);
    }

    private FechamentoCaixa fechamentoCongelado() {
        FechamentoCaixa fechamento = new FechamentoCaixa();
        fechamento.setId(1L);
        fechamento.setDe(DE);
        fechamento.setAte(ATE);
        fechamento.setGeradoEm(LocalDateTime.of(2026, 10, 7, 3, 0));
        fechamento.setGeradoPor(gerente());
        fechamento.setTotalPedidos(42);
        fechamento.setFaturamentoBruto(new BigDecimal("999.00"));
        fechamento.setTotalTaxas(new BigDecimal("99.90"));
        fechamento.setFaturamentoLiquido(new BigDecimal("899.10"));
        fechamento.setTicketMedio(new BigDecimal("23.79"));
        return fechamento;
    }
}
