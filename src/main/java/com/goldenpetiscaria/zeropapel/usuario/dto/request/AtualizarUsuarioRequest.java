package com.goldenpetiscaria.zeropapel.usuario.dto.request;

import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(
        String nome,
        Cargo cargo,
        @Size(min = 4, message = "Senha deve ter no mínimo 4 caracteres")
        String senha
) {}
