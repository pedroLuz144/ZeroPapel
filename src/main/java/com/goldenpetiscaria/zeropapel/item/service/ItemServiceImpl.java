package com.goldenpetiscaria.zeropapel.item.service;

import com.goldenpetiscaria.zeropapel.categoria.entity.Categoria;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.categoria.service.CategoriaService;
import com.goldenpetiscaria.zeropapel.item.dto.request.AdicionarItemRequest;
import com.goldenpetiscaria.zeropapel.item.dto.request.AtualizarItemRequest;
import com.goldenpetiscaria.zeropapel.item.dto.response.ItemResponseDTO;
import com.goldenpetiscaria.zeropapel.item.entity.Item;
import com.goldenpetiscaria.zeropapel.item.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository repository;
    private final CategoriaService categoriaService;

    public ItemServiceImpl(ItemRepository repository, CategoriaService categoriaService) {
        this.repository = repository;
        this.categoriaService = categoriaService;
    }

    @Override
    public ItemResponseDTO adicionarItemAoCardapio(AdicionarItemRequest request) {
        Categoria categoria = categoriaService.buscarEntidadePorId(request.categoria());
        Item novoItem = new Item();
        novoItem.setNome(request.nome());
        novoItem.setCategoria(categoria);
        novoItem.setPreco(request.preco());
        novoItem.setDescricao(request.descricao());
        return toDTO(repository.save(novoItem));
    }

    @Override
    public List<ItemResponseDTO> visualizarCardapio() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public ItemResponseDTO atualizarItem(Long id, AtualizarItemRequest request) {
        Optional<Item> optionalItem = repository.findById(id);
        if (optionalItem.isEmpty()) throw new RecursoNaoEncontradoException("Item não encontrado para o ID informado");
        Item itemExistente = optionalItem.get();
        if (request.nome() != null) itemExistente.setNome(request.nome());
        if (request.categoria() != null) {
            Categoria categoria = categoriaService.buscarEntidadePorId(request.categoria());
            itemExistente.setCategoria(categoria);
        }
        if (request.preco() != null) itemExistente.setPreco(request.preco());
        if (request.status() != null) itemExistente.setStatus(request.status());
        if (request.descricao() != null) itemExistente.setDescricao(request.descricao());
        return toDTO(repository.save(itemExistente));
    }

    @Override
    public boolean excluirItemDoCardapio(Long id) {
        Optional<Item> optionalItem = repository.findById(id);
        if (optionalItem.isEmpty()) throw new RecursoNaoEncontradoException("Item não encontrado para o ID informado");
        repository.delete(optionalItem.get());
        return true;
    }

    @Override
    public Item buscarEntidadePorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado para o ID informado: " + id));
    }

    private ItemResponseDTO toDTO(Item item) {
        return new ItemResponseDTO(
                item.getId(),
                item.getNome(),
                item.getCategoria().getId(),
                item.getCategoria().getNome(),
                item.getPreco(),
                item.getStatus(),
                item.getDescricao()
        );
    }
}
