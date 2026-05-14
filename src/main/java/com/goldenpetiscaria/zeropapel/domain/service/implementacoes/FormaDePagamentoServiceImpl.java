package com.goldenpetiscaria.zeropapel.domain.service.implementacoes;

import com.goldenpetiscaria.zeropapel.domain.entity.FormaDePagamento;
import com.goldenpetiscaria.zeropapel.domain.service.contratos.FormaDePagamentoService;
import com.goldenpetiscaria.zeropapel.dto.request.AdicionarFormaDePagamentoRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarFormaDePagamentoRequest;
import com.goldenpetiscaria.zeropapel.dto.response.FormaDePagamentoResponseDTO;
import com.goldenpetiscaria.zeropapel.infra.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.infra.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.repository.FormaDePagamentoRepository;
import com.goldenpetiscaria.zeropapel.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FormaDePagamentoServiceImpl implements FormaDePagamentoService {

    private final FormaDePagamentoRepository formaDePagamentoRepository;
    private final PedidoRepository pedidoRepository;

    public FormaDePagamentoServiceImpl(FormaDePagamentoRepository formaDePagamentoRepository, PedidoRepository pedidoRepository) {
        this.formaDePagamentoRepository = formaDePagamentoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public FormaDePagamentoResponseDTO adicionarFormaDePagamento(AdicionarFormaDePagamentoRequest request) {
        if (formaDePagamentoRepository.findByNome(request.nome()).isPresent()) {
            throw new ConflitoException("Já existe uma forma de pagamento com o nome: " + request.nome());
        }
        FormaDePagamento formaDePagamento = new FormaDePagamento();
        formaDePagamento.setNome(request.nome());
        formaDePagamento.setTaxaPercentual(request.taxaPercentual());
        return toDTO(formaDePagamentoRepository.save(formaDePagamento));
    }

    @Override
    public List<FormaDePagamentoResponseDTO> listarFormasDePagamento() {
        List<FormaDePagamentoResponseDTO> resultado = new ArrayList<>();
        for (FormaDePagamento forma : formaDePagamentoRepository.findAll()) {
            resultado.add(toDTO(forma));
        }
        return resultado;
    }

    @Override
    public FormaDePagamentoResponseDTO atualizarFormaDePagamento(Long id, AtualizarFormaDePagamentoRequest request) {
        FormaDePagamento formaDePagamento = formaDePagamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Forma de pagamento não encontrada para o ID informado"));
        if (request.nome() != null) {
            Optional<FormaDePagamento> existente = formaDePagamentoRepository.findByNome(request.nome());
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new ConflitoException("Já existe uma forma de pagamento com o nome: " + request.nome());
            }
            formaDePagamento.setNome(request.nome());
        }
        if (request.taxaPercentual() != null) {
            formaDePagamento.setTaxaPercentual(request.taxaPercentual());
        }
        return toDTO(formaDePagamentoRepository.save(formaDePagamento));
    }

    @Override
    public void excluirFormaDePagamento(Long id) {
        FormaDePagamento formaDePagamento = formaDePagamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Forma de pagamento não encontrada para o ID informado"));
        if (pedidoRepository.existsByFormaDePagamentoId(id)) {
            throw new ConflitoException("Não é possível excluir a forma de pagamento pois existem pedidos vinculados a ela");
        }
        formaDePagamentoRepository.delete(formaDePagamento);
    }

    private FormaDePagamentoResponseDTO toDTO(FormaDePagamento formaDePagamento) {
        return new FormaDePagamentoResponseDTO(formaDePagamento.getId(), formaDePagamento.getNome(), formaDePagamento.getTaxaPercentual());
    }
}
