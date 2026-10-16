package com.goldenpetiscaria.zeropapel.integracoes.ifood.client;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.EventoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IfoodEventoClientTest {

    private static final String URL_DO_TOKEN = "https://ifood.teste/authentication/v1.0/oauth/token";
    private static final String URL_DO_POLLING = "https://ifood.teste/events/v1.0/events:polling";
    private static final String URL_DA_CONFIRMACAO = "https://ifood.teste/events/v1.0/events/acknowledgment";

    private MockRestServiceServer servidor;
    private RestClient restClient;

    @BeforeEach
    void montarServidor() {
        RestClient.Builder construtor = RestClient.builder().baseUrl("https://ifood.teste");
        servidor = MockRestServiceServer.bindTo(construtor).build();
        restClient = construtor.build();
    }

    @Test
    void buscarEventos_enviaOBearerEDesserializaCadaEvento() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DO_POLLING))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-1"))
                .andRespond(withSuccess("""
                        [
                          {"id": "evento-1", "code": "PLC", "fullCode": "PLACED", "orderId": "pedido-1",
                           "merchantId": "loja-1", "createdAt": "2026-10-10T22:15:30.123Z", "metadata": {"a": 1}},
                          {"id": "evento-2", "code": "CFM", "orderId": "pedido-1", "merchantId": "loja-1"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<EventoIfoodResponse> eventos = cliente().buscarEventos();

        assertThat(eventos).hasSize(2);
        EventoIfoodResponse primeiro = eventos.get(0);
        assertThat(primeiro.id()).isEqualTo("evento-1");
        assertThat(primeiro.code()).isEqualTo("PLC");
        assertThat(primeiro.fullCode()).isEqualTo("PLACED");
        assertThat(primeiro.orderId()).isEqualTo("pedido-1");
        assertThat(primeiro.merchantId()).isEqualTo("loja-1");
        assertThat(primeiro.createdAt()).isEqualTo(OffsetDateTime.parse("2026-10-10T22:15:30.123Z"));
        assertThat(eventos.get(1).fullCode()).isNull();
        assertThat(eventos.get(1).createdAt()).isNull();
        servidor.verify();
    }

    @Test
    @DisplayName("o payload guardado e o JSON do evento inteiro, inclusive campos que o record nao conhece")
    void buscarEventos_guardaOPayloadCruDoEvento() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DO_POLLING)).andRespond(withSuccess(
                "[{\"id\":\"evento-1\",\"code\":\"PLC\",\"metadata\":{\"campoNovo\":\"valor\"}}]",
                MediaType.APPLICATION_JSON));

        List<EventoIfoodResponse> eventos = cliente().buscarEventos();

        assertThat(eventos.get(0).payload())
                .isEqualTo("{\"id\":\"evento-1\",\"code\":\"PLC\",\"metadata\":{\"campoNovo\":\"valor\"}}");
    }

    @Test
    void buscarEventos_devolveListaVazia_quandoOIfoodRespondeSemConteudo() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DO_POLLING)).andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThat(cliente().buscarEventos()).isEmpty();
    }

    @Test
    void buscarEventos_devolveDataNula_quandoOCreatedAtNaoEDataValida() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DO_POLLING)).andRespond(withSuccess(
                "[{\"id\":\"evento-1\",\"createdAt\":\"ontem\"}]", MediaType.APPLICATION_JSON));

        assertThat(cliente().buscarEventos().get(0).createdAt()).isNull();
    }

    @Test
    void buscarEventos_lancaIntegracaoIfoodEDescartaOToken_quandoOTokenERecusado() {
        esperarToken("token-1");
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_POLLING))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("corpo-cru-do-ifood"));
        esperarToken("token-2");
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_POLLING))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-2"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        IfoodEventoClient cliente = cliente();

        assertThatThrownBy(cliente::buscarEventos)
                .isInstanceOfSatisfying(IntegracaoIfoodException.class, excecao -> {
                    assertThat(excecao.getStatusHttp()).isEqualTo(401);
                    assertThat(excecao.getMessage()).doesNotContain("corpo-cru-do-ifood").doesNotContain("token-1");
                });

        assertThat(cliente.buscarEventos()).isEmpty();
        servidor.verify();
    }

    @Test
    void buscarEventos_lancaIntegracaoIfood_quandoOIfoodRespondeErroDeServidor() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DO_POLLING)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> cliente().buscarEventos())
                .isInstanceOfSatisfying(IntegracaoIfoodException.class,
                        excecao -> assertThat(excecao.getStatusHttp()).isEqualTo(503));
    }

    @Test
    void confirmarRecebimento_enviaOsIdsNoCorpo() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DA_CONFIRMACAO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-1"))
                .andExpect(content().string("[{\"id\":\"evento-1\"},{\"id\":\"evento-2\"}]"))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        cliente().confirmarRecebimento(List.of("evento-1", "evento-2"));

        servidor.verify();
    }

    @Test
    void confirmarRecebimento_naoChamaOIfood_quandoNaoHaEvento() {
        cliente().confirmarRecebimento(List.of());

        servidor.verify();
    }

    @Test
    void confirmarRecebimento_lancaIntegracaoIfood_quandoOIfoodRecusa() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DA_CONFIRMACAO)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> cliente().confirmarRecebimento(List.of("evento-1")))
                .isInstanceOfSatisfying(IntegracaoIfoodException.class,
                        excecao -> assertThat(excecao.getStatusHttp()).isEqualTo(502));
    }

    private void esperarToken(String token) {
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess("{\"accessToken\":\"" + token + "\",\"type\":\"bearer\",\"expiresIn\":21600}",
                        MediaType.APPLICATION_JSON));
    }

    private IfoodEventoClient cliente() {
        return new IfoodEventoClient(restClient,
                new IfoodAutenticador(restClient, "id", "segredo", Clock.systemUTC()));
    }
}
