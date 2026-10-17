package com.goldenpetiscaria.zeropapel.usuario.service;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.usuario.dto.request.AtualizarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.dto.request.CadastrarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.dto.response.UsuarioResponseDTO;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;
import com.goldenpetiscaria.zeropapel.usuario.enumerator.ResultadoUsuarioMaster;
import com.goldenpetiscaria.zeropapel.usuario.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            Validator validator
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
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
        if (request.cargo() != null && request.cargo() != usuario.getCargo()) {
            if (usuario.getCargo() == Cargo.GERENTE) {
                exigirOutroGerenteAtivo(usuario);
            }
            usuario.setCargo(request.cargo());
        }
        if (request.senha() != null) usuario.setSenha(passwordEncoder.encode(request.senha()));
        if (request.usuario() != null && !request.usuario().equals(usuario.getUsuario())) {
            if (usuarioRepository.findByUsuario(request.usuario()).isPresent()) {
                throw new ConflitoException("Já existe um usuário com o user: " + request.usuario());
            }
            usuario.setUsuario(request.usuario());
        }

        return toDTO(usuarioRepository.save(usuario));
    }

    @Override
    public void desativarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado para o ID informado"));
        if (usuario.getCargo() == Cargo.GERENTE) {
            exigirOutroGerenteAtivo(usuario);
        }
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuário id={} desativado", id);
    }

    @Override
    public void ativarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado para o ID informado"));
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
        log.info("Usuário id={} ativado", id);
    }

    @Override
    @Transactional
    public ResultadoUsuarioMaster criarUsuarioMasterSeNaoHouverUsuarios(String nome, String usuario, String senha) {
        if (usuarioRepository.count() > 0) {
            return ResultadoUsuarioMaster.JA_EXISTEM_USUARIOS;
        }
        if (emBranco(usuario) || emBranco(senha)) {
            return ResultadoUsuarioMaster.CREDENCIAIS_AUSENTES;
        }

        CadastrarUsuarioRequest request = new CadastrarUsuarioRequest(nome, usuario.strip(), senha, Cargo.GERENTE);
        Set<ConstraintViolation<CadastrarUsuarioRequest>> violacoes = validator.validate(request);
        if (!violacoes.isEmpty()) {
            List<String> motivos = violacoes.stream().map(ConstraintViolation::getMessage).sorted().toList();
            log.error("Usuário master recusado pelas regras do cadastro: {}", motivos);
            return ResultadoUsuarioMaster.CREDENCIAIS_INVALIDAS;
        }

        cadastrarUsuario(request);
        return ResultadoUsuarioMaster.CRIADO;
    }

    private boolean emBranco(String valor) {
        return valor == null || valor.isBlank();
    }

    private void exigirOutroGerenteAtivo(Usuario usuario) {
        if (!usuario.isAtivo()) {
            return;
        }
        if (usuarioRepository.countByCargoAndAtivoTrue(Cargo.GERENTE) <= 1) {
            throw new ConflitoException("Este é o único gerente ativo; promova outro usuário antes de alterá-lo");
        }
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
