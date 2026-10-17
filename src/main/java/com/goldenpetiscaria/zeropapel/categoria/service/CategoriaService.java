package com.goldenpetiscaria.zeropapel.categoria.service;

import com.goldenpetiscaria.zeropapel.categoria.dto.request.AdicionarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.categoria.dto.request.AtualizarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.categoria.dto.response.CategoriaResponseDTO;
import com.goldenpetiscaria.zeropapel.categoria.entity.Categoria;

import java.util.List;

public interface CategoriaService {
    CategoriaResponseDTO adicionarCategoria(AdicionarCategoriaRequest request);
    List<CategoriaResponseDTO> listarCategorias();
    CategoriaResponseDTO atualizarCategoria(Long id, AtualizarCategoriaRequest request);
    void excluirCategoria(Long id);
    Categoria buscarEntidadePorId(Long id);
}
