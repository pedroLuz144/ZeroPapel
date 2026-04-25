package com.goldenpetiscaria.zeropapel.domain.service;

import com.goldenpetiscaria.zeropapel.domain.entity.Item;
import com.goldenpetiscaria.zeropapel.dto.request.AdicionarItemRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarItemRequest;

import java.util.List;

public interface ItemService {
    Item adicionarItemAoCardapio(AdicionarItemRequest request);
    List<Item> visualizarCardapio();
    Item atualizarItem(Long id, AtualizarItemRequest request);
    boolean excluirItemDoCardapio(Long id);
}

