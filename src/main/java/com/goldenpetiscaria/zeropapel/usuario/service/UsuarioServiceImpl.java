package com.goldenpetiscaria.zeropapel.usuario.service;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.usuario.dto.request.CadastrarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.usuario.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
}
