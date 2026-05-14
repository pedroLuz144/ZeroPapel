package com.goldenpetiscaria.zeropapel.domain.service.contratos;

import com.goldenpetiscaria.zeropapel.dto.request.AdicionarFormaDePagamentoRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarFormaDePagamentoRequest;
import com.goldenpetiscaria.zeropapel.dto.response.FormaDePagamentoResponseDTO;

import java.util.List;

public interface FormaDePagamentoService {
    FormaDePagamentoResponseDTO adicionarFormaDePagamento(AdicionarFormaDePagamentoRequest request);
    List<FormaDePagamentoResponseDTO> listarFormasDePagamento();
    FormaDePagamentoResponseDTO atualizarFormaDePagamento(Long id, AtualizarFormaDePagamentoRequest request);
    void excluirFormaDePagamento(Long id);
}
