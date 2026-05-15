package com.goldenpetiscaria.zeropapel.plataforma.service;

import com.goldenpetiscaria.zeropapel.plataforma.dto.request.AdicionarPlataformaRequest;
import com.goldenpetiscaria.zeropapel.plataforma.dto.request.AtualizarPlataformaRequest;
import com.goldenpetiscaria.zeropapel.plataforma.dto.response.PlataformaResponseDTO;

import java.util.List;

public interface PlataformaService {
    PlataformaResponseDTO adicionarPlataforma(AdicionarPlataformaRequest request);
    List<PlataformaResponseDTO> listarPlataformas();
    PlataformaResponseDTO atualizarPlataforma(Long id, AtualizarPlataformaRequest request);
    void excluirPlataforma(Long id);
}
