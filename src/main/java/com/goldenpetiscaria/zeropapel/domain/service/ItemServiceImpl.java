package com.goldenpetiscaria.zeropapel.domain.service;

import com.goldenpetiscaria.zeropapel.domain.entity.Item;
import com.goldenpetiscaria.zeropapel.dto.request.AdicionarItemRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarItemRequest;
import com.goldenpetiscaria.zeropapel.infra.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository repository;

    public ItemServiceImpl(ItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public Item adicionarItemAoCardapio(AdicionarItemRequest request) {
        Item novoItem = new Item();
        novoItem.setNome(request.nome());
        novoItem.setCategoria(request.categoria());
        novoItem.setPreco(request.preco());
        novoItem.setDescricao(request.descricao());
        return repository.save(novoItem);
    }

    @Override
    public List<Item> visualizarCardapio() {
        return repository.findAll();
    }

    @Override
    public Item atualizarItem(Long id, AtualizarItemRequest request) {
        Optional<Item> optionalItem = repository.findById(id);
        if (optionalItem.isEmpty()) throw new RecursoNaoEncontradoException("Item não encontrado para o ID informado");
        Item itemExistente = optionalItem.get();
        if (request.nome() != null) itemExistente.setNome(request.nome());
        if (request.categoria() != null) itemExistente.setCategoria(request.categoria());
        if (request.preco() != null) itemExistente.setPreco(request.preco());
        if (request.status() != null) itemExistente.setStatus(request.status());
        if (request.descricao() != null) itemExistente.setDescricao(request.descricao());
        return repository.save(itemExistente);
    }

    @Override
    public boolean excluirItemDoCardapio(Long id) {
        Optional<Item> optionalItem = repository.findById(id);
        if (optionalItem.isEmpty()) throw new RecursoNaoEncontradoException("Item não encontrado para o ID informado");
        repository.delete(optionalItem.get());
        return true;
    }
}
