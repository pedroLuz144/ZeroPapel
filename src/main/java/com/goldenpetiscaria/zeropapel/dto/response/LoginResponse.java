package com.goldenpetiscaria.zeropapel.dto.response;

public record LoginResponse(
        String token,
        String refreshToken
) {}
