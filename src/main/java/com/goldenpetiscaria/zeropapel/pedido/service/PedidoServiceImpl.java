package com.goldenpetiscaria.zeropapel.pedido.service;

import com.goldenpetiscaria.zeropapel.formadepagamento.entity.FormaDePagamento;
import com.goldenpetiscaria.zeropapel.item.entity.Item;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AdicionarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.ItemPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.ItemPedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.dto.response.PedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.entity.ItemPedido;
import com.goldenpetiscaria.zeropapel.pedido.entity.Pedido;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido;
import com.goldenpetiscaria.zeropapel.pedido.repository.PedidoRepository;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.formadepagamento.repository.FormaDePagamentoRepository;
import com.goldenpetiscaria.zeropapel.item.repository.ItemRepository;
import com.goldenpetiscaria.zeropapel.plataforma.repository.PlataformaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PlataformaRepository plataformaRepository;
    private final FormaDePagamentoRepository formaDePagamentoRepository;
    private final ItemRepository itemRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             PlataformaRepository plataformaRepository,
                             FormaDePagamentoRepository formaDePagamentoRepository,
                             ItemRepository itemRepository) {
        this.pedidoRepository = pedidoRepository;
        this.plataformaRepository = plataformaRepository;
        this.formaDePagamentoRepository = formaDePagamentoRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public PedidoResponseDTO registrarPedido(AdicionarPedidoRequest request) {
        Plataforma plataforma = plataformaRepository.findById(request.plataformaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plataforma não encontrada para o ID informado"));

        FormaDePagamento formaDePagamento = formaDePagamentoRepository.findById(request.formaDePagamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Forma de pagamento não encontrada para o ID informado"));

        Pedido pedido = new Pedido();
        pedido.setPlataforma(plataforma);
        pedido.setTaxaPlataformaPercentual(plataforma.getTaxaPercentual());
        pedido.setFormaDePagamento(formaDePagamento);
        pedido.setTaxaPagamentoPercentual(formaDePagamento.getTaxaPercentual());
        pedido.setNomeCliente(request.nomeCliente());
        pedido.setHorarioPedido(LocalDateTime.now());
        pedido.setStatus(StatusPedido.EM_ABERTO);

        List<ItemPedido> itensPedido = montarItensPedido(request.itens(), pedido);
        pedido.setItens(itensPedido);
        pedido.setValor(calcularValor(itensPedido));

        return toDTO(pedidoRepository.save(pedido));
    }

    @Override
    public List<PedidoResponseDTO> listarPedidos() {
        List<Pedido> pedidos = pedidoRepository.findAll();
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

        if (request.plataformaId() != null) {
            Plataforma plataforma = plataformaRepository.findById(request.plataformaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Plataforma não encontrada para o ID informado"));
            pedido.setPlataforma(plataforma);
            pedido.setTaxaPlataformaPercentual(plataforma.getTaxaPercentual());
        }

        if (request.formaDePagamentoId() != null) {
            FormaDePagamento formaDePagamento = formaDePagamentoRepository.findById(request.formaDePagamentoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Forma de pagamento não encontrada para o ID informado"));
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

    private List<ItemPedido> montarItensPedido(List<ItemPedidoRequest> itensRequest, Pedido pedido) {
        List<ItemPedido> itensPedido = new ArrayList<>();
        for (ItemPedidoRequest itemRequest : itensRequest) {
            Item item = itemRepository.findById(itemRequest.itemId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado para o ID informado: " + itemRequest.itemId()));
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setPedido(pedido);
            itemPedido.setItem(item);
            itemPedido.setQuantidade(itemRequest.quantidade());
            itemPedido.setPrecoUnitario(item.getPreco());
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
                    i.getItem().getId(),
                    i.getItem().getNome(),
                    i.getQuantidade(),
                    i.getPrecoUnitario(),
                    subtotal
            ));
        }

        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getPlataforma().getId(),
                pedido.getPlataforma().getNome(),
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
