package com.goldenpetiscaria.zeropapel.integracoes.ifood.client;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.request.ConfirmarEventoIfoodRequest;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.EventoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class IfoodEventoClient {

    private static final String CAMINHO_DO_POLLING = "/events/v1.0/events:polling";
    private static final String CAMINHO_DA_CONFIRMACAO = "/events/v1.0/events/acknowledgment";
    private static final ParameterizedTypeReference<List<JsonNode>> LISTA_DE_EVENTOS =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final IfoodAutenticador autenticador;

    public IfoodEventoClient(RestClient restClient, IfoodAutenticador autenticador) {
        this.restClient = restClient;
        this.autenticador = autenticador;
    }

    public List<EventoIfoodResponse> buscarEventos() {
        String token = autenticador.obterToken();
        List<JsonNode> eventosCrus;
        try {
            eventosCrus = restClient.get()
                    .uri(CAMINHO_DO_POLLING)
                    .headers(cabecalhos -> cabecalhos.setBearerAuth(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(LISTA_DE_EVENTOS);
        } catch (RestClientResponseException e) {
            throw recusa(e, token, "ao polling de eventos");
        } catch (RestClientException e) {
            throw falhaDeComunicacao(e, "buscar os eventos");
        }

        List<EventoIfoodResponse> eventos = new ArrayList<>();
        if (eventosCrus == null) {
            return eventos;
        }
        for (JsonNode eventoCru : eventosCrus) {
            if (eventoCru != null && eventoCru.isObject()) {
                eventos.add(paraEvento(eventoCru));
            }
        }
        return eventos;
    }

    public void confirmarRecebimento(List<String> idsDosEventos) {
        if (idsDosEventos == null || idsDosEventos.isEmpty()) {
            return;
        }
        List<ConfirmarEventoIfoodRequest> corpo = new ArrayList<>();
        for (String idDoEvento : idsDosEventos) {
            corpo.add(new ConfirmarEventoIfoodRequest(idDoEvento));
        }
        String token = autenticador.obterToken();
        try {
            restClient.post()
                    .uri(CAMINHO_DA_CONFIRMACAO)
                    .headers(cabecalhos -> cabecalhos.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw recusa(e, token, "à confirmação de eventos");
        } catch (RestClientException e) {
            throw falhaDeComunicacao(e, "confirmar os eventos");
        }
    }

    private EventoIfoodResponse paraEvento(JsonNode eventoCru) {
        return new EventoIfoodResponse(
                texto(eventoCru, "id"),
                texto(eventoCru, "code"),
                texto(eventoCru, "fullCode"),
                texto(eventoCru, "orderId"),
                texto(eventoCru, "merchantId"),
                dataHora(texto(eventoCru, "createdAt")),
                eventoCru.toString());
    }

    private String texto(JsonNode eventoCru, String campo) {
        JsonNode valor = eventoCru.path(campo);
        if (valor.isMissingNode() || valor.isNull() || valor.isContainer()) {
            return null;
        }
        return valor.asString();
    }

    private OffsetDateTime dataHora(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private IntegracaoIfoodException recusa(RestClientResponseException e, String token, String operacao) {
        int status = e.getStatusCode().value();
        log.warn("iFood respondeu HTTP {} {}", status, operacao);
        if (status == 401) {
            autenticador.descartarToken(token);
        }
        return new IntegracaoIfoodException("O iFood respondeu HTTP " + status + " " + operacao, status);
    }

    private IntegracaoIfoodException falhaDeComunicacao(RestClientException e, String operacao) {
        log.warn("Falha de comunicação com o iFood ao {}: {}", operacao, e.getClass().getSimpleName());
        return new IntegracaoIfoodException("Não foi possível comunicar com o iFood para " + operacao);
    }
}
