package com.goldenpetiscaria.zeropapel.formadepagamento.service;

import com.goldenpetiscaria.zeropapel.formadepagamento.dto.request.AdicionarFormaDePagamentoRequest;
import com.goldenpetiscaria.zeropapel.formadepagamento.dto.request.AtualizarFormaDePagamentoRequest;
import com.goldenpetiscaria.zeropapel.formadepagamento.dto.response.FormaDePagamentoResponseDTO;

import java.util.List;

public interface FormaDePagamentoService {
    FormaDePagamentoResponseDTO adicionarFormaDePagamento(AdicionarFormaDePagamentoRequest request);
    List<FormaDePagamentoResponseDTO> listarFormasDePagamento();
    FormaDePagamentoResponseDTO atualizarFormaDePagamento(Long id, AtualizarFormaDePagamentoRequest request);
    void excluirFormaDePagamento(Long id);
}
