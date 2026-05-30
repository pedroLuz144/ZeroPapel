package com.goldenpetiscaria.zeropapel.usuario.service;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.usuario.dto.request.AtualizarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.dto.request.CadastrarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.dto.response.UsuarioResponseDTO;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.usuario.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void cadastrarUsuario(CadastrarUsuarioRequest request) {
        log.info("Cadastrando usuário={} cargo={}", request.usuario(), request.cargo());
        if (usuarioRepository.findByUsuario(request.usuario()).isPresent()) {
            throw new ConflitoException("Já existe um usuário com o user: " + request.usuario());
        }

        Usuario usuario = new Usuario(
                request.nome(),
                request.usuario(),
                passwordEncoder.encode(request.senha()),
                request.cargo()
        );

        usuarioRepository.save(usuario);
        log.info("Usuário cadastrado nome={} cargo={}", request.nome(), request.cargo());
    }

    @Override
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public UsuarioResponseDTO atualizarUsuario(Long id, AtualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado para o ID informado"));

        if (request.nome() != null) usuario.setNome(request.nome());
        if (request.cargo() != null) usuario.setCargo(request.cargo());
        if (request.senha() != null) usuario.setSenha(passwordEncoder.encode(request.senha()));

        return toDTO(usuarioRepository.save(usuario));
    }

    @Override
    public void desativarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado para o ID informado"));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuário id={} desativado", id);
    }

    private UsuarioResponseDTO toDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getUsername(),
                usuario.getCargo(),
                usuario.isAtivo()
        );
    }
}
