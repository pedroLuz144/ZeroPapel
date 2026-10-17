package com.goldenpetiscaria.zeropapel.item.listener;

import com.goldenpetiscaria.zeropapel.categoria.evento.ExclusaoDeCategoriaSolicitadaEvent;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.item.repository.ItemRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CategoriaVinculadaAItemListener {

    private final ItemRepository itemRepository;

    public CategoriaVinculadaAItemListener(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @EventListener
    public void aoSolicitarExclusaoDeCategoria(ExclusaoDeCategoriaSolicitadaEvent evento) {
        if (itemRepository.existsByCategoriaId(evento.categoriaId())) {
            throw new ConflitoException("Não é possível excluir a categoria pois existem itens vinculados a ela");
        }
    }
}
