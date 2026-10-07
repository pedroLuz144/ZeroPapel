package com.goldenpetiscaria.zeropapel.autenticacao.service;

import com.goldenpetiscaria.zeropapel.autenticacao.entity.RefreshToken;
import com.goldenpetiscaria.zeropapel.autenticacao.repository.RefreshTokenRepository;
import com.goldenpetiscaria.zeropapel.common.exception.TokenInvalidoException;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private static final String TOKEN = "a9f3c1d2-0000-4000-8000-000000000001";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl service;

    @Test
    @DisplayName("usuario desativado nao consegue renovar a sessao")
    void validarERetornarUsuario_lancaTokenInvalido_quandoUsuarioEstaDesativado() {
        RefreshToken token = refreshToken(usuario(false), LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validarERetornarUsuario(TOKEN))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void validarERetornarUsuario_apagaOToken_quandoUsuarioEstaDesativado() {
        RefreshToken token = refreshToken(usuario(false), LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validarERetornarUsuario(TOKEN))
                .isInstanceOf(TokenInvalidoException.class);

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void validarERetornarUsuario_retornaUsuario_quandoTokenValidoEUsuarioAtivo() {
        Usuario ativo = usuario(true);
        RefreshToken token = refreshToken(ativo, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(token));

        Usuario retornado = service.validarERetornarUsuario(TOKEN);

        assertThat(retornado).isSameAs(ativo);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void validarERetornarUsuario_lancaTokenInvalido_quandoTokenNaoExiste() {
        when(refreshTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validarERetornarUsuario(TOKEN))
                .isInstanceOf(TokenInvalidoException.class);

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void validarERetornarUsuario_apagaOTokenELanca_quandoExpirado() {
        RefreshToken expirado = refreshToken(usuario(true), LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service.validarERetornarUsuario(TOKEN))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessageContaining("expirado");

        verify(refreshTokenRepository).delete(expirado);
    }

    @Test
    @DisplayName("gerar revoga o token anterior antes de emitir o novo")
    void gerar_apagaOTokenAnteriorDoUsuario() {
        Usuario ativo = usuario(true);
        when(refreshTokenRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        String emitido = service.gerar(ativo);

        verify(refreshTokenRepository).deleteByUsuario(ativo);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(ativo);
        assertThat(captor.getValue().getExpiracao()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(emitido).isEqualTo(captor.getValue().getToken());
    }

    @Test
    void revogar_apagaOToken_quandoExiste() {
        RefreshToken token = refreshToken(usuario(true), LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(token));

        service.revogar(TOKEN);

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void revogar_naoFalha_quandoTokenNaoExiste() {
        when(refreshTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

        service.revogar(TOKEN);

        verify(refreshTokenRepository, never()).delete(any());
    }

    private RefreshToken refreshToken(Usuario usuario, LocalDateTime expiracao) {
        return new RefreshToken(TOKEN, usuario, expiracao);
    }

    private Usuario usuario(boolean ativo) {
        Usuario usuario = new Usuario("Wellington", "wellington", "hash", Cargo.OPERADOR);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
