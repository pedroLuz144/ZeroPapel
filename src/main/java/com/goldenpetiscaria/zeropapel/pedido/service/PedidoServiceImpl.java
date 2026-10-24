package com.goldenpetiscaria.zeropapel.pedido.service;

import com.goldenpetiscaria.zeropapel.canaldevenda.entity.CanalDeVenda;
import com.goldenpetiscaria.zeropapel.canaldevenda.enumerator.TratamentoTaxaEntrega;
import com.goldenpetiscaria.zeropapel.canaldevenda.service.CanalDeVendaService;
import com.goldenpetiscaria.zeropapel.formadepagamento.entity.FormaDePagamento;
import com.goldenpetiscaria.zeropapel.item.entity.Item;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AdicionarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.ItemPedidoExternoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.ItemPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.RegistrarPedidoExternoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.HistoricoPedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.ItemPedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.PedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.entity.HistoricoPedido;
import com.goldenpetiscaria.zeropapel.pedido.entity.ItemPedido;
import com.goldenpetiscaria.zeropapel.pedido.entity.Pedido;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.AcaoHistoricoPedido;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.ResultadoStatusExterno;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;
import com.goldenpetiscaria.zeropapel.pedido.repository.HistoricoPedidoRepository;
import com.goldenpetiscaria.zeropapel.pedido.repository.PedidoRepository;
import com.goldenpetiscaria.zeropapel.common.consulta.Periodo;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.common.exception.RequisicaoInvalidaException;
import com.goldenpetiscaria.zeropapel.formadepagamento.service.FormaDePagamentoService;
import com.goldenpetiscaria.zeropapel.item.service.ItemService;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.usuario.service.UsuarioService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class PedidoServiceImpl implements PedidoService {

    private static final int LIMITE_DE_DIAS_DO_PAINEL = 31;
    private static final int MINIMO_DO_MOTIVO = 3;
    private static final int MAXIMO_DO_MOTIVO = 255;
    private static final Comparator<HistoricoPedido> MAIS_RECENTE_POR_ULTIMO =
            Comparator.comparing(HistoricoPedido::getDataHora).thenComparing(HistoricoPedido::getId);

    private final PedidoRepository pedidoRepository;
    private final CanalDeVendaService canalDeVendaService;
    private final FormaDePagamentoService formaDePagamentoService;
    private final ItemService itemService;
    private final UsuarioService usuarioService;
    private final HistoricoPedidoRepository historicoRepository;
    private final Validator validator;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             CanalDeVendaService canalDeVendaService,
                             FormaDePagamentoService formaDePagamentoService,
                             ItemService itemService,
                             UsuarioService usuarioService,
                             HistoricoPedidoRepository historicoRepository,
                             Validator validator) {
        this.pedidoRepository = pedidoRepository;
        this.canalDeVendaService = canalDeVendaService;
        this.formaDePagamentoService = formaDePagamentoService;
        this.itemService = itemService;
        this.usuarioService = usuarioService;
        this.historicoRepository = historicoRepository;
        this.validator = validator;
    }

    @Override
    @Transactional
    public PedidoResponseDTO registrarPedido(AdicionarPedidoRequest request) {
        CanalDeVenda canal = canalDeVendaService.buscarCanalAtivo(request.lojaId(), request.plataformaId());
        Pedido pedido = abrirPedido(canal, request.formaDePagamentoId(), request.nomeCliente(), LocalDateTime.now());

        return salvarComItens(pedido, montarItensPedido(request.itens(), pedido));
    }

    @Override
    @Transactional
    public PedidoResponseDTO registrarPedidoExterno(RegistrarPedidoExternoRequest request) {
        validarPedidoExterno(request);

        Optional<Pedido> jaRegistrado = pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(
                request.plataformaId(), request.plataformaPedidoId());
        if (jaRegistrado.isPresent()) {
            return toDTO(jaRegistrado.get());
        }

        LocalDateTime horarioPedido = request.horarioPedido() != null ? request.horarioPedido() : LocalDateTime.now();
        CanalDeVenda canal = canalDeVendaService.buscarCanalAtivo(request.lojaId(), request.plataformaId());
        Pedido pedido = abrirPedido(canal, request.formaDePagamentoId(), request.nomeCliente(), horarioPedido);
        pedido.setPlataformaPedidoId(request.plataformaPedidoId());
        congelarTaxaEntrega(pedido, canal, request.taxaEntrega(), request.entregaPelaPlataforma());

        return salvarComItens(pedido, montarItensExternos(request.itens(), pedido));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPedidos(LocalDateTime de, LocalDateTime ate) {
        Periodo periodo = Periodo.doDiaCorrenteSeAusente(de, ate, LIMITE_DE_DIAS_DO_PAINEL);
        List<Pedido> pedidos = pedidoRepository.findDoPainelComItens(periodo.de(), periodo.ate());
        Map<Long, String> motivos = motivosDeCancelamento(pedidos);
        List<PedidoResponseDTO> resultado = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            resultado.add(toDTO(pedido, motivos.get(pedido.getId())));
        }
        return resultado;
    }

    @Override
    public PedidoResponseDTO buscarPedidoPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado para o ID informado"));
        return toDTO(pedido);
    }

    @Override
    @Transactional
    public PedidoResponseDTO atualizarPedido(Long id, AtualizarPedidoRequest request, Long usuarioId) {
        Pedido pedido = buscarAlteravel(id);
        recusarPedidoExterno(pedido, "Pedido vindo de plataforma externa não pode ser editado");
        String antes = "Antes da edição: " + resumir(pedido);

        if (request.plataformaId() != null
                && !request.plataformaId().equals(pedido.getPlataforma().getId())) {
            CanalDeVenda canal = canalDeVendaService.buscarCanalAtivo(
                    pedido.getLoja().getId(), request.plataformaId());
            pedido.setPlataforma(canal.getPlataforma());
            pedido.setTaxaPlataformaPercentual(canal.getTaxaPercentual());
        }

        if (request.formaDePagamentoId() != null
                && !request.formaDePagamentoId().equals(pedido.getFormaDePagamento().getId())) {
            FormaDePagamento formaDePagamento = buscarFormaDePagamento(request.formaDePagamentoId());
            pedido.setFormaDePagamento(formaDePagamento);
            pedido.setTaxaPagamentoPercentual(formaDePagamento.getTaxaPercentual());
        }

        if (request.nomeCliente() != null) {
            pedido.setNomeCliente(request.nomeCliente());
        }

        if (request.itens() != null && !request.itens().isEmpty()) {
            List<ItemPedido> novosItens = montarItensPedido(request.itens(), pedido);
            pedido.getItens().clear();
            pedido.getItens().addAll(novosItens);
            pedido.setValor(calcularValor(novosItens));
        }

        Pedido salvo = pedidoRepository.save(pedido);
        registrarHistorico(id, AcaoHistoricoPedido.EDITADO, usuarioId, antes);
        return toDTO(salvo);
    }

    @Override
    @Transactional
    public PedidoResponseDTO atualizarStatus(Long id, StatusPedido status, String motivo, Long usuarioId) {
        Pedido pedido = buscarAlteravel(id);
        recusarPedidoExterno(pedido,
                "Pedido vindo de plataforma externa tem o status definido pela plataforma; não pode ser alterado pelo painel");
        if (status != StatusPedido.CANCELADO) {
            pedido.setStatus(status);
            return toDTO(pedidoRepository.save(pedido));
        }

        String motivoDoCancelamento = exigirMotivo(motivo);
        pedido.setStatus(status);
        Pedido salvo = pedidoRepository.save(pedido);
        registrarHistorico(id, AcaoHistoricoPedido.CANCELADO, usuarioId, motivoDoCancelamento);
        return toDTO(salvo, motivoDoCancelamento);
    }

    @Override
    @Transactional
    public void excluirPedido(Long id, Long usuarioId) {
        Pedido pedido = buscarAlteravel(id);
        recusarPedidoExterno(pedido,
                "Pedido vindo de plataforma externa não pode ser excluído; o cancelamento vem da plataforma");
        String resumo = "Excluído com status " + pedido.getStatus() + "; " + resumir(pedido);
        registrarHistorico(id, AcaoHistoricoPedido.EXCLUIDO, usuarioId, resumo);
        pedidoRepository.delete(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricoPedidoResponseDTO> listarHistorico(Long pedidoId) {
        return historicoRepository.findByPedidoIdOrderByDataHoraAscIdAsc(pedidoId).stream()
                .map(this::toHistoricoDTO)
                .toList();
    }

    @Override
    @Transactional
    public ResultadoStatusExterno aplicarStatusExterno(Long plataformaId, String plataformaPedidoId, StatusPedido status) {
        if (plataformaId == null || plataformaPedidoId == null || plataformaPedidoId.isBlank() || status == null) {
            throw new RequisicaoInvalidaException(
                    "Plataforma, identificador do pedido na plataforma e status são obrigatórios");
        }

        Optional<Pedido> encontrado = pedidoRepository.findByPlataformaIdAndPlataformaPedidoId(
                plataformaId, plataformaPedidoId);
        if (encontrado.isEmpty()) {
            return ResultadoStatusExterno.PEDIDO_NAO_ENCONTRADO;
        }

        Pedido pedido = encontrado.get();
        if (pedido.isFechado()) {
            log.warn("Status externo ignorado em pedido já fechado: plataforma={}, pedidoNaPlataforma={}, statusAtual={}, statusRecebido={}",
                    plataformaId, plataformaPedidoId, pedido.getStatus(), status);
            return ResultadoStatusExterno.PEDIDO_FECHADO;
        }
        if (!pedido.getStatus().podeAvancarPara(status)) {
            return ResultadoStatusExterno.IGNORADO;
        }

        pedido.setStatus(status);
        pedidoRepository.save(pedido);
        if (status == StatusPedido.CANCELADO) {
            registrarHistorico(pedido.getId(), AcaoHistoricoPedido.CANCELADO, null,
                    "Cancelado pela plataforma " + pedido.getPlataforma().getNome());
        }
        return ResultadoStatusExterno.APLICADO;
    }

    private String exigirMotivo(String motivo) {
        String limpo = motivo == null ? "" : motivo.strip();
        if (limpo.length() < MINIMO_DO_MOTIVO || limpo.length() > MAXIMO_DO_MOTIVO) {
            throw new RequisicaoInvalidaException("Informe o motivo do cancelamento, de "
                    + MINIMO_DO_MOTIVO + " a " + MAXIMO_DO_MOTIVO + " caracteres");
        }
        return limpo;
    }

    private void registrarHistorico(Long pedidoId, AcaoHistoricoPedido acao, Long usuarioId, String detalhe) {
        Usuario usuario = usuarioId != null ? usuarioService.buscarEntidadePorId(usuarioId) : null;
        HistoricoPedido registro = new HistoricoPedido();
        registro.setPedidoId(pedidoId);
        registro.setAcao(acao);
        registro.setUsuario(usuario);
        registro.setDataHora(LocalDateTime.now());
        registro.setDetalhe(limitar(detalhe));
        historicoRepository.save(registro);
    }

    private String resumir(Pedido pedido) {
        String cliente = pedido.getNomeCliente() == null || pedido.getNomeCliente().isBlank()
                ? "sem nome" : pedido.getNomeCliente().strip();
        BigDecimal valor = pedido.getValor() == null ? BigDecimal.ZERO : pedido.getValor();
        return "valor " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString()
                + "; cliente " + cliente
                + "; plataforma " + pedido.getPlataforma().getNome()
                + "; pagamento " + pedido.getFormaDePagamento().getNome();
    }

    private String limitar(String detalhe) {
        return detalhe.length() > HistoricoPedido.TAMANHO_DO_DETALHE
                ? detalhe.substring(0, HistoricoPedido.TAMANHO_DO_DETALHE)
                : detalhe;
    }

    private Map<Long, String> motivosDeCancelamento(List<Pedido> pedidos) {
        List<Long> cancelados = pedidos.stream()
                .filter(pedido -> pedido.getStatus() == StatusPedido.CANCELADO)
                .map(Pedido::getId)
                .toList();
        Map<Long, String> motivos = new HashMap<>();
        if (cancelados.isEmpty()) {
            return motivos;
        }
        Map<Long, HistoricoPedido> maisRecente = new HashMap<>();
        for (HistoricoPedido registro : historicoRepository.findByPedidoIdInAndAcao(cancelados, AcaoHistoricoPedido.CANCELADO)) {
            maisRecente.merge(registro.getPedidoId(), registro,
                    (atual, outro) -> MAIS_RECENTE_POR_ULTIMO.compare(atual, outro) >= 0 ? atual : outro);
        }
        maisRecente.forEach((pedidoId, registro) -> motivos.put(pedidoId, registro.getDetalhe()));
        return motivos;
    }

    private String motivoDeCancelamento(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.CANCELADO || pedido.getId() == null) {
            return null;
        }
        return historicoRepository
                .findFirstByPedidoIdAndAcaoOrderByDataHoraDescIdDesc(pedido.getId(), AcaoHistoricoPedido.CANCELADO)
                .map(HistoricoPedido::getDetalhe)
                .orElse(null);
    }

    private HistoricoPedidoResponseDTO toHistoricoDTO(HistoricoPedido registro) {
        Usuario usuario = registro.getUsuario();
        return new HistoricoPedidoResponseDTO(
                registro.getId(),
                registro.getAcao(),
                registro.getDataHora(),
                usuario != null ? usuario.getNome() : null,
                usuario != null ? usuario.getUsuario() : null,
                registro.getDetalhe()
        );
    }

    private void recusarPedidoExterno(Pedido pedido, String mensagem) {
        if (pedido.getPlataformaPedidoId() != null) {
            throw new ConflitoException(mensagem);
        }
    }

    private Pedido buscarAlteravel(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado para o ID informado"));
        if (pedido.isFechado()) {
            throw new ConflitoException(
                    "Pedido já incluído num fechamento de caixa; não pode mais ser alterado nem excluído");
        }
        return pedido;
    }

    private void validarPedidoExterno(RegistrarPedidoExternoRequest request) {
        if (request == null) {
            throw new RequisicaoInvalidaException("O pedido externo é obrigatório");
        }
        Set<ConstraintViolation<RegistrarPedidoExternoRequest>> violacoes = validator.validate(request);
        if (!violacoes.isEmpty()) {
            List<String> motivos = violacoes.stream().map(ConstraintViolation::getMessage).sorted().toList();
            throw new RequisicaoInvalidaException("Pedido externo inválido: " + String.join("; ", motivos));
        }
    }

    private Pedido abrirPedido(CanalDeVenda canal, Long formaDePagamentoId,
                               String nomeCliente, LocalDateTime horarioPedido) {
        FormaDePagamento formaDePagamento = buscarFormaDePagamento(formaDePagamentoId);

        Pedido pedido = new Pedido();
        pedido.setLoja(canal.getLoja());
        pedido.setPlataforma(canal.getPlataforma());
        pedido.setTaxaPlataformaPercentual(canal.getTaxaPercentual());
        pedido.setFormaDePagamento(formaDePagamento);
        pedido.setTaxaPagamentoPercentual(formaDePagamento.getTaxaPercentual());
        pedido.setNomeCliente(nomeCliente);
        pedido.setHorarioPedido(horarioPedido);
        pedido.setStatus(StatusPedido.EM_ABERTO);
        return pedido;
    }

    private void congelarTaxaEntrega(Pedido pedido, CanalDeVenda canal, BigDecimal taxaEntrega,
                                     boolean entregaPelaPlataforma) {
        if (taxaEntrega == null || taxaEntrega.signum() == 0) {
            return;
        }
        pedido.setTaxaEntrega(taxaEntrega);
        pedido.setTratamentoTaxaEntrega(entregaPelaPlataforma
                ? canal.getTratamentoTaxaEntrega()
                : TratamentoTaxaEntrega.SOMAR);
    }

    private PedidoResponseDTO salvarComItens(Pedido pedido, List<ItemPedido> itensPedido) {
        pedido.setItens(itensPedido);
        pedido.setValor(calcularValor(itensPedido));
        return toDTO(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> buscarFaturaveisDoPeriodo(LocalDateTime de, LocalDateTime ate) {
        return pedidoRepository.findFaturaveisDoPeriodoComItens(de, ate);
    }

    @Override
    @Transactional
    public int marcarComoFechados(LocalDateTime de, LocalDateTime ate) {
        return pedidoRepository.marcarComoFechados(de, ate);
    }

    private FormaDePagamento buscarFormaDePagamento(Long formaDePagamentoId) {
        return formaDePagamentoService.buscarEntidadePorId(formaDePagamentoId);
    }

    private List<ItemPedido> montarItensPedido(List<ItemPedidoRequest> itensRequest, Pedido pedido) {
        List<ItemPedido> itensPedido = new ArrayList<>();
        for (ItemPedidoRequest itemRequest : itensRequest) {
            Item item = itemService.buscarEntidadePorId(itemRequest.itemId());
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setPedido(pedido);
            itemPedido.setItem(item);
            itemPedido.setNomeItem(item.getNome());
            itemPedido.setQuantidade(itemRequest.quantidade());
            itemPedido.setPrecoUnitario(item.getPreco());
            itensPedido.add(itemPedido);
        }
        return itensPedido;
    }

    private List<ItemPedido> montarItensExternos(List<ItemPedidoExternoRequest> itensRequest, Pedido pedido) {
        List<ItemPedido> itensPedido = new ArrayList<>();
        for (ItemPedidoExternoRequest itemRequest : itensRequest) {
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setPedido(pedido);
            itemPedido.setNomeItem(itemRequest.nomeItem());
            itemPedido.setQuantidade(itemRequest.quantidade());
            itemPedido.setPrecoUnitario(itemRequest.precoUnitario());
            itensPedido.add(itemPedido);
        }
        return itensPedido;
    }

    private BigDecimal calcularValor(List<ItemPedido> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal subtotal = item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
            total = total.add(subtotal);
        }
        return total;
    }

    private PedidoResponseDTO toDTO(Pedido pedido) {
        return toDTO(pedido, motivoDeCancelamento(pedido));
    }

    private PedidoResponseDTO toDTO(Pedido pedido, String motivoCancelamento) {
        List<ItemPedidoResponseDTO> itensDTO = new ArrayList<>();
        for (ItemPedido i : pedido.getItens()) {
            BigDecimal subtotal = i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade()));
            itensDTO.add(new ItemPedidoResponseDTO(
                    i.getItem() != null ? i.getItem().getId() : null,
                    i.getNomeItem(),
                    i.getQuantidade(),
                    i.getPrecoUnitario(),
                    subtotal
            ));
        }

        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getLoja().getId(),
                pedido.getLoja().getNome(),
                pedido.getPlataforma().getId(),
                pedido.getPlataforma().getNome(),
                pedido.getPlataformaPedidoId(),
                pedido.getNomeCliente(),
                pedido.getHorarioPedido(),
                pedido.getFormaDePagamento().getId(),
                pedido.getFormaDePagamento().getNome(),
                pedido.getStatus(),
                pedido.getValor(),
                pedido.isFechado(),
                pedido.getStatus() == StatusPedido.CANCELADO ? motivoCancelamento : null,
                itensDTO
        );
    }
}
