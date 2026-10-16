package com.goldenpetiscaria.zeropapel.integracoes.ifood.client;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.LojaIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.CredenciaisIfoodAusentesException;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IfoodLojaClientTest {

    private static final String URL_DO_TOKEN = "https://ifood.teste/authentication/v1.0/oauth/token";
    private static final String URL_DAS_LOJAS = "https://ifood.teste/merchant/v1.0/merchants";

    private MockRestServiceServer servidor;
    private RestClient restClient;

    @BeforeEach
    void montarServidor() {
        RestClient.Builder construtor = RestClient.builder().baseUrl("https://ifood.teste");
        servidor = MockRestServiceServer.bindTo(construtor).build();
        restClient = construtor.build();
    }

    @Test
    void listarLojas_enviaOBearerEDesserializaAResposta() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DAS_LOJAS))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-1"))
                .andRespond(withSuccess("""
                        [
                          {"id": "11111111-2222-3333-4444-555555555555", "name": "Loja Teste", "corporateName": "Loja Teste LTDA", "campoNovo": 1},
                          {"id": "66666666-7777-8888-9999-000000000000", "name": "Outra Loja", "corporateName": null}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<LojaIfoodResponse> lojas = cliente("id", "segredo").listarLojas();

        assertThat(lojas).containsExactly(
                new LojaIfoodResponse("11111111-2222-3333-4444-555555555555", "Loja Teste", "Loja Teste LTDA"),
                new LojaIfoodResponse("66666666-7777-8888-9999-000000000000", "Outra Loja", null));
        servidor.verify();
    }

    @Test
    void listarLojas_devolveListaVazia_quandoOAppNaoTemLoja() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DAS_LOJAS)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(cliente("id", "segredo").listarLojas()).isEmpty();
    }

    @Test
    @DisplayName("401 na listagem descarta o token para a proxima chamada pedir outro")
    void listarLojas_lancaIntegracaoIfoodEDescartaOToken_quandoOTokenERecusado() {
        esperarToken("token-1");
        servidor.expect(ExpectedCount.once(), requestTo(URL_DAS_LOJAS))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("corpo-cru-do-ifood"));
        esperarToken("token-2");
        servidor.expect(ExpectedCount.once(), requestTo(URL_DAS_LOJAS))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-2"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        IfoodLojaClient cliente = cliente("id", "segredo");

        assertThatThrownBy(cliente::listarLojas)
                .isInstanceOfSatisfying(IntegracaoIfoodException.class, excecao -> {
                    assertThat(excecao.getStatusHttp()).isEqualTo(401);
                    assertThat(excecao.getMessage()).doesNotContain("corpo-cru-do-ifood").doesNotContain("token-1");
                });

        assertThat(cliente.listarLojas()).isEmpty();
        servidor.verify();
    }

    @Test
    void listarLojas_lancaIntegracaoIfood_quandoOIfoodRespondeErroDeServidor() {
        esperarToken("token-1");
        servidor.expect(requestTo(URL_DAS_LOJAS)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> cliente("id", "segredo").listarLojas())
                .isInstanceOfSatisfying(IntegracaoIfoodException.class,
                        excecao -> assertThat(excecao.getStatusHttp()).isEqualTo(502));
    }

    @Test
    void listarLojas_lancaCredenciaisAusentes_semChamarHttp() {
        assertThatThrownBy(() -> cliente("", "").listarLojas())
                .isInstanceOf(CredenciaisIfoodAusentesException.class);

        servidor.verify();
    }

    private void esperarToken(String token) {
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess("{\"accessToken\":\"" + token + "\",\"type\":\"bearer\",\"expiresIn\":21600}",
                        MediaType.APPLICATION_JSON));
    }

    private IfoodLojaClient cliente(String clientId, String clientSecret) {
        return new IfoodLojaClient(restClient,
                new IfoodAutenticador(restClient, clientId, clientSecret, Clock.systemUTC()));
    }
}
