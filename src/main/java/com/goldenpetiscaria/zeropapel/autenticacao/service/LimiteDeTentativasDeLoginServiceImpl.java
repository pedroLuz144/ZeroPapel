package com.goldenpetiscaria.zeropapel.autenticacao.service;

import com.goldenpetiscaria.zeropapel.common.exception.MuitasTentativasException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LimiteDeTentativasDeLoginServiceImpl implements LimiteDeTentativasDeLoginService {

    static final int MAXIMO_DE_TENTATIVAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);
    static final int MAXIMO_DE_LOGINS_ACOMPANHADOS = 10_000;
    private static final int TAMANHO_MAXIMO_DA_CHAVE = 100;
    private static final Pattern MARCAS_COMBINANTES = Pattern.compile("\\p{M}+");
    private static final Duration INTERVALO_ENTRE_AVISOS_DE_LOTACAO = Duration.ofMinutes(1);

    private final Map<String, Tentativas> tentativasPorLogin = new HashMap<>();
    private final Clock relogio;
    private Instant ultimoAvisoDeLotacao;

    public LimiteDeTentativasDeLoginServiceImpl() {
        this(Clock.systemUTC());
    }

    LimiteDeTentativasDeLoginServiceImpl(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    public synchronized void registrarTentativa(String login) {
        Instant agora = relogio.instant();
        String chave = chave(login);
        garantirEspaco(chave, agora);
        Tentativas registradas = tentativasPorLogin.compute(chave, (k, atual) -> somarTentativa(atual, agora));
        if (registradas.quantidade() == MAXIMO_DE_TENTATIVAS) {
            log.warn("Login bloqueado por {} min depois de {} tentativas sem sucesso",
                    JANELA.toMinutes(), MAXIMO_DE_TENTATIVAS);
        }
    }

    @Override
    public synchronized void registrarSucesso(String login) {
        tentativasPorLogin.remove(chave(login));
    }

    synchronized int loginsAcompanhados() {
        return tentativasPorLogin.size();
    }

    private Tentativas somarTentativa(Tentativas atual, Instant agora) {
        if (atual == null || atual.expirou(agora)) {
            return new Tentativas(1, agora.plus(JANELA));
        }
        if (atual.quantidade() >= MAXIMO_DE_TENTATIVAS) {
            throw new MuitasTentativasException(Duration.between(agora, atual.liberaEm()));
        }
        return new Tentativas(atual.quantidade() + 1, atual.liberaEm());
    }

    private void garantirEspaco(String chave, Instant agora) {
        if (tentativasPorLogin.size() < MAXIMO_DE_LOGINS_ACOMPANHADOS || tentativasPorLogin.containsKey(chave)) {
            return;
        }
        tentativasPorLogin.values().removeIf(t -> t.expirou(agora));
        if (tentativasPorLogin.size() >= MAXIMO_DE_LOGINS_ACOMPANHADOS) {
            Instant primeiraLiberacao = tentativasPorLogin.values().stream()
                    .map(Tentativas::liberaEm)
                    .min(Instant::compareTo)
                    .orElse(agora.plus(JANELA));
            avisarLotacao(agora);
            throw new MuitasTentativasException(Duration.between(agora, primeiraLiberacao));
        }
    }

    private void avisarLotacao(Instant agora) {
        if (ultimoAvisoDeLotacao != null && agora.isBefore(ultimoAvisoDeLotacao.plus(INTERVALO_ENTRE_AVISOS_DE_LOTACAO))) {
            return;
        }
        ultimoAvisoDeLotacao = agora;
        log.warn("Limite de {} logins acompanhados atingido; tentativas de login novo recusadas",
                MAXIMO_DE_LOGINS_ACOMPANHADOS);
    }

    static String chave(String login) {
        if (login == null) {
            return "";
        }
        String compatibilizado = Normalizer.normalize(login, Normalizer.Form.NFKC);
        String semAcento = MARCAS_COMBINANTES
                .matcher(Normalizer.normalize(compatibilizado, Normalizer.Form.NFD))
                .replaceAll("");
        String normalizado = semAcento.toLowerCase(Locale.ROOT).strip();
        return normalizado.length() > TAMANHO_MAXIMO_DA_CHAVE
                ? normalizado.substring(0, TAMANHO_MAXIMO_DA_CHAVE)
                : normalizado;
    }

    private record Tentativas(int quantidade, Instant liberaEm) {

        private boolean expirou(Instant agora) {
            return !agora.isBefore(liberaEm);
        }
    }
}
