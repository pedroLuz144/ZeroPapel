package com.goldenpetiscaria.zeropapel.fechamentodecaixa.service;

import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response.FechamentoCaixaListagemDTO;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response.FechamentoResponseDTO;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.request.RealizarFechamentoRequest;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;

import java.util.List;

public interface FechamentoService {
    FechamentoResponseDTO realizarFechamento(RealizarFechamentoRequest request, Usuario usuario);
    FechamentoResponseDTO buscarFechamentoPorId(Long id);
    List<FechamentoCaixaListagemDTO> listarFechamentos();
}
