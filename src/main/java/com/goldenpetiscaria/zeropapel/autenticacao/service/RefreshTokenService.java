package com.goldenpetiscaria.zeropapel.autenticacao.service;

import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;

public interface RefreshTokenService {
    String gerar(Usuario usuario);
    Usuario validarERetornarUsuario(String token);
    void revogar(String token);
}
