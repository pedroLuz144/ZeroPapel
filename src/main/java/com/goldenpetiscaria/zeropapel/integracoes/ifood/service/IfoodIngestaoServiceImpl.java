package com.goldenpetiscaria.zeropapel.integracoes.ifood.service;

import com.goldenpetiscaria.zeropapel.canaldevenda.entity.CanalDeVenda;
import com.goldenpetiscaria.zeropapel.canaldevenda.service.CanalDeVendaService;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodEventoClient;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodPedidoClient;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.EventoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.PedidoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.entity.EventoIfood;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.enumerator.EventoPedidoIfood;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.enumerator.SituacaoEventoIfood;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.exception.IntegracaoIfoodException;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.repository.EventoIfoodRepository;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.translator.IfoodPedidoTranslator;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.ResultadoStatusExterno;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;
import com.goldenpetiscaria.zeropapel.pedido.service.PedidoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IfoodIngestaoServiceImpl implements IfoodIngestaoService {

    static final int LIMITE_DE_TENTATIVAS = 10;

    private final IfoodEventoClient eventoClient;
    private final IfoodPedidoClient pedidoClient;
    private final IfoodPedidoTranslator translator;
    private final EventoIfoodRepository eventoRepository;
    private final PedidoService pedidoService;
    private final CanalDeVendaService canalDeVendaService;
    private final Long formaDePagamentoId;

    public IfoodIngestaoServiceImpl(IfoodEventoClient eventoClient,
                                    IfoodPedidoClient pedidoClient,
                                    IfoodPedidoTranslator translator,
                                    EventoIfoodRepository eventoRepository,
                                    PedidoService pedidoService,
                                    CanalDeVendaService canalDeVendaService,
                                    @Value("${ifood.forma-de-pagamento-id:}") Long formaDePagamentoId) {
        this.eventoClient = eventoClient;
        this.pedidoClient = pedidoClient;
        this.translator = translator;
        this.eventoRepository = eventoRepository;
        this.pedidoService = pedidoService;
        this.canalDeVendaService = canalDeVendaService;
        this.formaDePagamentoId = formaDePagamentoId;
    }

    @Override
    public int receberEventos() {
        exigirFormaDePagamento();
        List<EventoIfoodResponse> eventos = eventoClient.buscarEventos();
        List<String> idsRecebidos = new ArrayList<>();
        int novos = 0;
        for (EventoIfoodResponse evento : eventos) {
            if (evento.id() == null || evento.id().isBlank()) {
                log.warn("Evento do iFood sem id descartado: código {}", evento.fullCode());
                continue;
            }
            if (!eventoRepository.existsByEventoId(evento.id())) {
                eventoRepository.save(novoEvento(evento));
                novos++;
            }
            idsRecebidos.add(evento.id());
        }
        eventoClient.confirmarRecebimento(idsRecebidos);
        return novos;
    }

    @Override
    public void processarEventosPendentes() {
        exigirFormaDePagamento();
        List<EventoIfood> pendentes =
                eventoRepository.findTop200BySituacaoOrderByCriadoEmAscIdAsc(SituacaoEventoIfood.PENDENTE);
        for (EventoIfood evento : pendentes) {
            processar(evento);
            eventoRepository.save(evento);
        }
    }

    private void processar(EventoIfood evento) {
        try {
            aplicar(evento);
        } catch (IntegracaoIfoodException e) {
            evento.setTentativas(evento.getTentativas() + 1);
            evento.setDetalhe(limitarDetalhe(e.getMessage()));
            if (evento.getTentativas() >= LIMITE_DE_TENTATIVAS) {
                encerrar(evento, SituacaoEventoIfood.FALHOU, e.getMessage());
                log.error("Evento {} do iFood abandonado depois de {} tentativas: {}",
                        evento.getEventoId(), evento.getTentativas(), e.getMessage());
            }
        } catch (RuntimeException e) {
            evento.setTentativas(evento.getTentativas() + 1);
            encerrar(evento, SituacaoEventoIfood.FALHOU, e.getMessage());
            log.error("Evento {} do iFood não pôde ser aplicado: {}", evento.getEventoId(), e.getMessage());
        }
    }

    private void aplicar(EventoIfood evento) {
        Optional<StatusPedido> status = EventoPedidoIfood.paraStatusPedido(evento.getCodigo());
        if (status.isEmpty()) {
            encerrar(evento, SituacaoEventoIfood.IGNORADO, "Evento sem equivalente no ciclo de vida do pedido");
            return;
        }
        if (evento.getPedidoId() == null || evento.getPedidoId().isBlank()) {
            encerrar(evento, SituacaoEventoIfood.IGNORADO, "Evento sem orderId");
            return;
        }
        CanalDeVenda canal = canalDeVendaService.buscarCanalAtivoPorMerchantId(evento.getMerchantId());
        Long plataformaId = canal.getPlataforma().getId();

        if (status.get() == StatusPedido.EM_ABERTO) {
            registrarPedido(evento, canal);
            encerrar(evento, SituacaoEventoIfood.PROCESSADO, "Pedido registrado");
            return;
        }
        ResultadoStatusExterno resultado =
                pedidoService.aplicarStatusExterno(plataformaId, evento.getPedidoId(), status.get());
        if (resultado == ResultadoStatusExterno.PEDIDO_NAO_ENCONTRADO) {
            registrarPedido(evento, canal);
            resultado = pedidoService.aplicarStatusExterno(plataformaId, evento.getPedidoId(), status.get());
        }
        encerrar(evento, SituacaoEventoIfood.PROCESSADO, "Status " + status.get() + ": " + resultado);
    }

    private void registrarPedido(EventoIfood evento, CanalDeVenda canal) {
        PedidoIfoodResponse pedido = pedidoClient.buscarPedido(evento.getPedidoId());
        pedidoService.registrarPedidoExterno(translator.traduzir(
                pedido, canal.getLoja().getId(), canal.getPlataforma().getId(), formaDePagamentoId));
    }

    private void exigirFormaDePagamento() {
        if (formaDePagamentoId == null) {
            throw new IntegracaoIfoodException(
                    "Forma de pagamento dos pedidos do iFood não configurada: defina IFOOD_FORMA_DE_PAGAMENTO_ID");
        }
    }

    private EventoIfood novoEvento(EventoIfoodResponse recebido) {
        EventoIfood evento = new EventoIfood();
        evento.setEventoId(recebido.id());
        evento.setCodigo(recebido.fullCode() != null ? recebido.fullCode() : recebido.code());
        evento.setPedidoId(recebido.orderId());
        evento.setMerchantId(recebido.merchantId());
        if (recebido.createdAt() != null) {
            evento.setCriadoEm(recebido.createdAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        }
        evento.setPayload(limitar(recebido.payload(), EventoIfood.TAMANHO_MAXIMO_DO_PAYLOAD));
        evento.setRecebidoEm(LocalDateTime.now());
        evento.setSituacao(SituacaoEventoIfood.PENDENTE);
        return evento;
    }

    private void encerrar(EventoIfood evento, SituacaoEventoIfood situacao, String detalhe) {
        evento.setSituacao(situacao);
        evento.setDetalhe(limitarDetalhe(detalhe));
        evento.setProcessadoEm(LocalDateTime.now());
    }

    private String limitarDetalhe(String detalhe) {
        return limitar(detalhe, EventoIfood.TAMANHO_MAXIMO_DO_DETALHE);
    }

    private String limitar(String texto, int tamanhoMaximo) {
        if (texto == null || texto.length() <= tamanhoMaximo) {
            return texto;
        }
        return texto.substring(0, tamanhoMaximo);
    }
}
