package com.goldenpetiscaria.zeropapel.item.repository;

import com.goldenpetiscaria.zeropapel.item.dto.request.AdicionarItemRequest;
import com.goldenpetiscaria.zeropapel.item.dto.request.AtualizarItemRequest;
import com.goldenpetiscaria.zeropapel.item.dto.response.ItemResponseDTO;

import java.util.List;

public interface ItemService {
    ItemResponseDTO adicionarItemAoCardapio(AdicionarItemRequest request);
    List<ItemResponseDTO> visualizarCardapio();
    ItemResponseDTO atualizarItem(Long id, AtualizarItemRequest request);
    boolean excluirItemDoCardapio(Long id);
}

