package com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response;

import java.time.OffsetDateTime;

public record EventoIfoodResponse(
        String id,
        String code,
        String fullCode,
        String orderId,
        String merchantId,
        OffsetDateTime createdAt,
        String payload
) {}
