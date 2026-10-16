package com.goldenpetiscaria.zeropapel.integracoes.ifood.enumerator;

import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EventoPedidoIfoodTest {

    @ParameterizedTest
    @CsvSource({
            "PLC, EM_ABERTO",
            "CFM, ACEITO",
            "DSP, EM_ROTA",
            "CON, CONCLUIDO",
            "CAN, CANCELADO"
    })
    void paraStatusPedido_traduzOCodigoCurto(String codigo, StatusPedido esperado) {
        assertThat(EventoPedidoIfood.paraStatusPedido(codigo)).contains(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "PLACED, EM_ABERTO",
            "CONFIRMED, ACEITO",
            "DISPATCHED, EM_ROTA",
            "CONCLUDED, CONCLUIDO",
            "CANCELLED, CANCELADO"
    })
    void paraStatusPedido_traduzOCodigoPorExtenso(String codigo, StatusPedido esperado) {
        assertThat(EventoPedidoIfood.paraStatusPedido(codigo)).contains(esperado);
    }

    @Test
    void paraStatusPedido_ignoraCaixaEEspacos() {
        assertThat(EventoPedidoIfood.paraStatusPedido(" cfm ")).contains(StatusPedido.ACEITO);
        assertThat(EventoPedidoIfood.paraStatusPedido("cancelled")).contains(StatusPedido.CANCELADO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"XYZ", "", "  ", "RTP", "READY_TO_PICKUP", "PREPARATION_STARTED",
            "SEPARATION_STARTED", "SEPARATION_ENDED", "CANCELLATION_REQUEST_FAILED"})
    @DisplayName("codigo desconhecido ou sem equivalente decidido devolve vazio, nunca lanca")
    void paraStatusPedido_devolveVazio_quandoOCodigoNaoTemEquivalente(String codigo) {
        assertThat(EventoPedidoIfood.paraStatusPedido(codigo)).isEmpty();
    }

    @Test
    void paraStatusPedido_devolveVazio_quandoOCodigoENulo() {
        assertThat(EventoPedidoIfood.paraStatusPedido(null)).isEmpty();
    }
}
