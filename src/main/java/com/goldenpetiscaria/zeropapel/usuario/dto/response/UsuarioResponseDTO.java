package com.goldenpetiscaria.zeropapel.usuario.dto.response;

import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String usuario,
        Cargo cargo,
        boolean ativo
) {}
