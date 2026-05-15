package com.goldenpetiscaria.zeropapel.usuario.dto.request;

import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastrarUsuarioRequest(

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Usuário é obrigatório")
        String usuario,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 4, message = "Senha deve ter no mínimo 4 caracteres")
        String senha,

        @NotNull(message = "Role é obrigatória")
        Cargo cargo
) {
}
