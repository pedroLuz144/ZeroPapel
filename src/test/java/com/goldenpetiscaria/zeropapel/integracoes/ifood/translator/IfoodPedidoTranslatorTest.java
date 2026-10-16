package com.goldenpetiscaria.zeropapel.integracoes.ifood.translator;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.PedidoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.ItemPedidoExternoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.RegistrarPedidoExternoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IfoodPedidoTranslatorTest {

    private static final Long LOJA_ID = 3L;
    private static final Long PLATAFORMA_ID = 2L;
    private static final Long FORMA_DE_PAGAMENTO_ID = 7L;

    private final IfoodPedidoTranslator translator = new IfoodPedidoTranslator(ZoneId.of("America/Sao_Paulo"));

    @Test
    void traduzir_montaORequestComLojaPlataformaFormaDePagamentoEIdentidadeExterna() {
        PedidoIfoodResponse pedido = pedido(" pedido-1 ", "2026-10-10T22:15:30Z", "  Maria  ",
                item("Porção de fritas", "2", "25.50", "0", "51.00"));

        RegistrarPedidoExternoRequest request = traduzir(pedido);

        assertThat(request.lojaId()).isEqualTo(LOJA_ID);
        assertThat(request.plataformaId()).isEqualTo(PLATAFORMA_ID);
        assertThat(request.formaDePagamentoId()).isEqualTo(FORMA_DE_PAGAMENTO_ID);
        assertThat(request.plataformaPedidoId()).isEqualTo("pedido-1");
        assertThat(request.nomeCliente()).isEqualTo("Maria");
        assertThat(request.itens()).hasSize(1);
    }

    @Test
    @DisplayName("o horario em UTC do iFood vira o horario local da loja")
    void traduzir_converteOHorarioParaOFusoDaLoja() {
        PedidoIfoodResponse pedido = pedido("pedido-1", "2026-10-11T01:15:30Z", "Maria",
                item("Fritas", "1", "10.00", "0", "10.00"));

        assertThat(traduzir(pedido).horarioPedido()).isEqualTo(LocalDateTime.of(2026, 10, 10, 22, 15, 30));
    }

    @Test
    void traduzir_deixaHorarioEClienteNulos_quandoOIfoodNaoInforma() {
        PedidoIfoodResponse pedido = new PedidoIfoodResponse("pedido-1", null, null,
                List.of(item("Fritas", "1", "10.00", "0", "10.00")), null, null);

        RegistrarPedidoExternoRequest request = traduzir(pedido);

        assertThat(request.horarioPedido()).isNull();
        assertThat(request.nomeCliente()).isNull();
    }

    @Test
    void traduzir_usaOPrecoUnitario_quandoOItemNaoTemOpcional() {
        ItemPedidoExternoRequest item = unicoItem(item("Fritas", "3", "12.90", "0", "38.70"));

        assertThat(item.nomeItem()).isEqualTo("Fritas");
        assertThat(item.quantidade()).isEqualTo(3);
        assertThat(item.precoUnitario()).isEqualByComparingTo("12.90");
    }

    @Test
    @DisplayName("o opcional entra no preco unitario para o valor do pedido fechar com o total do item")
    void traduzir_embuteOsOpcionaisNoPrecoUnitario() {
        ItemPedidoExternoRequest item = unicoItem(item("Hambúrguer", "2", "30.00", "8.00", "68.00"));

        assertThat(item.quantidade()).isEqualTo(2);
        assertThat(item.precoUnitario()).isEqualByComparingTo("34.00");
    }

    @Test
    @DisplayName("total que nao divide exato pela quantidade vira uma linha so, para nao perder centavo")
    void traduzir_gravaALinhaComQuantidadeUm_quandoOTotalNaoDivideExato() {
        ItemPedidoExternoRequest item = unicoItem(item("Combo", "3", "10.00", "0.01", "30.01"));

        assertThat(item.quantidade()).isEqualTo(1);
        assertThat(item.precoUnitario()).isEqualByComparingTo("30.01");
    }

    @Test
    void traduzir_gravaALinhaComQuantidadeUm_quandoAQuantidadeEFracionada() {
        ItemPedidoExternoRequest item = unicoItem(item("Picanha no quilo", "0.350", "100.00", "0", "35.00"));

        assertThat(item.quantidade()).isEqualTo(1);
        assertThat(item.precoUnitario()).isEqualByComparingTo("35.00");
    }

    @Test
    void traduzir_calculaOTotalDaLinha_quandoOIfoodNaoMandaTotalPrice() {
        ItemPedidoExternoRequest item = unicoItem(item("Hambúrguer", "2", "30.00", "8.00", null));

        assertThat(item.quantidade()).isEqualTo(2);
        assertThat(item.precoUnitario()).isEqualByComparingTo("34.00");
    }

    @Test
    void traduzir_aceitaQuantidadeComCasasDecimaisZeradas() {
        ItemPedidoExternoRequest item = unicoItem(item("Fritas", "2.00", "10.00", "0", "20.00"));

        assertThat(item.quantidade()).isEqualTo(2);
        assertThat(item.precoUnitario()).isEqualByComparingTo("10.00");
    }

    @Test
    void traduzir_limitaONomeDoItemEUsaNomePadrao_quandoAusente() {
        String nomeLongo = "x".repeat(300);

        assertThat(unicoItem(item(nomeLongo, "1", "10.00", "0", "10.00")).nomeItem()).hasSize(255);
        assertThat(unicoItem(item("  ", "1", "10.00", "0", "10.00")).nomeItem()).isEqualTo("Item sem nome");
    }

    @Test
    void traduzir_ignoraItemNulo() {
        List<PedidoIfoodResponse.Item> itens = new ArrayList<>();
        itens.add(null);
        itens.add(item("Fritas", "1", "10.00", "0", "10.00"));

        assertThat(traduzir(new PedidoIfoodResponse("pedido-1", null, null, itens, null, null)).itens()).hasSize(1);
    }

    @Test
    void traduzir_lancaIntegracaoIfood_quandoOPedidoNaoTemIdOuItens() {
        assertThatThrownBy(() -> traduzir(new PedidoIfoodResponse(" ", null, null,
                List.of(item("Fritas", "1", "10.00", "0", "10.00")), null, null)))
                .isInstanceOf(IntegracaoIfoodException.class);
        assertThatThrownBy(() -> traduzir(new PedidoIfoodResponse("pedido-1", null, null, List.of(), null, null)))
                .isInstanceOf(IntegracaoIfoodException.class);
        assertThatThrownBy(() -> traduzir(new PedidoIfoodResponse("pedido-1", null, null, null, null, null)))
                .isInstanceOf(IntegracaoIfoodException.class);
    }

    @Test
    void traduzir_lancaIntegracaoIfood_quandoOItemNaoTemPreco() {
        assertThatThrownBy(() -> unicoItem(item("Fritas", "1", null, null, null)))
                .isInstanceOf(IntegracaoIfoodException.class);
    }

    @Test
    void traduzir_repassaATaxaDeEntregaEQuemEntregou() {
        RegistrarPedidoExternoRequest pelaLoja = traduzir(comEntrega("MERCHANT", "5"));
        RegistrarPedidoExternoRequest peloIfood = traduzir(comEntrega("IFOOD", "7.9"));

        assertThat(pelaLoja.taxaEntrega()).isEqualByComparingTo("5.00");
        assertThat(pelaLoja.entregaPelaPlataforma()).isFalse();
        assertThat(peloIfood.taxaEntrega()).isEqualByComparingTo("7.90");
        assertThat(peloIfood.entregaPelaPlataforma()).isTrue();
    }

    @Test
    void traduzir_trataComoEntregaDaPlataforma_quandoOIfoodNaoDizQuemEntregou() {
        RegistrarPedidoExternoRequest semDelivery = traduzir(new PedidoIfoodResponse("pedido-1", null, null,
                List.of(item("Fritas", "1", "10.00", "0", "10.00")), null,
                new PedidoIfoodResponse.Total(new BigDecimal("6.00"))));

        assertThat(semDelivery.entregaPelaPlataforma()).isTrue();
        assertThat(semDelivery.taxaEntrega()).isEqualByComparingTo("6.00");
    }

    @Test
    void traduzir_usaTaxaDeEntregaZero_quandoOIfoodNaoInformaOTotal() {
        RegistrarPedidoExternoRequest request = traduzir(new PedidoIfoodResponse("pedido-1", null, null,
                List.of(item("Fritas", "1", "10.00", "0", "10.00")), new PedidoIfoodResponse.Entrega("MERCHANT"),
                null));

        assertThat(request.taxaEntrega()).isEqualByComparingTo("0");
    }

    private PedidoIfoodResponse comEntrega(String entreguePor, String taxa) {
        return new PedidoIfoodResponse("pedido-1", null, null, List.of(item("Fritas", "1", "10.00", "0", "10.00")),
                new PedidoIfoodResponse.Entrega(entreguePor), new PedidoIfoodResponse.Total(new BigDecimal(taxa)));
    }

    private RegistrarPedidoExternoRequest traduzir(PedidoIfoodResponse pedido) {
        return translator.traduzir(pedido, LOJA_ID, PLATAFORMA_ID, FORMA_DE_PAGAMENTO_ID);
    }

    private ItemPedidoExternoRequest unicoItem(PedidoIfoodResponse.Item item) {
        return traduzir(new PedidoIfoodResponse("pedido-1", null, null, List.of(item), null, null)).itens().get(0);
    }

    private PedidoIfoodResponse pedido(String id, String criadoEm, String cliente, PedidoIfoodResponse.Item item) {
        return new PedidoIfoodResponse(id, OffsetDateTime.parse(criadoEm),
                new PedidoIfoodResponse.Cliente(cliente), List.of(item), null, null);
    }

    private PedidoIfoodResponse.Item item(String nome, String quantidade, String precoUnitario, String opcionais,
                                          String total) {
        return new PedidoIfoodResponse.Item(nome, decimal(quantidade), decimal(precoUnitario), decimal(opcionais),
                decimal(total));
    }

    private BigDecimal decimal(String valor) {
        return valor != null ? new BigDecimal(valor) : null;
    }
}
