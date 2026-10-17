package com.goldenpetiscaria.zeropapel.loja.service;

import com.goldenpetiscaria.zeropapel.loja.dto.request.AdicionarLojaRequest;
import com.goldenpetiscaria.zeropapel.loja.dto.request.AtualizarLojaRequest;
import com.goldenpetiscaria.zeropapel.loja.dto.response.LojaResponseDTO;
import com.goldenpetiscaria.zeropapel.loja.entity.Loja;

import java.util.List;

public interface LojaService {
    LojaResponseDTO adicionarLoja(AdicionarLojaRequest request);
    List<LojaResponseDTO> listarLojas();
    LojaResponseDTO atualizarLoja(Long id, AtualizarLojaRequest request);
    Loja buscarEntidadePorId(Long id);
}
