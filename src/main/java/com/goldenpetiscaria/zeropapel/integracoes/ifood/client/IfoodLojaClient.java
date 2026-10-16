package com.goldenpetiscaria.zeropapel.integracoes.ifood.client;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.LojaIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
public class IfoodLojaClient {

    private static final String CAMINHO_DAS_LOJAS = "/merchant/v1.0/merchants";
    private static final ParameterizedTypeReference<List<LojaIfoodResponse>> LISTA_DE_LOJAS =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final IfoodAutenticador autenticador;

    public IfoodLojaClient(RestClient restClient, IfoodAutenticador autenticador) {
        this.restClient = restClient;
        this.autenticador = autenticador;
    }

    public List<LojaIfoodResponse> listarLojas() {
        String token = autenticador.obterToken();
        try {
            List<LojaIfoodResponse> lojas = restClient.get()
                    .uri(CAMINHO_DAS_LOJAS)
                    .headers(cabecalhos -> cabecalhos.setBearerAuth(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(LISTA_DE_LOJAS);
            return lojas != null ? lojas : List.of();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("iFood recusou a listagem de lojas: HTTP {}", status);
            if (status == 401) {
                autenticador.descartarToken(token);
            }
            throw new IntegracaoIfoodException("O iFood respondeu HTTP " + status + " à listagem de lojas", status);
        } catch (RestClientException e) {
            log.warn("Falha de comunicação com o iFood ao listar as lojas: {}", e.getClass().getSimpleName());
            throw new IntegracaoIfoodException("Não foi possível comunicar com o iFood para listar as lojas");
        }
    }
}
