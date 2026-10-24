package com.goldenpetiscaria.zeropapel.usuario.dto.request;

import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(
        String nome,
        String usuario,
        Cargo cargo,
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
        String senha
) {}
