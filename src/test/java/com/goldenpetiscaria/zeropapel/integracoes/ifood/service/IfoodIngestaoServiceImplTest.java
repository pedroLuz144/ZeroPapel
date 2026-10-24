package com.goldenpetiscaria.zeropapel.integracoes.ifood.service;

import com.goldenpetiscaria.zeropapel.canaldevenda.entity.CanalDeVenda;
import com.goldenpetiscaria.zeropapel.canaldevenda.service.CanalDeVendaService;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodEventoClient;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodPedidoClient;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.EventoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.PedidoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.entity.EventoIfood;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.enumerator.SituacaoEventoIfood;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.repository.EventoIfoodRepository;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.translator.IfoodPedidoTranslator;
import com.goldenpetiscaria.zeropapel.loja.entity.Loja;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.RegistrarPedidoExternoRequest;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.ResultadoStatusExterno;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;
import com.goldenpetiscaria.zeropapel.pedido.service.PedidoService;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IfoodIngestaoServiceImplTest {

    private static final Long LOJA_ID = 3L;
    private static final Long PLATAFORMA_ID = 2L;
    private static final Long FORMA_DE_PAGAMENTO_ID = 7L;
    private static final String MERCHANT_ID = "loja-1";
    private static final String PEDIDO_ID = "pedido-1";

    @Mock
    private IfoodEventoClient eventoClient;
    @Mock
    private IfoodPedidoClient pedidoClient;
    @Mock
    private EventoIfoodRepository eventoRepository;
    @Mock
    private PedidoService pedidoService;
    @Mock
    private CanalDeVendaService canalDeVendaService;

    private IfoodIngestaoServiceImpl service;

    @BeforeEach
    void montarService() {
        service = service(FORMA_DE_PAGAMENTO_ID);
    }

    @Test
    void receberEventos_gravaOsEventosNovosComoPendentesEConfirmaTodos() {
        when(eventoClient.buscarEventos()).thenReturn(List.of(
                recebido("evento-1", "PLC", "PLACED"), recebido("evento-2", "CFM", null)));
        when(eventoRepository.existsByEventoId(anyString())).thenReturn(false);

        int novos = service.receberEventos();

        assertThat(novos).isEqualTo(2);
        ArgumentCaptor<EventoIfood> gravados = ArgumentCaptor.forClass(EventoIfood.class);
        verify(eventoRepository, times(2)).save(gravados.capture());
        EventoIfood primeiro = gravados.getAllValues().get(0);
        assertThat(primeiro.getEventoId()).isEqualTo("evento-1");
        assertThat(primeiro.getCodigo()).isEqualTo("PLACED");
        assertThat(primeiro.getPedidoId()).isEqualTo(PEDIDO_ID);
        assertThat(primeiro.getMerchantId()).isEqualTo(MERCHANT_ID);
        assertThat(primeiro.getCriadoEm()).isNotNull();
        assertThat(primeiro.getRecebidoEm()).isNotNull();
        assertThat(primeiro.getPayload()).isEqualTo("{\"id\":\"evento-1\"}");
        assertThat(primeiro.getSituacao()).isEqualTo(SituacaoEventoIfood.PENDENTE);
        assertThat(gravados.getAllValues().get(1).getCodigo()).isEqualTo("CFM");
        verify(eventoClient).confirmarRecebimento(List.of("evento-1", "evento-2"));
    }

    @Test
    @DisplayName("a confirmacao so vai ao iFood depois de o evento estar gravado na inbox")
    void receberEventos_confirmaDepoisDeGravar() {
        when(eventoClient.buscarEventos()).thenReturn(List.of(recebido("evento-1", "PLC", "PLACED")));

        service.receberEventos();

        InOrder ordem = inOrder(eventoRepository, eventoClient);
        ordem.verify(eventoRepository).save(any(EventoIfood.class));
        ordem.verify(eventoClient).confirmarRecebimento(List.of("evento-1"));
    }

    @Test
    @DisplayName("evento reentregue nao e gravado de novo, mas e confirmado para o iFood parar de reenviar")
    void receberEventos_naoRegravaEventoJaRecebido() {
        when(eventoClient.buscarEventos()).thenReturn(List.of(recebido("evento-1", "PLC", "PLACED")));
        when(eventoRepository.existsByEventoId("evento-1")).thenReturn(true);

        int novos = service.receberEventos();

        assertThat(novos).isZero();
        verify(eventoRepository, never()).save(any());
        verify(eventoClient).confirmarRecebimento(List.of("evento-1"));
    }

    @Test
    void receberEventos_descartaEventoSemId() {
        when(eventoClient.buscarEventos()).thenReturn(List.of(recebido(" ", "PLC", "PLACED")));

        assertThat(service.receberEventos()).isZero();

        verify(eventoRepository, never()).save(any());
        verify(eventoClient).confirmarRecebimento(List.of());
    }

    @Test
    @DisplayName("falha ao gravar interrompe o ciclo sem confirmar: o iFood reentrega no proximo polling")
    void receberEventos_naoConfirma_quandoAGravacaoFalha() {
        when(eventoClient.buscarEventos()).thenReturn(List.of(recebido("evento-1", "PLC", "PLACED")));
        when(eventoRepository.save(any(EventoIfood.class))).thenThrow(new IllegalStateException("banco fora"));

        assertThatThrownBy(() -> service.receberEventos()).isInstanceOf(IllegalStateException.class);

        verify(eventoClient, never()).confirmarRecebimento(anyList());
    }

    @Test
    void receberEventos_limitaOPayloadAoTamanhoDaColuna() {
        EventoIfoodResponse grande = new EventoIfoodResponse("evento-1", "PLC", "PLACED", PEDIDO_ID, MERCHANT_ID,
                null, "x".repeat(9000));
        when(eventoClient.buscarEventos()).thenReturn(List.of(grande));

        service.receberEventos();

        ArgumentCaptor<EventoIfood> gravado = ArgumentCaptor.forClass(EventoIfood.class);
        verify(eventoRepository).save(gravado.capture());
        assertThat(gravado.getValue().getPayload()).hasSize(EventoIfood.TAMANHO_MAXIMO_DO_PAYLOAD);
        assertThat(gravado.getValue().getCriadoEm()).isNull();
    }

    @Test
    void semFormaDePagamentoConfigurada_naoChamaOIfoodNemAInbox() {
        IfoodIngestaoServiceImpl semForma = service(null);

        assertThatThrownBy(semForma::receberEventos)
                .isInstanceOf(IntegracaoIfoodException.class)
                .hasMessageContaining("IFOOD_FORMA_DE_PAGAMENTO_ID");
        assertThatThrownBy(semForma::processarEventosPendentes).isInstanceOf(IntegracaoIfoodException.class);

        verifyNoInteractions(eventoClient, eventoRepository, pedidoService);
    }

    @Test
    void processar_registraOPedido_quandoOEventoEPlaced() {
        EventoIfood evento = pendente("PLACED");
        comCanal();
        when(pedidoClient.buscarPedido(PEDIDO_ID)).thenReturn(pedidoDoIfood());

        service.processarEventosPendentes();

        ArgumentCaptor<RegistrarPedidoExternoRequest> request =
                ArgumentCaptor.forClass(RegistrarPedidoExternoRequest.class);
        verify(pedidoService).registrarPedidoExterno(request.capture());
        assertThat(request.getValue().lojaId()).isEqualTo(LOJA_ID);
        assertThat(request.getValue().plataformaId()).isEqualTo(PLATAFORMA_ID);
        assertThat(request.getValue().formaDePagamentoId()).isEqualTo(FORMA_DE_PAGAMENTO_ID);
        assertThat(request.getValue().plataformaPedidoId()).isEqualTo(PEDIDO_ID);
        verify(pedidoService, never()).aplicarStatusExterno(any(), any(), any());
        assertThat(evento.getSituacao()).isEqualTo(SituacaoEventoIfood.PROCESSADO);
        assertThat(evento.getProcessadoEm()).isNotNull();
        verify(eventoRepository).save(evento);
    }

    @Test
    void processar_aplicaOStatus_quandoOPedidoJaExiste() {
        EventoIfood evento = pendente("CFM");
        comCanal();
        when(pedidoService.aplicarStatusExterno(PLATAFORMA_ID, PEDIDO_ID, StatusPedido.ACEITO))
                .thenReturn(ResultadoStatusExterno.APLICADO);

        service.processarEventosPendentes();

        verifyNoInteractions(pedidoClient);
        verify(pedidoService, never()).registrarPedidoExterno(any());
        assertThat(evento.getSituacao()).isEqualTo(SituacaoEventoIfood.PROCESSADO);
        assertThat(evento.getDetalhe()).contains("ACEITO").contains("APLICADO");
    }

    @Test
    @DisplayName("status de pedido que ainda nao existe: registra o pedido e aplica o status em seguida")
    void processar_registraEAplica_quandoOStatusChegaAntesDoPedido() {
        EventoIfood evento = pendente("CONCLUDED");
        comCanal();
        when(pedidoService.aplicarStatusExterno(PLATAFORMA_ID, PEDIDO_ID, StatusPedido.CONCLUIDO))
                .thenReturn(ResultadoStatusExterno.PEDIDO_NAO_ENCONTRADO, ResultadoStatusExterno.APLICADO);
        when(pedidoClient.buscarPedido(PEDIDO_ID)).thenReturn(pedidoDoIfood());

        service.processarEventosPendentes();

        verify(pedidoService).registrarPedidoExterno(any());
        verify(pedidoService, times(2)).aplicarStatusExterno(PLATAFORMA_ID, PEDIDO_ID, StatusPedido.CONCLUIDO);
        assertThat(evento.getSituacao()).isEqualTo(SituacaoEventoIfood.PROCESSADO);
        assertThat(evento.getDetalhe()).contains("APLICADO");
    }

    @Test
    void processar_marcaProcessado_quandoOPedidoJaEstaFechadoOuOStatusNaoAvanca() {
        EventoIfood evento = pendente("CANCELLED");
        comCanal();
        when(pedidoService.aplicarStatusExterno(PLATAFORMA_ID, PEDIDO_ID, StatusPedido.CANCELADO))
                .thenReturn(ResultadoStatusExterno.PEDIDO_FECHADO);

        service.processarEventosPendentes();

        assertThat(evento.getSituacao()).isEqualTo(SituacaoEventoIfood.PROCESSADO);
        assertThat(evento.getDetalhe()).contains("PEDIDO_FECHADO");
        verifyNoInteractions(pedidoClient);
    }

    @Test
    void processar_ignoraEventoSemEquivalente() {
        EventoIfood evento = pendente("DELIVERY_GROUP_ASSIGNED");

        service.processarEventosPendentes();

        assertThat(evento.getSituacao()).isEqualTo(SituacaoEventoIfood.IGNORADO);
        verifyNoInteractions(canalDeVendaService, pedidoService, pedidoClient);
        verify(eventoRepository).save(evento);
    }

    @Test
    void processar_ignoraEventoSemOrderId() {
        EventoIfood evento = pendente("PLACED");
        evento.setPedidoId(null);

        service.processarEventosPendentes();

        assertThat(evento.getSituacao()).isEqualTo(SituacaoEventoIfood.IGNORADO);
        verifyNoInteractions(canalDeVendaService, pedidoService, pedidoClient);
    }

    @Test
    @DisplayName("falha do iFood mantem o evento pendente para a proxima rodada")
    void processar_mantemPendente_quandoOIfoodFalha() {
        EventoIfood evento = pendente("PLACED");
        comCanal();
        when(pedidoClient.buscarPedido(PEDIDO_ID)).thenThrow(new IntegracaoIfoodException("fora do ar", 503));

        service.processarEventosPendentes();

        assertThat(evento.getSituacao()).isEqualTo(SituacaoEventoIfood.PENDENTE);
        assertThat(evento.getTentativas()).isEqualTo(1);
        assertThat(evento.getDetalhe()).isEqualTo("fora do ar");
        assertThat(evento.getProcessadoEm()).isNull();
        verify(eventoRepository).save(evento);
    }

    @Test
    void processar_marcaFalhou_quandoOIfoodFalhaNaUltimaTentativa() {
        EventoIfood evento = pendente("PLACED");
        evento.setTentativas(IfoodIngestaoServiceImpl.LIMITE_DE_TENTATIVAS - 1);
        comCanal();
        when(pedidoClient.buscarPedido(PEDIDO_ID)).thenThrow(new IntegracaoIfoodException("fora do ar", 503));

        service.processarEventosPendentes();

        assertThat(evento.getSituacao()).isEqualTo(SituacaoEventoIfood.FALHOU);
        assertThat(evento.getTentativas()).isEqualTo(IfoodIngestaoServiceImpl.LIMITE_DE_TENTATIVAS);
    }

    @Test
    @DisplayName("merchantId sem canal de venda falha de imediato, sem buscar o pedido")
    void processar_marcaFalhou_quandoNaoHaCanalParaOMerchantId() {
        EventoIfood evento = pendente("PLACED");
        when(canalDeVendaService.buscarCanalAtivoPorMerchantId(MERCHANT_ID))
                .thenThrow(new RecursoNaoEncontradoException("Canal de venda não encontrado"));

        service.processarEventosPendentes();

        assertThat(evento.getSituacao()).isEqualTo(SituacaoEventoIfood.FALHOU);
        assertThat(evento.getDetalhe()).isEqualTo("Canal de venda não encontrado");
        verifyNoInteractions(pedidoClient, pedidoService);
    }

    @Test
    void processar_marcaFalhou_quandoOPedidoERecusadoPelaRegraDeNegocio() {
        EventoIfood evento = pendente("PLACED");
        comCanal();
        when(pedidoClient.buscarPedido(PEDIDO_ID)).thenReturn(pedidoDoIfood());
        when(pedidoService.registrarPedidoExterno(any())).thenThrow(new ConflitoException("x".repeat(600)));

        service.processarEventosPendentes();

        assertThat(evento.getSituacao()).isEqualTo(SituacaoEventoIfood.FALHOU);
        assertThat(evento.getDetalhe()).hasSize(EventoIfood.TAMANHO_MAXIMO_DO_DETALHE);
    }

    @Test
    @DisplayName("um evento com falha nao impede o processamento dos seguintes")
    void processar_continuaDepoisDeUmaFalha() {
        EventoIfood comFalha = evento("evento-1", "PLACED", "pedido-ruim");
        EventoIfood seguinte = evento("evento-2", "CFM", PEDIDO_ID);
        when(eventoRepository.findTop200BySituacaoOrderByCriadoEmAscIdAsc(SituacaoEventoIfood.PENDENTE))
                .thenReturn(List.of(comFalha, seguinte));
        comCanal();
        when(pedidoClient.buscarPedido("pedido-ruim")).thenThrow(new IntegracaoIfoodException("404", 404));
        when(pedidoService.aplicarStatusExterno(PLATAFORMA_ID, PEDIDO_ID, StatusPedido.ACEITO))
                .thenReturn(ResultadoStatusExterno.IGNORADO);

        service.processarEventosPendentes();

        assertThat(comFalha.getSituacao()).isEqualTo(SituacaoEventoIfood.PENDENTE);
        assertThat(seguinte.getSituacao()).isEqualTo(SituacaoEventoIfood.PROCESSADO);
        verify(eventoRepository).save(comFalha);
        verify(eventoRepository).save(seguinte);
    }

    private IfoodIngestaoServiceImpl service(Long formaDePagamentoId) {
        return new IfoodIngestaoServiceImpl(eventoClient, pedidoClient,
                new IfoodPedidoTranslator(ZoneId.of("America/Sao_Paulo")), eventoRepository, pedidoService,
                canalDeVendaService, formaDePagamentoId);
    }

    private EventoIfoodResponse recebido(String id, String codigo, String codigoCompleto) {
        return new EventoIfoodResponse(id, codigo, codigoCompleto, PEDIDO_ID, MERCHANT_ID,
                OffsetDateTime.parse("2026-10-10T22:15:30Z"), "{\"id\":\"" + id + "\"}");
    }

    private EventoIfood pendente(String codigo) {
        EventoIfood evento = evento("evento-1", codigo, PEDIDO_ID);
        when(eventoRepository.findTop200BySituacaoOrderByCriadoEmAscIdAsc(SituacaoEventoIfood.PENDENTE))
                .thenReturn(List.of(evento));
        return evento;
    }

    private EventoIfood evento(String eventoId, String codigo, String pedidoId) {
        EventoIfood evento = new EventoIfood();
        evento.setEventoId(eventoId);
        evento.setCodigo(codigo);
        evento.setPedidoId(pedidoId);
        evento.setMerchantId(MERCHANT_ID);
        evento.setPayload("{}");
        return evento;
    }

    private void comCanal() {
        Loja loja = new Loja();
        loja.setId(LOJA_ID);
        Plataforma plataforma = new Plataforma();
        plataforma.setId(PLATAFORMA_ID);
        CanalDeVenda canal = new CanalDeVenda();
        canal.setLoja(loja);
        canal.setPlataforma(plataforma);
        canal.setMerchantId(MERCHANT_ID);
        when(canalDeVendaService.buscarCanalAtivoPorMerchantId(MERCHANT_ID)).thenReturn(canal);
    }

    private PedidoIfoodResponse pedidoDoIfood() {
        return new PedidoIfoodResponse(PEDIDO_ID, OffsetDateTime.parse("2026-10-10T22:15:30Z"),
                new PedidoIfoodResponse.Cliente("Maria"),
                List.of(new PedidoIfoodResponse.Item("Fritas", new BigDecimal("2"), new BigDecimal("25.50"),
                        BigDecimal.ZERO, new BigDecimal("51.00"))), null, null);
    }
}
