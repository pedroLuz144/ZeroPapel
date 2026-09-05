package com.goldenpetiscaria.zeropapel.fechamentodecaixa.service;

import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response.FechamentoCaixaListagemDTO;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response.FechamentoResponseDTO;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.request.RealizarFechamentoRequest;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;

import java.time.LocalDateTime;
import java.util.List;

public interface FechamentoService {
    FechamentoResponseDTO realizarFechamento(RealizarFechamentoRequest request, Usuario usuario);
    FechamentoResponseDTO buscarFechamentoPorId(Long id);
    List<FechamentoCaixaListagemDTO> listarFechamentos();

    /**
     * Calcula o mesmo consolidado de um fechamento para um período, mas apenas em
     * leitura: não persiste nada e não valida sobreposição. Usado pelo dashboard.
     * O {@code id} no retorno vem {@code null} (não é um fechamento salvo).
     */
    FechamentoResponseDTO calcularPrevia(LocalDateTime de, LocalDateTime ate);
}
