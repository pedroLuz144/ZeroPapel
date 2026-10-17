package com.goldenpetiscaria.zeropapel.pedido.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemPedidoExternoRequest(
        @NotBlank(message = "O nome do item é obrigatório")
        @Size(max = 255, message = "O nome do item deve ter no máximo 255 caracteres")
        String nomeItem,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser pelo menos 1")
        Integer quantidade,

        @NotNull(message = "O preço unitário é obrigatório")
        @DecimalMin(value = "0.00", message = "O preço unitário não pode ser negativo")
        @Digits(integer = 36, fraction = 2, message = "O preço unitário deve ter no máximo duas casas decimais")
        BigDecimal precoUnitario
) {}
