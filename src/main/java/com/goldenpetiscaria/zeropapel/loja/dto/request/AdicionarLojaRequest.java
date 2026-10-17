package com.goldenpetiscaria.zeropapel.loja.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdicionarLojaRequest(
        @NotBlank(message = "O nome da loja é obrigatório")
        String nome
) {}
