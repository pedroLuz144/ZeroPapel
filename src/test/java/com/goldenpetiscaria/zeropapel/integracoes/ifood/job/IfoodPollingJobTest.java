package com.goldenpetiscaria.zeropapel.integracoes.ifood.job;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.service.IfoodIngestaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IfoodPollingJobTest {

    @Mock
    private IfoodIngestaoService ingestaoService;

    @InjectMocks
    private IfoodPollingJob job;

    @Test
    void executar_recebeOsEventosEDepoisProcessaOsPendentes() {
        when(ingestaoService.receberEventos()).thenReturn(2);

        job.executar();

        InOrder ordem = inOrder(ingestaoService);
        ordem.verify(ingestaoService).receberEventos();
        ordem.verify(ingestaoService).processarEventosPendentes();
    }

    @Test
    @DisplayName("polling com falha nao impede o processamento do que ja esta na inbox")
    void executar_processaOsPendentes_mesmoQuandoOPollingFalha() {
        when(ingestaoService.receberEventos()).thenThrow(new IntegracaoIfoodException("fora do ar", 503));

        assertThatCode(job::executar).doesNotThrowAnyException();

        verify(ingestaoService).processarEventosPendentes();
    }

    @Test
    void executar_naoPropagaFalhaDoProcessamento() {
        doThrow(new IllegalStateException("banco fora")).when(ingestaoService).processarEventosPendentes();

        assertThatCode(job::executar).doesNotThrowAnyException();
    }
}
