package com.goldenpetiscaria.zeropapel.integracoes.ifood.exception;

public class CredenciaisIfoodAusentesException extends IntegracaoIfoodException {

    public CredenciaisIfoodAusentesException() {
        super("Credenciais do iFood não configuradas: defina IFOOD_CLIENT_ID e IFOOD_CLIENT_SECRET");
    }
}
