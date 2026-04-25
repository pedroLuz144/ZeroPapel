package com.goldenpetiscaria.zeropapel.dto.request;

import com.goldenpetiscaria.zeropapel.domain.enums.Categoria;
import com.goldenpetiscaria.zeropapel.domain.enums.Status;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record AtualizarItemRequest(
        String nome,
        Categoria categoria,
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
        BigDecimal preco,
        Status status,
        String descricao
) {}
