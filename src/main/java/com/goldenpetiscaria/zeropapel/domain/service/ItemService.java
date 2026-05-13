package com.goldenpetiscaria.zeropapel.domain.service;

import com.goldenpetiscaria.zeropapel.dto.request.AdicionarItemRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarItemRequest;
import com.goldenpetiscaria.zeropapel.dto.response.ItemResponseDTO;

import java.util.List;

public interface ItemService {
    ItemResponseDTO adicionarItemAoCardapio(AdicionarItemRequest request);
    List<ItemResponseDTO> visualizarCardapio();
    ItemResponseDTO atualizarItem(Long id, AtualizarItemRequest request);
    boolean excluirItemDoCardapio(Long id);
}

