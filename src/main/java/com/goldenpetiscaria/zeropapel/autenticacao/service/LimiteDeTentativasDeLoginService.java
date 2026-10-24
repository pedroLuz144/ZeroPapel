package com.goldenpetiscaria.zeropapel.autenticacao.service;

public interface LimiteDeTentativasDeLoginService {
    void registrarTentativa(String login);
    void registrarSucesso(String login);
}
