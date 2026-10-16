package com.goldenpetiscaria.zeropapel.canaldevenda.listener;

import com.goldenpetiscaria.zeropapel.canaldevenda.service.CanalDeVendaService;
import com.goldenpetiscaria.zeropapel.loja.evento.LojaCadastradaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CanalDeBalcaoListener {

    private final CanalDeVendaService canalDeVendaService;

    public CanalDeBalcaoListener(CanalDeVendaService canalDeVendaService) {
        this.canalDeVendaService = canalDeVendaService;
    }

    @EventListener
    public void aoCadastrarLoja(LojaCadastradaEvent evento) {
        canalDeVendaService.garantirCanalDeBalcao(evento.lojaId());
    }
}
