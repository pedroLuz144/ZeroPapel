package com.goldenpetiscaria.zeropapel.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PontoDeEntradaNaoAutenticado implements AuthenticationEntryPoint {

    static final String MENSAGEM = "Não autenticado";

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        RespostaDeErroJson.escrever(response, HttpStatus.UNAUTHORIZED, MENSAGEM);
    }
}
