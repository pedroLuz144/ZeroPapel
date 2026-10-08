package com.goldenpetiscaria.zeropapel.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RespostaNaoAutorizadaTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pedidos");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void commence_responde401_quandoRequisicaoNaoEstaAutenticada() throws Exception {
        new PontoDeEntradaNaoAutenticado()
                .commence(request, response, new InsufficientAuthenticationException("sem token"));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void commence_escreveCorpoJsonComCampoError() throws Exception {
        new PontoDeEntradaNaoAutenticado()
                .commence(request, response, new InsufficientAuthenticationException("sem token"));

        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("{\"error\":\"Não autenticado\"}");
    }

    @Test
    void commence_naoVazaAMensagemDaExcecao() throws Exception {
        new PontoDeEntradaNaoAutenticado()
                .commence(request, response, new InsufficientAuthenticationException("detalhe interno"));

        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).doesNotContain("detalhe interno");
    }

    @Test
    void handle_responde403_quandoPapelEInsuficiente() throws Exception {
        new ManipuladorDeAcessoNegado()
                .handle(request, response, new AccessDeniedException("negado"));

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void handle_escreveCorpoJsonComCampoError() throws Exception {
        new ManipuladorDeAcessoNegado()
                .handle(request, response, new AccessDeniedException("negado"));

        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("{\"error\":\"Acesso negado\"}");
    }

    @Test
    void escrever_escapaAspasEBarras_paraOCorpoContinuarJsonValido() throws Exception {
        RespostaDeErroJson.escrever(response, HttpStatus.UNAUTHORIZED, "a\"b\\c");

        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("{\"error\":\"a\\\"b\\\\c\"}");
    }
}
