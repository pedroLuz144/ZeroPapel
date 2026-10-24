package com.goldenpetiscaria.zeropapel.autenticacao.listener;

import com.goldenpetiscaria.zeropapel.autenticacao.service.RefreshTokenService;
import com.goldenpetiscaria.zeropapel.usuario.evento.SessoesDoUsuarioInvalidadasEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RevogacaoDeSessoesListener {

    private final RefreshTokenService refreshTokenService;

    public RevogacaoDeSessoesListener(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @EventListener
    public void aoInvalidarSessoesDoUsuario(SessoesDoUsuarioInvalidadasEvent evento) {
        refreshTokenService.revogarTodosDoUsuario(evento.usuarioId());
    }
}
