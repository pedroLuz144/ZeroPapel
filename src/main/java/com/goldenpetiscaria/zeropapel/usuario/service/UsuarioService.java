package com.goldenpetiscaria.zeropapel.usuario.service;

import com.goldenpetiscaria.zeropapel.usuario.dto.request.AtualizarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.dto.request.CadastrarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.dto.response.UsuarioResponseDTO;

import java.util.List;

public interface UsuarioService {
    void cadastrarUsuario(CadastrarUsuarioRequest request);
    List<UsuarioResponseDTO> listarUsuarios();
    UsuarioResponseDTO atualizarUsuario(Long id, AtualizarUsuarioRequest request);
    void desativarUsuario(Long id);
    void ativarUsuario(Long id);
}
