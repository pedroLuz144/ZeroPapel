package com.goldenpetiscaria.zeropapel.domain.service.contratos;

import com.goldenpetiscaria.zeropapel.dto.request.AdicionarPlataformaRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarPlataformaRequest;
import com.goldenpetiscaria.zeropapel.dto.response.PlataformaResponseDTO;

import java.util.List;

public interface PlataformaService {
    PlataformaResponseDTO adicionarPlataforma(AdicionarPlataformaRequest request);
    List<PlataformaResponseDTO> listarPlataformas();
    PlataformaResponseDTO atualizarPlataforma(Long id, AtualizarPlataformaRequest request);
    void excluirPlataforma(Long id);
}
