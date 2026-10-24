package com.goldenpetiscaria.zeropapel.usuario.inicializacao;

import com.goldenpetiscaria.zeropapel.usuario.enumerator.ResultadoUsuarioMaster;
import com.goldenpetiscaria.zeropapel.usuario.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InicializadorUsuarioMasterTest {

    @Mock
    private UsuarioService usuarioService;

    @Test
    @DisplayName("repassa login, senha e nome configurados para o service, uma unica vez")
    void run_delegaParaOService_comOsValoresConfigurados() {
        when(usuarioService.criarUsuarioMasterSeNaoHouverUsuarios(any(), any(), any()))
                .thenReturn(ResultadoUsuarioMaster.CRIADO);

        inicializador("admin", "senha-forte-123", "Eliane").run(new DefaultApplicationArguments());

        verify(usuarioService).criarUsuarioMasterSeNaoHouverUsuarios("Eliane", "admin", "senha-forte-123");
        verifyNoMoreInteractions(usuarioService);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("nome ausente ou em branco vira o nome padrao")
    void run_usaNomePadrao_quandoNomeAusenteOuEmBranco(String nome) {
        when(usuarioService.criarUsuarioMasterSeNaoHouverUsuarios(any(), any(), any()))
                .thenReturn(ResultadoUsuarioMaster.CRIADO);

        inicializador("admin", "senha-forte-123", nome).run(new DefaultApplicationArguments());

        verify(usuarioService).criarUsuarioMasterSeNaoHouverUsuarios(
                InicializadorUsuarioMaster.NOME_PADRAO, "admin", "senha-forte-123");
    }

    @Test
    @DisplayName("sem variaveis definidas ainda consulta o service, que decide pela tabela")
    void run_delegaCredenciaisVazias_quandoVariaveisNaoDefinidas() {
        when(usuarioService.criarUsuarioMasterSeNaoHouverUsuarios(any(), any(), any()))
                .thenReturn(ResultadoUsuarioMaster.CREDENCIAIS_AUSENTES);

        inicializador("", "", "").run(new DefaultApplicationArguments());

        verify(usuarioService).criarUsuarioMasterSeNaoHouverUsuarios(
                InicializadorUsuarioMaster.NOME_PADRAO, "", "");
    }

    @ParameterizedTest
    @EnumSource(ResultadoUsuarioMaster.class)
    @DisplayName("nenhum resultado derruba a subida")
    void run_naoLancaExcecao_paraNenhumResultado(ResultadoUsuarioMaster resultado) {
        when(usuarioService.criarUsuarioMasterSeNaoHouverUsuarios(any(), any(), any())).thenReturn(resultado);

        assertThatCode(() -> inicializador("admin", "abc", "").run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }

    private InicializadorUsuarioMaster inicializador(String usuario, String senha, String nome) {
        return new InicializadorUsuarioMaster(usuarioService, usuario, senha, nome);
    }
}
