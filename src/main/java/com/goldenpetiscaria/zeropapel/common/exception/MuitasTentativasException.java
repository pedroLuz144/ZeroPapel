package com.goldenpetiscaria.zeropapel.common.exception;

import lombok.Getter;

import java.time.Duration;

@Getter
public class MuitasTentativasException extends RuntimeException {

    private final Duration espera;

    public MuitasTentativasException(Duration espera) {
        super("Muitas tentativas de login. Tente novamente em " + minutos(espera) + " min.");
        this.espera = espera;
    }

    private static long minutos(Duration espera) {
        return Math.max(1, (espera.toSeconds() + 59) / 60);
    }
}
