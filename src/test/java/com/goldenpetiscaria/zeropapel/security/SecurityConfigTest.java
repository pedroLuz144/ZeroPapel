package com.goldenpetiscaria.zeropapel.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    @DisplayName("a CSP so libera a propria origem e nao abre excecao para script ou style inline")
    void politicaDeConteudo_restringeAPropriaOrigem() {
        assertThat(SecurityConfig.POLITICA_DE_CONTEUDO)
                .startsWith("default-src 'self'")
                .contains("img-src 'self' data:", "object-src 'none'", "frame-ancestors 'none'", "base-uri 'self'")
                .doesNotContain("unsafe-inline", "unsafe-eval", "*");
    }

    @Test
    void hsts_valeUmAno() {
        assertThat(SecurityConfig.VALIDADE_DO_HSTS_EM_SEGUNDOS).isEqualTo(365L * 24 * 60 * 60);
    }
}
