package com.goldenpetiscaria.zeropapel.domain.service.contratos;

import com.goldenpetiscaria.zeropapel.dto.request.AdicionarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.dto.response.CategoriaResponseDTO;

import java.util.List;

public interface ClienteService {
    ClienteResponseDTO adicionarCliente(AdicionarClienteRequest request);
    List<ClienteResponseDTO> listarClientes();
    ClienteResponseDTO buscarCliente(Long id);
    ClienteResponseDTO atualizarCliente(Long id, AtualizarClienteRequest request);
    void excluirCliente(Long id);
}
