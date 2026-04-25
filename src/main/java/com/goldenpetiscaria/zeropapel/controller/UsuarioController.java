package com.goldenpetiscaria.zeropapel.controller;

import com.goldenpetiscaria.zeropapel.domain.service.UsuarioService;
import com.goldenpetiscaria.zeropapel.dto.request.CadastrarUsuarioRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(
            UsuarioService usuarioService
    ) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid CadastrarUsuarioRequest request) {
        usuarioService.cadastrarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
