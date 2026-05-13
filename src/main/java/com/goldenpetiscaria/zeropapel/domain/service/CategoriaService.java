package com.goldenpetiscaria.zeropapel.domain.service;

import com.goldenpetiscaria.zeropapel.dto.request.AdicionarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.dto.response.CategoriaResponseDTO;

import java.util.List;

public interface CategoriaService {
    CategoriaResponseDTO adicionarCategoria(AdicionarCategoriaRequest request);
    List<CategoriaResponseDTO> listarCategorias();
    CategoriaResponseDTO atualizarCategoria(Long id, AtualizarCategoriaRequest request);
    void excluirCategoria(Long id);
}
