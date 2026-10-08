package com.goldenpetiscaria.zeropapel.plataforma.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdicionarPlataformaRequest(
        @NotBlank(message = "O nome da plataforma é obrigatório")
        String nome,

        Boolean entrega
) {}
