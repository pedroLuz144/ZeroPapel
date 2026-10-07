package com.goldenpetiscaria.zeropapel.security;

import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtFilterTest {

    private static final String TOKEN = "token-assinado";

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("usuario desativado nao autentica, mesmo com token valido e assinado")
    void doFilterInternal_naoAutentica_quandoUsuarioEstaDesativado() throws Exception {
        Usuario desativado = usuario(false);
        comHeaderBearer();
        when(jwtService.extrairUsuario(TOKEN)).thenReturn("wellington");
        when(userDetailsService.loadUserByUsername("wellington")).thenReturn(desativado);
        when(jwtService.tokenValido(TOKEN, desativado)).thenReturn(true);

        filtro().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_autentica_quandoUsuarioEstaAtivo() throws Exception {
        Usuario ativo = usuario(true);
        comHeaderBearer();
        when(jwtService.extrairUsuario(TOKEN)).thenReturn("wellington");
        when(userDetailsService.loadUserByUsername("wellington")).thenReturn(ativo);
        when(jwtService.tokenValido(TOKEN, ativo)).thenReturn(true);

        filtro().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(ativo);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_naoAutentica_quandoAssinaturaNaoConfere() throws Exception {
        Usuario ativo = usuario(true);
        comHeaderBearer();
        when(jwtService.extrairUsuario(TOKEN)).thenReturn("wellington");
        when(userDetailsService.loadUserByUsername("wellington")).thenReturn(ativo);
        when(jwtService.tokenValido(TOKEN, ativo)).thenReturn(false);

        filtro().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_seguePelaCadeia_quandoNaoHaHeaderAuthorization() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filtro().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtService, org.mockito.Mockito.never()).extrairUsuario(any());
    }

    @Test
    void doFilterInternal_seguePelaCadeia_quandoTokenEstaMalformado() throws Exception {
        comHeaderBearer();
        when(jwtService.extrairUsuario(TOKEN)).thenThrow(new RuntimeException("malformado"));

        filtro().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private JwtFilter filtro() {
        return new JwtFilter(jwtService, userDetailsService);
    }

    private void comHeaderBearer() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
    }

    private Usuario usuario(boolean ativo) {
        Usuario usuario = new Usuario("Wellington", "wellington", "hash", Cargo.OPERADOR);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
