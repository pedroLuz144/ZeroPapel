package com.goldenpetiscaria.zeropapel.integracoes.ifood.translator;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.PedidoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.ItemPedidoExternoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.RegistrarPedidoExternoRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class IfoodPedidoTranslator {

    private static final int TAMANHO_MAXIMO_DO_NOME = 255;
    private static final String NOME_DE_ITEM_AUSENTE = "Item sem nome";
    private static final String ENTREGA_PELA_LOJA = "MERCHANT";

    private final ZoneId fusoDaLoja;

    public IfoodPedidoTranslator(ZoneId fusoDaLoja) {
        this.fusoDaLoja = fusoDaLoja;
    }

    public RegistrarPedidoExternoRequest traduzir(PedidoIfoodResponse pedido, Long lojaId, Long plataformaId,
                                                  Long formaDePagamentoId) {
        if (pedido.id() == null || pedido.id().isBlank()) {
            throw new IntegracaoIfoodException("O iFood devolveu o pedido sem identificador");
        }
        if (pedido.items() == null || pedido.items().isEmpty()) {
            throw new IntegracaoIfoodException("O iFood devolveu o pedido sem itens");
        }
        List<ItemPedidoExternoRequest> itens = new ArrayList<>();
        for (PedidoIfoodResponse.Item item : pedido.items()) {
            if (item != null) {
                itens.add(traduzirItem(item));
            }
        }
        return new RegistrarPedidoExternoRequest(
                lojaId,
                plataformaId,
                pedido.id().trim(),
                nomeDoCliente(pedido),
                formaDePagamentoId,
                horarioDoPedido(pedido),
                taxaDeEntrega(pedido),
                entregaPelaPlataforma(pedido),
                itens);
    }

    private ItemPedidoExternoRequest traduzirItem(PedidoIfoodResponse.Item item) {
        BigDecimal totalDaLinha = totalDaLinha(item);
        BigDecimal quantidade = item.quantity();
        if (ehQuantidadeInteiraPositiva(quantidade)) {
            BigDecimal precoUnitario = totalDaLinha.divide(quantidade, 2, RoundingMode.HALF_UP);
            if (precoUnitario.multiply(quantidade).compareTo(totalDaLinha) == 0) {
                return new ItemPedidoExternoRequest(nomeDoItem(item), quantidade.intValueExact(), precoUnitario);
            }
        }
        return new ItemPedidoExternoRequest(nomeDoItem(item), 1, totalDaLinha.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal totalDaLinha(PedidoIfoodResponse.Item item) {
        if (item.totalPrice() != null) {
            return item.totalPrice();
        }
        if (item.unitPrice() == null || item.quantity() == null) {
            throw new IntegracaoIfoodException("O iFood devolveu item de pedido sem preço");
        }
        BigDecimal opcionais = item.optionsPrice() != null ? item.optionsPrice() : BigDecimal.ZERO;
        return item.unitPrice().multiply(item.quantity()).add(opcionais);
    }

    private boolean ehQuantidadeInteiraPositiva(BigDecimal quantidade) {
        return quantidade != null
                && quantidade.signum() > 0
                && quantidade.stripTrailingZeros().scale() <= 0
                && quantidade.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0;
    }

    private String nomeDoItem(PedidoIfoodResponse.Item item) {
        if (item.name() == null || item.name().isBlank()) {
            return NOME_DE_ITEM_AUSENTE;
        }
        return limitar(item.name().trim());
    }

    private String nomeDoCliente(PedidoIfoodResponse pedido) {
        if (pedido.customer() == null || pedido.customer().name() == null || pedido.customer().name().isBlank()) {
            return null;
        }
        return limitar(pedido.customer().name().trim());
    }

    private LocalDateTime horarioDoPedido(PedidoIfoodResponse pedido) {
        if (pedido.createdAt() == null) {
            return null;
        }
        return pedido.createdAt().atZoneSameInstant(fusoDaLoja).toLocalDateTime();
    }

    private BigDecimal taxaDeEntrega(PedidoIfoodResponse pedido) {
        if (pedido.total() == null || pedido.total().deliveryFee() == null) {
            return BigDecimal.ZERO;
        }
        return pedido.total().deliveryFee().setScale(2, RoundingMode.HALF_UP);
    }

    private boolean entregaPelaPlataforma(PedidoIfoodResponse pedido) {
        return pedido.delivery() == null || !ENTREGA_PELA_LOJA.equalsIgnoreCase(pedido.delivery().deliveredBy());
    }

    private String limitar(String texto) {
        return texto.length() <= TAMANHO_MAXIMO_DO_NOME ? texto : texto.substring(0, TAMANHO_MAXIMO_DO_NOME);
    }
}
