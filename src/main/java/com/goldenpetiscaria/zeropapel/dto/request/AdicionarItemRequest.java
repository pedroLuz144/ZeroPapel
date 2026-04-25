package com.goldenpetiscaria.zeropapel.dto.request;

import com.goldenpetiscaria.zeropapel.domain.enums.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record AdicionarItemRequest(
        @NotBlank(message = "O item precisa de um nome")
        String nome,

        @NotNull(message = "O item precisa de uma categoria")
        Categoria categoria,

        @NotNull(message = "O item precisa ter preço")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
        BigDecimal preco,

        String descricao
) {}
