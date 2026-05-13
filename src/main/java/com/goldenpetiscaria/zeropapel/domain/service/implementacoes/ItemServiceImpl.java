package com.goldenpetiscaria.zeropapel.domain.service.implementacoes;

import com.goldenpetiscaria.zeropapel.domain.entity.Categoria;
import com.goldenpetiscaria.zeropapel.domain.entity.Item;
import com.goldenpetiscaria.zeropapel.domain.service.contratos.ItemService;
import com.goldenpetiscaria.zeropapel.dto.request.AdicionarItemRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarItemRequest;
import com.goldenpetiscaria.zeropapel.dto.response.ItemResponseDTO;
import com.goldenpetiscaria.zeropapel.infra.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.repository.CategoriaRepository;
import com.goldenpetiscaria.zeropapel.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository repository;
    private final CategoriaRepository categoriaRepository;

    public ItemServiceImpl(ItemRepository repository, CategoriaRepository categoriaRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public ItemResponseDTO adicionarItemAoCardapio(AdicionarItemRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoria())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada para o ID informado"));
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
            Categoria categoria = categoriaRepository.findById(request.categoria())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada para o ID informado"));
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
