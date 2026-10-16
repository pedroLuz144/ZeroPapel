package com.goldenpetiscaria.zeropapel.integracoes.ifood.client;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.CredenciaisIfoodAusentesException;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IfoodAutenticadorTest {

    private static final String URL_DO_TOKEN = "https://ifood.teste/authentication/v1.0/oauth/token";
    private static final String CLIENT_ID = "id-do-app";
    private static final String CLIENT_SECRET = "segredo-do-app";

    private MockRestServiceServer servidor;
    private RestClient restClient;
    private RelogioAjustavel relogio;

    @BeforeEach
    void montarServidor() {
        RestClient.Builder construtor = RestClient.builder().baseUrl("https://ifood.teste");
        servidor = MockRestServiceServer.bindTo(construtor).build();
        restClient = construtor.build();
        relogio = new RelogioAjustavel(Instant.parse("2026-10-08T20:00:00Z"));
    }

    @Test
    void obterToken_enviaAsCredenciaisComoFormularioEDevolveOToken() {
        servidor.expect(requestTo(URL_DO_TOKEN))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formDataContains(java.util.Map.of(
                        "grantType", "client_credentials",
                        "clientId", CLIENT_ID,
                        "clientSecret", CLIENT_SECRET)))
                .andRespond(withSuccess(respostaDeToken("token-1", 21600), MediaType.APPLICATION_JSON));

        String token = autenticador().obterToken();

        assertThat(token).isEqualTo("token-1");
        servidor.verify();
    }

    @Test
    @DisplayName("enquanto o token vale, nao ha segunda chamada HTTP")
    void obterToken_reutilizaOToken_enquantoEleEValido() {
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess(respostaDeToken("token-1", 21600), MediaType.APPLICATION_JSON));
        IfoodAutenticador autenticador = autenticador();

        String primeiro = autenticador.obterToken();
        relogio.avancar(Duration.ofHours(5));
        String segundo = autenticador.obterToken();

        assertThat(segundo).isEqualTo(primeiro).isEqualTo("token-1");
        servidor.verify();
    }

    @Test
    void obterToken_renova_depoisDeExpirar() {
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess(respostaDeToken("token-1", 21600), MediaType.APPLICATION_JSON));
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess(respostaDeToken("token-2", 21600), MediaType.APPLICATION_JSON));
        IfoodAutenticador autenticador = autenticador();

        String primeiro = autenticador.obterToken();
        relogio.avancar(Duration.ofSeconds(21601));
        String segundo = autenticador.obterToken();

        assertThat(primeiro).isEqualTo("token-1");
        assertThat(segundo).isEqualTo("token-2");
        servidor.verify();
    }

    @Test
    @DisplayName("renova com margem de seguranca, antes do instante exato de expiracao")
    void obterToken_renova_dentroDaMargemAntesDeExpirar() {
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess(respostaDeToken("token-1", 21600), MediaType.APPLICATION_JSON));
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess(respostaDeToken("token-2", 21600), MediaType.APPLICATION_JSON));
        IfoodAutenticador autenticador = autenticador();

        autenticador.obterToken();
        relogio.avancar(Duration.ofSeconds(21600 - 30));

        assertThat(autenticador.obterToken()).isEqualTo("token-2");
        servidor.verify();
    }

    @Test
    void descartarToken_forcaNovaSolicitacao_quandoOTokenDescartadoEOVigente() {
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess(respostaDeToken("token-1", 21600), MediaType.APPLICATION_JSON));
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess(respostaDeToken("token-2", 21600), MediaType.APPLICATION_JSON));
        IfoodAutenticador autenticador = autenticador();

        autenticador.descartarToken(autenticador.obterToken());

        assertThat(autenticador.obterToken()).isEqualTo("token-2");
        servidor.verify();
    }

    @ParameterizedTest
    @CsvSource(value = {"'', segredo", "id, ''", "'  ', segredo", "NULO, segredo", "id, NULO"}, nullValues = "NULO")
    @DisplayName("sem credenciais nao ha chamada ao iFood")
    void obterToken_lancaCredenciaisAusentes_semChamarHttp(String clientId, String clientSecret) {
        IfoodAutenticador autenticador = new IfoodAutenticador(restClient, clientId, clientSecret, relogio);

        assertThatThrownBy(autenticador::obterToken)
                .isInstanceOf(CredenciaisIfoodAusentesException.class)
                .isInstanceOf(IntegracaoIfoodException.class)
                .hasMessageContaining("IFOOD_CLIENT_ID");

        servidor.verify();
    }

    @Test
    @DisplayName("401 do iFood vira excecao da integracao, sem o corpo cru da resposta")
    void obterToken_lancaIntegracaoIfood_quandoOIfoodRecusaAsCredenciais() {
        servidor.expect(requestTo(URL_DO_TOKEN))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"Unauthorized\",\"message\":\"corpo-cru-do-ifood\"}}"));

        assertThatThrownBy(() -> autenticador().obterToken())
                .isInstanceOfSatisfying(IntegracaoIfoodException.class, excecao -> {
                    assertThat(excecao.getStatusHttp()).isEqualTo(401);
                    assertThat(excecao.getMessage()).contains("401").doesNotContain("corpo-cru-do-ifood");
                    assertThat(excecao.getCause()).isNull();
                });
    }

    @Test
    void obterToken_lancaIntegracaoIfood_quandoOIfoodRespondeErroDeServidor() {
        servidor.expect(requestTo(URL_DO_TOKEN)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> autenticador().obterToken())
                .isInstanceOfSatisfying(IntegracaoIfoodException.class,
                        excecao -> assertThat(excecao.getStatusHttp()).isEqualTo(503));
    }

    @Test
    void obterToken_lancaIntegracaoIfood_quandoAConexaoFalha() {
        servidor.expect(requestTo(URL_DO_TOKEN)).andRespond(requisicao -> {
            throw new IOException("tempo esgotado");
        });

        assertThatThrownBy(() -> autenticador().obterToken())
                .isInstanceOfSatisfying(IntegracaoIfoodException.class,
                        excecao -> assertThat(excecao.getStatusHttp()).isNull());
    }

    @Test
    void obterToken_lancaIntegracaoIfood_quandoARespostaNaoTrazToken() {
        servidor.expect(requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess("{\"type\":\"bearer\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> autenticador().obterToken())
                .isInstanceOf(IntegracaoIfoodException.class)
                .hasMessageContaining("inválida");
    }

    @Test
    @DisplayName("falha na renovacao nao deixa token velho em uso nem trava a proxima tentativa")
    void obterToken_tentaDeNovo_depoisDeUmaFalha() {
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        servidor.expect(ExpectedCount.once(), requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess(respostaDeToken("token-1", 21600), MediaType.APPLICATION_JSON));
        IfoodAutenticador autenticador = autenticador();

        assertThatThrownBy(autenticador::obterToken).isInstanceOf(IntegracaoIfoodException.class);

        assertThat(autenticador.obterToken()).isEqualTo("token-1");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("o log informa a expiracao e nunca traz o token nem o segredo")
    void obterToken_naoLogaTokenNemSegredo(CapturedOutput saida) {
        servidor.expect(requestTo(URL_DO_TOKEN))
                .andRespond(withSuccess(respostaDeToken("token-sigiloso", 21600), MediaType.APPLICATION_JSON));

        autenticador().obterToken();

        assertThat(saida.getAll())
                .contains("expira em 21600 segundos")
                .doesNotContain("token-sigiloso")
                .doesNotContain(CLIENT_SECRET);
    }

    private IfoodAutenticador autenticador() {
        return new IfoodAutenticador(restClient, CLIENT_ID, CLIENT_SECRET, relogio);
    }

    private String respostaDeToken(String token, long expiraEmSegundos) {
        return "{\"accessToken\":\"" + token + "\",\"type\":\"bearer\",\"expiresIn\":" + expiraEmSegundos + "}";
    }

    private static final class RelogioAjustavel extends Clock {

        private Instant agora;

        private RelogioAjustavel(Instant agora) {
            this.agora = agora;
        }

        private void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
