package com.goldenpetiscaria.zeropapel.controller;

import com.goldenpetiscaria.zeropapel.domain.entity.Usuario;
import com.goldenpetiscaria.zeropapel.domain.service.contratos.RefreshTokenService;
import com.goldenpetiscaria.zeropapel.dto.request.LoginRequest;
import com.goldenpetiscaria.zeropapel.dto.request.RefreshRequest;
import com.goldenpetiscaria.zeropapel.dto.response.LoginResponse;
import com.goldenpetiscaria.zeropapel.infra.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            RefreshTokenService refreshTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.usuario(),
                        request.senha()
                )
        );

        Usuario usuario = (Usuario) userDetailsService.loadUserByUsername(request.usuario());
        String token = jwtService.gerarToken(usuario);
        String refreshToken = refreshTokenService.gerar(usuario);

        return ResponseEntity.ok(new LoginResponse(token, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        Usuario usuario = refreshTokenService.validarERetornarUsuario(request.refreshToken());
        String novoAccessToken = jwtService.gerarToken(usuario);
        String novoRefreshToken = refreshTokenService.gerar(usuario);

        return ResponseEntity.ok(new LoginResponse(novoAccessToken, novoRefreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshRequest request) {
        refreshTokenService.revogar(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
