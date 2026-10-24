package com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenIfoodResponse(
        String accessToken,
        String type,
        Long expiresIn
) {
    @Override
    public String toString() {
        return "TokenIfoodResponse[type=" + type + ", expiresIn=" + expiresIn + "]";
    }
}
