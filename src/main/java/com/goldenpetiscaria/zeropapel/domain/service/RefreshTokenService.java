package com.goldenpetiscaria.zeropapel.domain.service;

import com.goldenpetiscaria.zeropapel.domain.entity.Usuario;

public interface RefreshTokenService {
    String gerar(Usuario usuario);
    Usuario validarERetornarUsuario(String token);
    void revogar(String token);
}
