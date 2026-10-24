package com.goldenpetiscaria.zeropapel.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class RespostaDeErroJson {

    private RespostaDeErroJson() {
    }

    static void escrever(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + escapar(mensagem) + "\"}");
    }

    private static String escapar(String mensagem) {
        return mensagem.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
