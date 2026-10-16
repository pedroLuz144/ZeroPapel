package com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LojaIfoodResponse(
        String id,
        String name,
        String corporateName
) {}
