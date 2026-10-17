package com.goldenpetiscaria.zeropapel.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ManipuladorDeAcessoNegado implements AccessDeniedHandler {

    static final String MENSAGEM = "Acesso negado";

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        RespostaDeErroJson.escrever(response, HttpStatus.FORBIDDEN, MENSAGEM);
    }
}
