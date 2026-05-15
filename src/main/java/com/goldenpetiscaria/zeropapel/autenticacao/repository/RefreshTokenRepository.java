package com.goldenpetiscaria.zeropapel.autenticacao.repository;

import com.goldenpetiscaria.zeropapel.autenticacao.entity.RefreshToken;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.usuario = :usuario")
    void deleteByUsuario(@Param("usuario") Usuario usuario);
}
