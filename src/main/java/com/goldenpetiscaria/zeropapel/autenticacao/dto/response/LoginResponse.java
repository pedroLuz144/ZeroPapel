package com.goldenpetiscaria.zeropapel.autenticacao.dto.response;

public record LoginResponse(
        String token,
        String refreshToken
) {}
