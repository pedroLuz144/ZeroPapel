package com.goldenpetiscaria.zeropapel.domain.service.contratos;

import com.goldenpetiscaria.zeropapel.dto.request.AdicionarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.dto.response.CategoriaResponseDTO;

import java.util.List;

public interface PlataformaService {
    PlataformaResponseDTO adicionarPlataforma(AdicionarPlataformaRequest request);
    List<PlataformaResponseDTO> listarPlataformas();
    PlataformaResponseDTO atualizarPlataforma(Long id, AtualizarPlataformaRequest request);
    void excluirPlataforma(Long id);
}
