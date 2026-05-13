package com.goldenpetiscaria.zeropapel.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdicionarCategoriaRequest(
        @NotBlank(message = "A categoria precisa de um nome")
        String nome
) {}
