package com.goldenpetiscaria.zeropapel.domain.service.contratos;

import com.goldenpetiscaria.zeropapel.domain.entity.Usuario;
import com.goldenpetiscaria.zeropapel.dto.request.RealizarFechamentoRequest;
import com.goldenpetiscaria.zeropapel.dto.response.FechamentoCaixaListagemDTO;
import com.goldenpetiscaria.zeropapel.dto.response.FechamentoResponseDTO;

import java.util.List;

public interface FechamentoService {
    FechamentoResponseDTO realizarFechamento(RealizarFechamentoRequest request, Usuario usuario);
    FechamentoResponseDTO buscarFechamentoPorId(Long id);
    List<FechamentoCaixaListagemDTO> listarFechamentos();
}
