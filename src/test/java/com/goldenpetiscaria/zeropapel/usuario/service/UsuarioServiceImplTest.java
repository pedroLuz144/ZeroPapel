package com.goldenpetiscaria.zeropapel.usuario.service;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.usuario.dto.request.AtualizarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.dto.request.CadastrarUsuarioRequest;
import com.goldenpetiscaria.zeropapel.usuario.dto.response.UsuarioResponseDTO;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;
import com.goldenpetiscaria.zeropapel.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl service;

    @Nested
    class UltimoGerente {

        @Test
        @DisplayName("nao desativa o unico gerente ativo, para nao trancar o sistema")
        void desativarUsuario_lancaConflito_quandoEOUnicoGerenteAtivo() {
            Usuario gerente = usuario(Cargo.GERENTE, true);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(gerente));
            when(usuarioRepository.countByCargoAndAtivoTrue(Cargo.GERENTE)).thenReturn(1L);

            assertThatThrownBy(() -> service.desativarUsuario(1L))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessageContaining("único gerente");

            assertThat(gerente.isAtivo()).isTrue();
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        void desativarUsuario_desativa_quandoHaOutroGerenteAtivo() {
            Usuario gerente = usuario(Cargo.GERENTE, true);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(gerente));
            when(usuarioRepository.countByCargoAndAtivoTrue(Cargo.GERENTE)).thenReturn(2L);

            service.desativarUsuario(1L);

            assertThat(gerente.isAtivo()).isFalse();
            verify(usuarioRepository).save(gerente);
        }

        @Test
        void desativarUsuario_desativa_quandoUsuarioEOperador() {
            Usuario operador = usuario(Cargo.OPERADOR, true);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(operador));

            service.desativarUsuario(1L);

            assertThat(operador.isAtivo()).isFalse();
            verify(usuarioRepository, never()).countByCargoAndAtivoTrue(any());
        }

        @Test
        @DisplayName("nao rebaixa o unico gerente ativo para operador")
        void atualizarUsuario_lancaConflito_quandoRebaixaOUnicoGerenteAtivo() {
            Usuario gerente = usuario(Cargo.GERENTE, true);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(gerente));
            when(usuarioRepository.countByCargoAndAtivoTrue(Cargo.GERENTE)).thenReturn(1L);

            assertThatThrownBy(() -> service.atualizarUsuario(1L, pedido(null, null, Cargo.OPERADOR, null)))
                    .isInstanceOf(ConflitoException.class);

            assertThat(gerente.getCargo()).isEqualTo(Cargo.GERENTE);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        void atualizarUsuario_rebaixa_quandoHaOutroGerenteAtivo() {
            Usuario gerente = usuario(Cargo.GERENTE, true);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(gerente));
            when(usuarioRepository.countByCargoAndAtivoTrue(Cargo.GERENTE)).thenReturn(2L);
            when(usuarioRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarUsuario(1L, pedido(null, null, Cargo.OPERADOR, null));

            assertThat(gerente.getCargo()).isEqualTo(Cargo.OPERADOR);
        }

        @Test
        @DisplayName("reenviar o mesmo cargo do unico gerente nao e rebaixamento")
        void atualizarUsuario_naoValidaInvariante_quandoCargoNaoMuda() {
            Usuario gerente = usuario(Cargo.GERENTE, true);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(gerente));
            when(usuarioRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarUsuario(1L, pedido("Eliane Nova", null, Cargo.GERENTE, null));

            assertThat(gerente.getCargo()).isEqualTo(Cargo.GERENTE);
            assertThat(gerente.getNome()).isEqualTo("Eliane Nova");
            verify(usuarioRepository, never()).countByCargoAndAtivoTrue(any());
        }

        @Test
        void atualizarUsuario_promove_quandoUsuarioEraOperador() {
            Usuario operador = usuario(Cargo.OPERADOR, true);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(operador));
            when(usuarioRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarUsuario(1L, pedido(null, null, Cargo.GERENTE, null));

            assertThat(operador.getCargo()).isEqualTo(Cargo.GERENTE);
            verify(usuarioRepository, never()).countByCargoAndAtivoTrue(any());
        }
    }

    @Nested
    class Cadastro {

        @Test
        void cadastrarUsuario_salvaComSenhaCodificada() {
            when(usuarioRepository.findByUsuario("wellington")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("segredo1234")).thenReturn("hash-bcrypt");

            service.cadastrarUsuario(new CadastrarUsuarioRequest(
                    "Wellington", "wellington", "segredo1234", Cargo.OPERADOR));

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            Usuario salvo = captor.getValue();
            assertThat(salvo.getNome()).isEqualTo("Wellington");
            assertThat(salvo.getUsername()).isEqualTo("wellington");
            assertThat(salvo.getPassword()).isEqualTo("hash-bcrypt");
            assertThat(salvo.getCargo()).isEqualTo(Cargo.OPERADOR);
            assertThat(salvo.isAtivo()).isTrue();
        }

        @Test
        void cadastrarUsuario_lancaConflito_quandoLoginJaExiste() {
            when(usuarioRepository.findByUsuario("wellington"))
                    .thenReturn(Optional.of(usuario(Cargo.OPERADOR, true)));

            assertThatThrownBy(() -> service.cadastrarUsuario(new CadastrarUsuarioRequest(
                    "Outro", "wellington", "segredo1234", Cargo.OPERADOR)))
                    .isInstanceOf(ConflitoException.class);

            verify(usuarioRepository, never()).save(any());
        }
    }

    @Nested
    class AtualizacaoParcial {

        @Test
        @DisplayName("campo nulo preserva o valor anterior em vez de apagar")
        void atualizarUsuario_preservaCamposNaoInformados() {
            Usuario existente = usuario(Cargo.OPERADOR, true);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            UsuarioResponseDTO resposta = service.atualizarUsuario(1L, pedido(null, null, null, null));

            assertThat(resposta.nome()).isEqualTo("Wellington");
            assertThat(resposta.usuario()).isEqualTo("wellington");
            assertThat(resposta.cargo()).isEqualTo(Cargo.OPERADOR);
            assertThat(existente.getPassword()).isEqualTo("hash-antigo");
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void atualizarUsuario_codificaASenha_quandoInformada() {
            Usuario existente = usuario(Cargo.OPERADOR, true);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("hash-novo");
            when(usuarioRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            service.atualizarUsuario(1L, pedido(null, null, null, "novaSenha123"));

            assertThat(existente.getPassword()).isEqualTo("hash-novo");
        }

        @Test
        void atualizarUsuario_lancaConflito_quandoNovoLoginJaPertenceAOutro() {
            Usuario existente = usuario(Cargo.OPERADOR, true);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.findByUsuario("eliane"))
                    .thenReturn(Optional.of(usuario(Cargo.GERENTE, true)));

            assertThatThrownBy(() -> service.atualizarUsuario(1L, pedido(null, "eliane", null, null)))
                    .isInstanceOf(ConflitoException.class);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        void atualizarUsuario_lancaRecursoNaoEncontrado_quandoIdNaoExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizarUsuario(99L, pedido("Nome", null, null, null)))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Nested
    class Ativacao {

        @Test
        void ativarUsuario_marcaComoAtivo() {
            Usuario inativo = usuario(Cargo.OPERADOR, false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(inativo));

            service.ativarUsuario(1L);

            assertThat(inativo.isAtivo()).isTrue();
            verify(usuarioRepository).save(inativo);
        }

        @Test
        void desativarUsuario_lancaRecursoNaoEncontrado_quandoIdNaoExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.desativarUsuario(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    private AtualizarUsuarioRequest pedido(String nome, String login, Cargo cargo, String senha) {
        return new AtualizarUsuarioRequest(nome, login, cargo, senha);
    }

    private Usuario usuario(Cargo cargo, boolean ativo) {
        Usuario usuario = new Usuario("Wellington", "wellington", "hash-antigo", cargo);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
