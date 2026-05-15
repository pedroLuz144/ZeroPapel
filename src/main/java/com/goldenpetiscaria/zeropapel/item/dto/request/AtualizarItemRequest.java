package com.goldenpetiscaria.zeropapel.item.dto.request;

import com.goldenpetiscaria.zeropapel.item.enumerator.Status;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record AtualizarItemRequest(
        String nome,
        Long categoria,
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
        BigDecimal preco,
        Status status,
        String descricao
) {}
