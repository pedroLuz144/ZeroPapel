package com.goldenpetiscaria.zeropapel.autenticacao.listener;

import com.goldenpetiscaria.zeropapel.autenticacao.service.RefreshTokenService;
import com.goldenpetiscaria.zeropapel.usuario.evento.SessoesDoUsuarioInvalidadasEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevogacaoDeSessoesListenerTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private RevogacaoDeSessoesListener listener;

    @Test
    void revogaTodosOsRefreshTokensDoUsuario() {
        listener.aoInvalidarSessoesDoUsuario(new SessoesDoUsuarioInvalidadasEvent(7L));

        verify(refreshTokenService).revogarTodosDoUsuario(7L);
    }
}
