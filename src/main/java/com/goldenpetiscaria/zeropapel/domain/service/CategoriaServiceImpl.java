package com.goldenpetiscaria.zeropapel.domain.service;

import com.goldenpetiscaria.zeropapel.domain.entity.Categoria;
import com.goldenpetiscaria.zeropapel.dto.request.AdicionarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.dto.response.CategoriaResponseDTO;
import com.goldenpetiscaria.zeropapel.infra.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.infra.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.repository.CategoriaRepository;
import com.goldenpetiscaria.zeropapel.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ItemRepository itemRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository, ItemRepository itemRepository) {
        this.categoriaRepository = categoriaRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public CategoriaResponseDTO adicionarCategoria(AdicionarCategoriaRequest request) {
        if (categoriaRepository.findByNome(request.nome()).isPresent()) {
            throw new ConflitoException("Já existe uma categoria com o nome: " + request.nome());
        }
        Categoria categoria = new Categoria();
        categoria.setNome(request.nome());
        return toDTO(categoriaRepository.save(categoria));
    }

    @Override
    public List<CategoriaResponseDTO> listarCategorias() {
        return categoriaRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public CategoriaResponseDTO atualizarCategoria(Long id, AtualizarCategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada para o ID informado"));
        Optional<Categoria> existente = categoriaRepository.findByNome(request.nome());
        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new ConflitoException("Já existe uma categoria com o nome: " + request.nome());
        }
        categoria.setNome(request.nome());
        return toDTO(categoriaRepository.save(categoria));
    }

    @Override
    public void excluirCategoria(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada para o ID informado"));
        if (itemRepository.existsByCategoriaId(id)) {
            throw new ConflitoException("Não é possível excluir a categoria pois existem itens vinculados a ela");
        }
        categoriaRepository.delete(categoria);
    }

    private CategoriaResponseDTO toDTO(Categoria categoria) {
        return new CategoriaResponseDTO(categoria.getId(), categoria.getNome());
    }
}
