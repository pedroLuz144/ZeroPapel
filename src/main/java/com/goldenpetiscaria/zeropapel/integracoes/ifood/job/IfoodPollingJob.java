package com.goldenpetiscaria.zeropapel.integracoes.ifood.job;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.service.IfoodIngestaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "ifood.polling.ativo", havingValue = "true")
public class IfoodPollingJob {

    private final IfoodIngestaoService ingestaoService;

    public IfoodPollingJob(IfoodIngestaoService ingestaoService) {
        this.ingestaoService = ingestaoService;
    }

    @Scheduled(fixedDelayString = "${ifood.polling.intervalo-ms:30000}")
    public void executar() {
        try {
            int novos = ingestaoService.receberEventos();
            if (novos > 0) {
                log.info("{} evento(s) novo(s) do iFood recebido(s)", novos);
            }
        } catch (RuntimeException e) {
            log.warn("Polling do iFood não concluído: {}", e.getMessage());
        }
        try {
            ingestaoService.processarEventosPendentes();
        } catch (RuntimeException e) {
            log.warn("Processamento dos eventos do iFood não concluído: {}", e.getMessage());
        }
    }
}
