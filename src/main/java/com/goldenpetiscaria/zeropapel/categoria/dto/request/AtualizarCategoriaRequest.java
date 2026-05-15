package com.goldenpetiscaria.zeropapel.categoria.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AtualizarCategoriaRequest(
        @NotBlank(message = "A categoria precisa de um nome")
        String nome
) {}
