package com.goldenpetiscaria.zeropapel.integracoes.ifood.client;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.PedidoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
public class IfoodPedidoClient {

    private static final String CAMINHO_DO_PEDIDO = "/order/v1.0/orders/{orderId}";

    private final RestClient restClient;
    private final IfoodAutenticador autenticador;

    public IfoodPedidoClient(RestClient restClient, IfoodAutenticador autenticador) {
        this.restClient = restClient;
        this.autenticador = autenticador;
    }

    public PedidoIfoodResponse buscarPedido(String orderId) {
        String token = autenticador.obterToken();
        PedidoIfoodResponse pedido;
        try {
            pedido = restClient.get()
                    .uri(CAMINHO_DO_PEDIDO, orderId)
                    .headers(cabecalhos -> cabecalhos.setBearerAuth(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PedidoIfoodResponse.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("iFood respondeu HTTP {} à busca do pedido", status);
            if (status == 401) {
                autenticador.descartarToken(token);
            }
            throw new IntegracaoIfoodException("O iFood respondeu HTTP " + status + " à busca do pedido", status);
        } catch (RestClientException e) {
            log.warn("Falha de comunicação com o iFood ao buscar o pedido: {}", e.getClass().getSimpleName());
            throw new IntegracaoIfoodException("Não foi possível comunicar com o iFood para buscar o pedido");
        }
        if (pedido == null) {
            throw new IntegracaoIfoodException("O iFood devolveu o pedido sem conteúdo");
        }
        return pedido;
    }
}
