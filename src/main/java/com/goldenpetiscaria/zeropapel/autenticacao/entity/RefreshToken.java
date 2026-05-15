package com.goldenpetiscaria.zeropapel.autenticacao.entity;

import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private LocalDateTime expiracao;

    public RefreshToken(String token, Usuario usuario, LocalDateTime expiracao) {
        this.token = token;
        this.usuario = usuario;
        this.expiracao = expiracao;
    }

    public boolean expirado() {
        return LocalDateTime.now().isAfter(this.expiracao);
    }
}
