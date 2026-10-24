package com.goldenpetiscaria.zeropapel.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("JSON malformado responde 400 com mensagem generica, sem a mensagem do Jackson")
    void handleCorpoIlegivel_responde400SemVazarDetalhes() {
        HttpMessageNotReadableException excecao = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected character ('}' (code 125)) at [Source: REDACTED; line: 1, column: 12]",
                new IllegalStateException("com.fasterxml.jackson.core.JsonParseException"),
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Map<String, String>> resposta = handler.handleCorpoIlegivel(excecao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsOnlyKeys("error");
        assertThat(resposta.getBody().get("error"))
                .isEqualTo("Corpo da requisição inválido ou malformado")
                .doesNotContain("JSON parse", "jackson", "Source", "column");
    }

    @Test
    @DisplayName("tipo errado num campo tambem responde 400 com a mesma mensagem generica")
    void handleCorpoIlegivel_tipoErrado_responde400() {
        HttpMessageNotReadableException excecao = new HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type `java.lang.Long` from String \"abc\"",
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Map<String, String>> resposta = handler.handleCorpoIlegivel(excecao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsExactly(Map.entry("error", "Corpo da requisição inválido ou malformado"));
    }
}
