package com.goldenpetiscaria.zeropapel.usuario.controller;

import com.goldenpetiscaria.zeropapel.usuario.dto.request.AtualizarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.dto.request.CadastrarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.dto.response.UsuarioResponseDTO;
import com.goldenpetiscaria.zeropapel.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GERENTE')")
    public void cadastrar(@RequestBody @Valid CadastrarUsuarioRequest request) {
        usuarioService.cadastrarUsuario(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('GERENTE')")
    public List<UsuarioResponseDTO> listar() {
        return usuarioService.listarUsuarios();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public UsuarioResponseDTO atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarUsuarioRequest request) {
        return usuarioService.atualizarUsuario(id, request);
    }

    @PatchMapping("/{id}/desativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GERENTE')")
    public void desativar(@PathVariable Long id) {
        usuarioService.desativarUsuario(id);
    }

    @PatchMapping("/{id}/ativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GERENTE')")
    public void ativar(@PathVariable Long id) {
        usuarioService.ativarUsuario(id);
    }
}
