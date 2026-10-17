package com.goldenpetiscaria.zeropapel.autenticacao.service;

import com.goldenpetiscaria.zeropapel.common.exception.MuitasTentativasException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.goldenpetiscaria.zeropapel.autenticacao.service.LimiteDeTentativasDeLoginServiceImpl.JANELA;
import static com.goldenpetiscaria.zeropapel.autenticacao.service.LimiteDeTentativasDeLoginServiceImpl.MAXIMO_DE_LOGINS_ACOMPANHADOS;
import static com.goldenpetiscaria.zeropapel.autenticacao.service.LimiteDeTentativasDeLoginServiceImpl.MAXIMO_DE_TENTATIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimiteDeTentativasDeLoginServiceImplTest {

    private RelogioAjustavel relogio;
    private LimiteDeTentativasDeLoginServiceImpl service;

    @BeforeEach
    void preparar() {
        relogio = new RelogioAjustavel(Instant.parse("2026-10-16T22:00:00Z"));
        service = new LimiteDeTentativasDeLoginServiceImpl(relogio);
    }

    @Test
    @DisplayName("aceita ate o maximo de tentativas na janela")
    void registrarTentativa_aceita_ateOMaximo() {
        assertThatCode(() -> tentar("eliane", MAXIMO_DE_TENTATIVAS)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("recusa a tentativa seguinte ao maximo, com o tempo de espera")
    void registrarTentativa_recusa_depoisDoMaximo() {
        tentar("eliane", MAXIMO_DE_TENTATIVAS);
        relogio.avancar(Duration.ofMinutes(5));

        assertThatThrownBy(() -> service.registrarTentativa("eliane"))
                .isInstanceOf(MuitasTentativasException.class)
                .hasMessageContaining("10 min")
                .extracting("espera")
                .isEqualTo(JANELA.minusMinutes(5));
    }

    @Test
    @DisplayName("tentativa recusada nao estende o bloqueio")
    void registrarTentativa_recusada_naoEstendeOBloqueio() {
        tentar("eliane", MAXIMO_DE_TENTATIVAS);
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.registrarTentativa("eliane"))
                    .isInstanceOf(MuitasTentativasException.class);
        }

        relogio.avancar(JANELA);

        assertThatCode(() -> service.registrarTentativa("eliane")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("libera de novo quando a janela termina")
    void registrarTentativa_libera_quandoAJanelaTermina() {
        tentar("eliane", MAXIMO_DE_TENTATIVAS);
        relogio.avancar(JANELA);

        assertThatCode(() -> tentar("eliane", MAXIMO_DE_TENTATIVAS)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("login com sucesso zera as tentativas")
    void registrarSucesso_zeraAsTentativas() {
        tentar("eliane", MAXIMO_DE_TENTATIVAS - 1);
        service.registrarSucesso("eliane");

        assertThatCode(() -> tentar("eliane", MAXIMO_DE_TENTATIVAS)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("um login bloqueado nao bloqueia outro")
    void registrarTentativa_contaPorLogin() {
        tentar("eliane", MAXIMO_DE_TENTATIVAS);

        assertThatCode(() -> service.registrarTentativa("carlos")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("maiusculas e espacos nao contornam o limite")
    void registrarTentativa_normalizaOLogin() {
        tentar("eliane", MAXIMO_DE_TENTATIVAS);

        assertThatThrownBy(() -> service.registrarTentativa("  ELIANE "))
                .isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    @DisplayName("acento, largura total, maiusculas e espacos caem no mesmo contador")
    void registrarTentativa_normalizaAcentoELarguraTotal() {
        service.registrarTentativa("admin");
        service.registrarTentativa("ádmin");
        service.registrarTentativa("ＡＤＭＩＮ");
        service.registrarTentativa(" ÀDMÍN ");
        service.registrarTentativa("a\u0301dmin");

        assertThat(service.loginsAcompanhados()).isEqualTo(1);
        assertThatThrownBy(() -> service.registrarTentativa("Ａｄｍｉｎ"))
                .isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    @DisplayName("a chave canonica tira acento, largura total, caixa e espacos")
    void chave_ehCanonica() {
        assertThat(LimiteDeTentativasDeLoginServiceImpl.chave("  ＥＬＩＡＮＥ ")).isEqualTo("eliane");
        assertThat(LimiteDeTentativasDeLoginServiceImpl.chave("Conceição")).isEqualTo("conceicao");
        assertThat(LimiteDeTentativasDeLoginServiceImpl.chave(null)).isEmpty();
    }

    @Test
    @DisplayName("cheio de logins na janela, recusa login novo com 429 sem acompanhar alem do limite")
    void registrarTentativa_recusaLoginNovo_quandoCheio() {
        encher();
        relogio.avancar(Duration.ofMinutes(5));

        assertThatThrownBy(() -> service.registrarTentativa("novo"))
                .isInstanceOf(MuitasTentativasException.class)
                .extracting("espera")
                .isEqualTo(JANELA.minusMinutes(5));
        assertThat(service.loginsAcompanhados()).isEqualTo(MAXIMO_DE_LOGINS_ACOMPANHADOS);
    }

    @Test
    @DisplayName("encher o mapa com nomes inventados nao descarta nem zera o contador de um login real")
    void registrarTentativa_encherNaoExpulsaLoginReal() {
        tentar("eliane", MAXIMO_DE_TENTATIVAS);
        for (int i = 1; i < MAXIMO_DE_LOGINS_ACOMPANHADOS; i++) {
            service.registrarTentativa("inventado-" + i);
        }
        for (int i = 0; i < 100; i++) {
            int indice = i;
            assertThatThrownBy(() -> service.registrarTentativa("outro-" + indice))
                    .isInstanceOf(MuitasTentativasException.class);
        }

        assertThatThrownBy(() -> service.registrarTentativa("eliane"))
                .isInstanceOf(MuitasTentativasException.class);
        assertThat(service.loginsAcompanhados()).isEqualTo(MAXIMO_DE_LOGINS_ACOMPANHADOS);
    }

    @Test
    @DisplayName("cheio, login ja acompanhado continua sendo contado")
    void registrarTentativa_cheio_continuaContandoLoginAcompanhado() {
        service.registrarTentativa("eliane");
        for (int i = 1; i < MAXIMO_DE_LOGINS_ACOMPANHADOS; i++) {
            service.registrarTentativa("inventado-" + i);
        }

        tentar("eliane", MAXIMO_DE_TENTATIVAS - 1);

        assertThatThrownBy(() -> service.registrarTentativa("eliane"))
                .isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    @DisplayName("cheio, abre espaco para login novo assim que alguma janela vence")
    void registrarTentativa_cheio_aceitaLoginNovoQuandoAlgumaJanelaVence() {
        service.registrarTentativa("antigo");
        relogio.avancar(Duration.ofMinutes(1));
        for (int i = 1; i < MAXIMO_DE_LOGINS_ACOMPANHADOS; i++) {
            service.registrarTentativa("inventado-" + i);
        }
        relogio.avancar(JANELA.minusMinutes(1));

        assertThatCode(() -> service.registrarTentativa("novo")).doesNotThrowAnyException();
        assertThat(service.loginsAcompanhados()).isEqualTo(MAXIMO_DE_LOGINS_ACOMPANHADOS);
    }

    @Test
    @DisplayName("ao encher, descarta primeiro os logins com a janela ja vencida")
    void registrarTentativa_descartaVencidosAoEncher() {
        encher();
        relogio.avancar(JANELA);

        service.registrarTentativa("novo");

        assertThat(service.loginsAcompanhados()).isEqualTo(1);
    }

    private void encher() {
        for (int i = 0; i < MAXIMO_DE_LOGINS_ACOMPANHADOS; i++) {
            service.registrarTentativa("login-" + i);
        }
    }

    private void tentar(String login, int vezes) {
        for (int i = 0; i < vezes; i++) {
            service.registrarTentativa(login);
        }
    }

    private static final class RelogioAjustavel extends Clock {

        private Instant agora;

        private RelogioAjustavel(Instant agora) {
            this.agora = agora;
        }

        private void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
