package com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoIfoodResponse(
        String id,
        OffsetDateTime createdAt,
        Cliente customer,
        List<Item> items,
        Entrega delivery,
        Total total
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cliente(String name) {
        @Override
        public String toString() {
            return "Cliente[]";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entrega(String deliveredBy) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Total(BigDecimal deliveryFee) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String name,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal optionsPrice,
            BigDecimal totalPrice
    ) {}
}
