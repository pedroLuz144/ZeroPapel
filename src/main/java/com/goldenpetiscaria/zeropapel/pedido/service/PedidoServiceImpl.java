package com.goldenpetiscaria.zeropapel.pedido.service;

import com.goldenpetiscaria.zeropapel.canaldevenda.entity.CanalDeVenda;
import com.goldenpetiscaria.zeropapel.canaldevenda.service.CanalDeVendaService;
import com.goldenpetiscaria.zeropapel.formadepagamento.entity.FormaDePagamento;
import com.goldenpetiscaria.zeropapel.item.entity.Item;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AdicionarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.ItemPedidoExternoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.ItemPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.RegistrarPedidoExternoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.ItemPedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.PedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.entity.ItemPedido;
import com.goldenpetiscaria.zeropapel.pedido.entity.Pedido;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;
import com.goldenpetiscaria.zeropapel.pedido.repository.PedidoRepository;
import com.goldenpetiscaria.zeropapel.common.consulta.Periodo;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.common.exception.RequisicaoInvalidaException;
import com.goldenpetiscaria.zeropapel.formadepagamento.repository.FormaDePagamentoRepository;
import com.goldenpetiscaria.zeropapel.item.repository.ItemRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PedidoServiceImpl implements PedidoService {

    private static final int LIMITE_DE_DIAS_DO_PAINEL = 31;

    private final PedidoRepository pedidoRepository;
    private final CanalDeVendaService canalDeVendaService;
    private final FormaDePagamentoRepository formaDePagamentoRepository;
    private final ItemRepository itemRepository;
    private final Validator validator;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             CanalDeVendaService canalDeVendaService,
                             FormaDePagamentoRepository formaDePagamentoRepository,
                             ItemRepository itemRepository,
                             Validator validator) {
        this.pedidoRepository = pedidoRepository;
        this.canalDeVendaService = canalDeVendaService;
        this.formaDePagamentoRepository = formaDePagamentoRepository;
        this.itemRepository = itemRepository;
        this.validator = validator;
    }

    @Override
    @Transactional
    public PedidoResponseDTO registrarPedido(AdicionarPedidoRequest request) {
        Pedido pedido = abrirPedido(request.lojaId(), request.plataformaId(), request.formaDePagamentoId(),
                request.nomeCliente(), LocalDateTime.now());

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
        Pedido pedido = abrirPedido(request.lojaId(), request.plataformaId(), request.formaDePagamentoId(),
                request.nomeCliente(), horarioPedido);
        pedido.setPlataformaPedidoId(request.plataformaPedidoId());

        return salvarComItens(pedido, montarItensExternos(request.itens(), pedido));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPedidos(LocalDateTime de, LocalDateTime ate) {
        Periodo periodo = Periodo.doDiaCorrenteSeAusente(de, ate, LIMITE_DE_DIAS_DO_PAINEL);
        List<Pedido> pedidos = pedidoRepository.findDoPainelComItens(periodo.de(), periodo.ate());
        List<PedidoResponseDTO> resultado = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            resultado.add(toDTO(pedido));
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
    public PedidoResponseDTO atualizarPedido(Long id, AtualizarPedidoRequest request) {
        Pedido pedido = buscarAlteravel(id);
        if (pedido.getPlataformaPedidoId() != null) {
            throw new ConflitoException(
                    "Pedido vindo de plataforma externa não pode ser editado; só o status pode mudar");
        }

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

        return toDTO(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoResponseDTO atualizarStatus(Long id, StatusPedido status) {
        Pedido pedido = buscarAlteravel(id);
        pedido.setStatus(status);
        return toDTO(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public void excluirPedido(Long id) {
        Pedido pedido = buscarAlteravel(id);
        pedidoRepository.delete(pedido);
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

    private Pedido abrirPedido(Long lojaId, Long plataformaId, Long formaDePagamentoId,
                               String nomeCliente, LocalDateTime horarioPedido) {
        CanalDeVenda canal = canalDeVendaService.buscarCanalAtivo(lojaId, plataformaId);
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

    private PedidoResponseDTO salvarComItens(Pedido pedido, List<ItemPedido> itensPedido) {
        pedido.setItens(itensPedido);
        pedido.setValor(calcularValor(itensPedido));
        return toDTO(pedidoRepository.save(pedido));
    }

    private FormaDePagamento buscarFormaDePagamento(Long formaDePagamentoId) {
        return formaDePagamentoRepository.findById(formaDePagamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Forma de pagamento não encontrada para o ID informado"));
    }

    private List<ItemPedido> montarItensPedido(List<ItemPedidoRequest> itensRequest, Pedido pedido) {
        List<ItemPedido> itensPedido = new ArrayList<>();
        for (ItemPedidoRequest itemRequest : itensRequest) {
            Item item = itemRepository.findById(itemRequest.itemId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado para o ID informado: " + itemRequest.itemId()));
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
                itensDTO
        );
    }
}
