package com.goldenpetiscaria.zeropapel.autenticacao.service;

import com.goldenpetiscaria.zeropapel.common.exception.MuitasTentativasException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LimiteDeTentativasDeLoginServiceImpl implements LimiteDeTentativasDeLoginService {

    static final int MAXIMO_DE_TENTATIVAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);
    static final int MAXIMO_DE_LOGINS_ACOMPANHADOS = 10_000;
    private static final int TAMANHO_MAXIMO_DA_CHAVE = 100;

    private final Map<String, Tentativas> tentativasPorLogin = new ConcurrentHashMap<>();
    private final Clock relogio;

    public LimiteDeTentativasDeLoginServiceImpl() {
        this(Clock.systemUTC());
    }

    LimiteDeTentativasDeLoginServiceImpl(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    public void registrarTentativa(String login) {
        Instant agora = relogio.instant();
        String chave = chave(login);
        abrirEspacoSeNecessario(chave, agora);
        Tentativas registradas = tentativasPorLogin.compute(chave, (k, atual) -> somarTentativa(atual, agora));
        if (registradas.quantidade() == MAXIMO_DE_TENTATIVAS) {
            log.warn("Login bloqueado por {} min depois de {} tentativas sem sucesso",
                    JANELA.toMinutes(), MAXIMO_DE_TENTATIVAS);
        }
    }

    @Override
    public void registrarSucesso(String login) {
        tentativasPorLogin.remove(chave(login));
    }

    int loginsAcompanhados() {
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

    private void abrirEspacoSeNecessario(String chave, Instant agora) {
        if (tentativasPorLogin.size() < MAXIMO_DE_LOGINS_ACOMPANHADOS || tentativasPorLogin.containsKey(chave)) {
            return;
        }
        tentativasPorLogin.values().removeIf(t -> t.expirou(agora));
        while (tentativasPorLogin.size() >= MAXIMO_DE_LOGINS_ACOMPANHADOS) {
            tentativasPorLogin.entrySet().stream()
                    .min(Comparator.comparing(e -> e.getValue().liberaEm()))
                    .ifPresent(e -> tentativasPorLogin.remove(e.getKey(), e.getValue()));
        }
    }

    private static String chave(String login) {
        String normalizado = login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
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
