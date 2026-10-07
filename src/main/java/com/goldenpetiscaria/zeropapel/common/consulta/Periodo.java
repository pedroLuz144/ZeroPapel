package com.goldenpetiscaria.zeropapel.common.consulta;

import com.goldenpetiscaria.zeropapel.common.exception.RequisicaoInvalidaException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Periodo(LocalDateTime de, LocalDateTime ate) {

    public static Periodo doDiaCorrenteSeAusente(LocalDateTime de, LocalDateTime ate, int limiteEmDias) {
        LocalDateTime inicio = de != null ? de : LocalDate.now().atStartOfDay();
        LocalDateTime fim = ate != null ? ate : LocalDateTime.now();

        if (fim.isBefore(inicio)) {
            throw new RequisicaoInvalidaException("O fim do período não pode ser anterior ao início");
        }
        if (Duration.between(inicio, fim).toDays() > limiteEmDias) {
            throw new RequisicaoInvalidaException(
                    "O período não pode passar de " + limiteEmDias + " dias");
        }
        return new Periodo(inicio, fim);
    }
}
