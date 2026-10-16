package com.goldenpetiscaria.zeropapel.integracoes.ifood.client;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.TokenIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.CredenciaisIfoodAusentesException;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
public class IfoodAutenticador {

    private static final String CAMINHO_DO_TOKEN = "/authentication/v1.0/oauth/token";
    private static final Duration MARGEM_DE_RENOVACAO = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final Clock relogio;

    private volatile TokenVigente tokenVigente;

    public IfoodAutenticador(RestClient restClient, String clientId, String clientSecret, Clock relogio) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.relogio = relogio;
    }

    public String obterToken() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new CredenciaisIfoodAusentesException();
        }
        TokenVigente atual = tokenVigente;
        if (aindaVale(atual)) {
            return atual.valor();
        }
        synchronized (this) {
            TokenVigente conferido = tokenVigente;
            if (aindaVale(conferido)) {
                return conferido.valor();
            }
            TokenVigente novo = solicitarToken();
            tokenVigente = novo;
            return novo.valor();
        }
    }

    public void descartarToken(String tokenRecusado) {
        synchronized (this) {
            TokenVigente atual = tokenVigente;
            if (atual != null && atual.valor().equals(tokenRecusado)) {
                tokenVigente = null;
            }
        }
    }

    private boolean aindaVale(TokenVigente token) {
        return token != null && relogio.instant().isBefore(token.renovarAPartirDe());
    }

    private TokenVigente solicitarToken() {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grantType", "client_credentials");
        formulario.add("clientId", clientId.trim());
        formulario.add("clientSecret", clientSecret.trim());

        TokenIfoodResponse resposta;
        try {
            resposta = restClient.post()
                    .uri(CAMINHO_DO_TOKEN)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(formulario)
                    .retrieve()
                    .body(TokenIfoodResponse.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("iFood recusou o pedido de token: HTTP {}", status);
            if (status == 401 || status == 403) {
                throw new IntegracaoIfoodException(
                        "O iFood recusou as credenciais do aplicativo (HTTP " + status + ")", status);
            }
            throw new IntegracaoIfoodException("O iFood respondeu HTTP " + status + " ao pedido de token", status);
        } catch (RestClientException e) {
            log.warn("Falha de comunicação com o iFood ao pedir o token: {}", e.getClass().getSimpleName());
            throw new IntegracaoIfoodException("Não foi possível comunicar com o iFood para obter o token");
        }

        if (resposta == null || resposta.accessToken() == null || resposta.accessToken().isBlank()
                || resposta.expiresIn() == null || resposta.expiresIn() <= 0) {
            throw new IntegracaoIfoodException("O iFood devolveu uma resposta de token inválida");
        }

        log.info("Token do iFood obtido; expira em {} segundos", resposta.expiresIn());
        Instant expiraEm = relogio.instant().plusSeconds(resposta.expiresIn());
        return new TokenVigente(resposta.accessToken(), expiraEm.minus(MARGEM_DE_RENOVACAO));
    }

    private record TokenVigente(String valor, Instant renovarAPartirDe) {
        @Override
        public String toString() {
            return "TokenVigente[renovarAPartirDe=" + renovarAPartirDe + "]";
        }
    }
}
