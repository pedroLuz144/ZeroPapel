package com.goldenpetiscaria.zeropapel.integracoes.ifood.exception;

public class IntegracaoIfoodException extends RuntimeException {

    private final Integer statusHttp;

    public IntegracaoIfoodException(String mensagem) {
        this(mensagem, null);
    }

    public IntegracaoIfoodException(String mensagem, Integer statusHttp) {
        super(mensagem);
        this.statusHttp = statusHttp;
    }

    public Integer getStatusHttp() {
        return statusHttp;
    }
}
