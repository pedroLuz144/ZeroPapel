package com.goldenpetiscaria.zeropapel.categoria.service;

import com.goldenpetiscaria.zeropapel.categoria.dto.request.AdicionarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.categoria.dto.request.AtualizarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.categoria.dto.response.CategoriaResponseDTO;
import com.goldenpetiscaria.zeropapel.categoria.entity.Categoria;
import com.goldenpetiscaria.zeropapel.categoria.evento.ExclusaoDeCategoriaSolicitadaEvent;
import com.goldenpetiscaria.zeropapel.categoria.repository.CategoriaRepository;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ApplicationEventPublisher eventos;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository, ApplicationEventPublisher eventos) {
        this.categoriaRepository = categoriaRepository;
        this.eventos = eventos;
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
        Categoria categoria = buscarEntidadePorId(id);
        Optional<Categoria> existente = categoriaRepository.findByNome(request.nome());
        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new ConflitoException("Já existe uma categoria com o nome: " + request.nome());
        }
        categoria.setNome(request.nome());
        return toDTO(categoriaRepository.save(categoria));
    }

    @Override
    public void excluirCategoria(Long id) {
        Categoria categoria = buscarEntidadePorId(id);
        eventos.publishEvent(new ExclusaoDeCategoriaSolicitadaEvent(id));
        categoriaRepository.delete(categoria);
    }

    @Override
    public Categoria buscarEntidadePorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada para o ID informado"));
    }

    private CategoriaResponseDTO toDTO(Categoria categoria) {
        return new CategoriaResponseDTO(categoria.getId(), categoria.getNome());
    }
}
