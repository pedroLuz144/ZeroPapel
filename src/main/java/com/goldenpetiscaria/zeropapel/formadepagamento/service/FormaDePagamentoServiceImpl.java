package com.goldenpetiscaria.zeropapel.formadepagamento.service;

import com.goldenpetiscaria.zeropapel.formadepagamento.dto.request.AdicionarFormaDePagamentoRequest;
import com.goldenpetiscaria.zeropapel.formadepagamento.dto.request.AtualizarFormaDePagamentoRequest;
import com.goldenpetiscaria.zeropapel.formadepagamento.dto.response.FormaDePagamentoResponseDTO;
import com.goldenpetiscaria.zeropapel.formadepagamento.entity.FormaDePagamento;
import com.goldenpetiscaria.zeropapel.formadepagamento.evento.ExclusaoDeFormaDePagamentoSolicitadaEvent;
import com.goldenpetiscaria.zeropapel.formadepagamento.repository.FormaDePagamentoRepository;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FormaDePagamentoServiceImpl implements FormaDePagamentoService {

    private final FormaDePagamentoRepository formaDePagamentoRepository;
    private final ApplicationEventPublisher eventos;

    public FormaDePagamentoServiceImpl(FormaDePagamentoRepository formaDePagamentoRepository, ApplicationEventPublisher eventos) {
        this.formaDePagamentoRepository = formaDePagamentoRepository;
        this.eventos = eventos;
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
        FormaDePagamento formaDePagamento = buscarEntidadePorId(id);
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
        FormaDePagamento formaDePagamento = buscarEntidadePorId(id);
        eventos.publishEvent(new ExclusaoDeFormaDePagamentoSolicitadaEvent(id));
        formaDePagamentoRepository.delete(formaDePagamento);
    }

    @Override
    public FormaDePagamento buscarEntidadePorId(Long id) {
        return formaDePagamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Forma de pagamento não encontrada para o ID informado"));
    }

    private FormaDePagamentoResponseDTO toDTO(FormaDePagamento formaDePagamento) {
        return new FormaDePagamentoResponseDTO(formaDePagamento.getId(), formaDePagamento.getNome(), formaDePagamento.getTaxaPercentual());
    }
}
