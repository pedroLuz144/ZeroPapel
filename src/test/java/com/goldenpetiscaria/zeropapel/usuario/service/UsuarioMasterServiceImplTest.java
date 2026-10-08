package com.goldenpetiscaria.zeropapel.usuario.service;

import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;
import com.goldenpetiscaria.zeropapel.usuario.enumerator.ResultadoUsuarioMaster;
import com.goldenpetiscaria.zeropapel.usuario.repository.UsuarioRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioMasterServiceImplTest {

    private static ValidatorFactory fabricaDeValidador;
    private static Validator validator;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioServiceImpl service;

    @BeforeAll
    static void abrirValidador() {
        fabricaDeValidador = Validation.buildDefaultValidatorFactory();
        validator = fabricaDeValidador.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        fabricaDeValidador.close();
    }

    @BeforeEach
    void montarService() {
        service = new UsuarioServiceImpl(usuarioRepository, passwordEncoder, validator);
    }

    @Test
    @DisplayName("tabela vazia com login e senha definidos cria um gerente ativo com a senha codificada")
    void criarUsuarioMaster_criaGerenteAtivo_quandoTabelaVaziaECredenciaisDefinidas() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(usuarioRepository.findByUsuario("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha-forte-123")).thenReturn("hash-bcrypt");

        ResultadoUsuarioMaster resultado =
                service.criarUsuarioMasterSeNaoHouverUsuarios("Administrador", "admin", "senha-forte-123");

        assertThat(resultado).isEqualTo(ResultadoUsuarioMaster.CRIADO);
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Administrador");
        assertThat(salvo.getUsername()).isEqualTo("admin");
        assertThat(salvo.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(salvo.getCargo()).isEqualTo(Cargo.GERENTE);
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("login com espaco nas pontas e gravado sem o espaco")
    void criarUsuarioMaster_aparaOLogin() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(usuarioRepository.findByUsuario("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha-forte-123")).thenReturn("hash-bcrypt");

        service.criarUsuarioMasterSeNaoHouverUsuarios("Administrador", "  admin ", "senha-forte-123");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("com qualquer usuario cadastrado nao cria nem consulta mais nada")
    void criarUsuarioMaster_naoFazNada_quandoTabelaNaoEstaVazia() {
        when(usuarioRepository.count()).thenReturn(1L);

        ResultadoUsuarioMaster resultado =
                service.criarUsuarioMasterSeNaoHouverUsuarios("Administrador", "admin", "senha-forte-123");

        assertThat(resultado).isEqualTo(ResultadoUsuarioMaster.JA_EXISTEM_USUARIOS);
        verify(usuarioRepository).count();
        verifyNoMoreInteractions(usuarioRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("tabela nao vazia vence credencial ausente: nao ha o que avisar")
    void criarUsuarioMaster_devolveJaExistemUsuarios_mesmoSemCredenciais() {
        when(usuarioRepository.count()).thenReturn(3L);

        ResultadoUsuarioMaster resultado = service.criarUsuarioMasterSeNaoHouverUsuarios("Administrador", "", "");

        assertThat(resultado).isEqualTo(ResultadoUsuarioMaster.JA_EXISTEM_USUARIOS);
        verifyNoMoreInteractions(usuarioRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("login ausente ou em branco nao cria")
    void criarUsuarioMaster_naoCria_quandoLoginAusenteOuEmBranco(String login) {
        when(usuarioRepository.count()).thenReturn(0L);

        ResultadoUsuarioMaster resultado =
                service.criarUsuarioMasterSeNaoHouverUsuarios("Administrador", login, "senha-forte-123");

        assertThat(resultado).isEqualTo(ResultadoUsuarioMaster.CREDENCIAIS_AUSENTES);
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("senha ausente ou em branco nao cria")
    void criarUsuarioMaster_naoCria_quandoSenhaAusenteOuEmBranco(String senha) {
        when(usuarioRepository.count()).thenReturn(0L);

        ResultadoUsuarioMaster resultado =
                service.criarUsuarioMasterSeNaoHouverUsuarios("Administrador", "admin", senha);

        assertThat(resultado).isEqualTo(ResultadoUsuarioMaster.CREDENCIAIS_AUSENTES);
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("senha abaixo do minimo do cadastro nao cria")
    void criarUsuarioMaster_naoCria_quandoSenhaForaDaRegraDoCadastro() {
        when(usuarioRepository.count()).thenReturn(0L);

        ResultadoUsuarioMaster resultado =
                service.criarUsuarioMasterSeNaoHouverUsuarios("Administrador", "admin", "abc");

        assertThat(resultado).isEqualTo(ResultadoUsuarioMaster.CREDENCIAIS_INVALIDAS);
        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).findByUsuario(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("senha exatamente no minimo do cadastro cria")
    void criarUsuarioMaster_cria_quandoSenhaTemOTamanhoMinimo() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(usuarioRepository.findByUsuario("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("abcd")).thenReturn("hash-bcrypt");

        ResultadoUsuarioMaster resultado =
                service.criarUsuarioMasterSeNaoHouverUsuarios("Administrador", "admin", "abcd");

        assertThat(resultado).isEqualTo(ResultadoUsuarioMaster.CRIADO);
        verify(usuarioRepository).save(any());
    }
}
