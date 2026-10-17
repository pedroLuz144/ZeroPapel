package com.goldenpetiscaria.zeropapel.integracoes.ifood.client;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.PedidoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IfoodPedidoClientTest {

    private static final String URL_DO_TOKEN = "https://ifood.teste/authentication/v1.0/oauth/token";
    private static final String URL_DO_PEDIDO = "https://ifood.teste/order/v1.0/orders/pedido-1";

    private MockRestServiceServer servidor;
    private RestClient restClient;

    @BeforeEach
    void montarServidor() {
        RestClient.Builder construtor = RestClient.builder().baseUrl("https://ifood.teste");
        servidor = MockRestServiceServer.bindTo(construtor).build();
        restClient = construtor.build();
    }

    @Test
    void buscarPedido_enviaOBearerEDesserializaOPedido() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DO_PEDIDO))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-1"))
                .andRespond(withSuccess("""
                        {
                          "id": "pedido-1",
                          "displayId": "1234",
                          "createdAt": "2026-10-10T22:15:30.123Z",
                          "customer": {"id": "c1", "name": "Maria", "documentNumber": "000"},
                          "items": [
                            {"index": 1, "name": "Porção de fritas", "quantity": 2, "unitPrice": 25.5,
                             "optionsPrice": 4, "totalPrice": 55, "options": [{"name": "Cheddar"}]}
                          ],
                          "total": {"orderAmount": 61.99}
                        }
                        """, MediaType.APPLICATION_JSON));

        PedidoIfoodResponse pedido = cliente().buscarPedido("pedido-1");

        assertThat(pedido.id()).isEqualTo("pedido-1");
        assertThat(pedido.createdAt()).isEqualTo(OffsetDateTime.parse("2026-10-10T22:15:30.123Z"));
        assertThat(pedido.customer().name()).isEqualTo("Maria");
        assertThat(pedido.items()).hasSize(1);
        PedidoIfoodResponse.Item item = pedido.items().get(0);
        assertThat(item.name()).isEqualTo("Porção de fritas");
        assertThat(item.quantity()).isEqualByComparingTo("2");
        assertThat(item.unitPrice()).isEqualByComparingTo("25.50");
        assertThat(item.optionsPrice()).isEqualByComparingTo("4");
        assertThat(item.totalPrice()).isEqualByComparingTo(new BigDecimal("55"));
        servidor.verify();
    }

    @Test
    void buscarPedido_lancaIntegracaoIfood_quandoOPedidoNaoExiste() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DO_PEDIDO))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("corpo-cru-do-ifood"));

        assertThatThrownBy(() -> cliente().buscarPedido("pedido-1"))
                .isInstanceOfSatisfying(IntegracaoIfoodException.class, excecao -> {
                    assertThat(excecao.getStatusHttp()).isEqualTo(404);
                    assertThat(excecao.getMessage()).doesNotContain("corpo-cru-do-ifood");
                });
    }

    @Test
    void buscarPedido_descartaOToken_quandoOTokenERecusado() {
        esperarToken("token-1");
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_PEDIDO))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        esperarToken("token-2");
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_PEDIDO))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-2"))
                .andRespond(withSuccess("{\"id\":\"pedido-1\"}", MediaType.APPLICATION_JSON));
        IfoodPedidoClient cliente = cliente();

        assertThatThrownBy(() -> cliente.buscarPedido("pedido-1")).isInstanceOf(IntegracaoIfoodException.class);

        assertThat(cliente.buscarPedido("pedido-1").id()).isEqualTo("pedido-1");
        servidor.verify();
    }

    private void esperarToken(String token) {
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess("{\"accessToken\":\"" + token + "\",\"type\":\"bearer\",\"expiresIn\":21600}",
                        MediaType.APPLICATION_JSON));
    }

    private IfoodPedidoClient cliente() {
        return new IfoodPedidoClient(restClient,
                new IfoodAutenticador(restClient, "id", "segredo", Clock.systemUTC()));
    }
}
