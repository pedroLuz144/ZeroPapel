package com.goldenpetiscaria.zeropapel.autenticacao.service;

import com.goldenpetiscaria.zeropapel.autenticacao.entity.RefreshToken;
import com.goldenpetiscaria.zeropapel.autenticacao.repository.RefreshTokenRepository;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.common.exception.TokenInvalidoException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public String gerar(Usuario usuario) {
        log.info("Gerando refresh token para usuário id={}", usuario.getId());
        refreshTokenRepository.deleteByUsuario(usuario);

        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID().toString(),
                usuario,
                LocalDateTime.now().plusDays(7)
        );

        String token = refreshTokenRepository.save(refreshToken).getToken();
        log.info("Refresh token gerado para usuário id={}", usuario.getId());
        return token;
    }

    @Transactional
    public Usuario validarERetornarUsuario(String token) {
        log.debug("Validando refresh token");
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenInvalidoException("Refresh token inválido"));

        if (refreshToken.expirado()) {
            log.warn("Refresh token expirado para usuário id={}", refreshToken.getUsuario().getId());
            refreshTokenRepository.delete(refreshToken);
            throw new TokenInvalidoException("Refresh token expirado, faça login novamente");
        }

        log.debug("Refresh token válido para usuário id={}", refreshToken.getUsuario().getId());
        return refreshToken.getUsuario();
    }

    @Transactional
    public void revogar(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            log.info("Revogando refresh token do usuário id={}", rt.getUsuario().getId());
            refreshTokenRepository.delete(rt);
        });
    }
}
