package com.goldenpetiscaria.zeropapel.integracoes.ifood.service;

public interface IfoodIngestaoService {
    int receberEventos();
    void processarEventosPendentes();
}
