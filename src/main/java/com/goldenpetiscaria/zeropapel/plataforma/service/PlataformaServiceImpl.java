package com.goldenpetiscaria.zeropapel.plataforma.service;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.plataforma.dto.request.AdicionarPlataformaRequest;
import com.goldenpetiscaria.zeropapel.plataforma.dto.request.AtualizarPlataformaRequest;
import com.goldenpetiscaria.zeropapel.plataforma.dto.response.PlataformaResponseDTO;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import com.goldenpetiscaria.zeropapel.plataforma.evento.ExclusaoDePlataformaSolicitadaEvent;
import com.goldenpetiscaria.zeropapel.plataforma.repository.PlataformaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PlataformaServiceImpl implements PlataformaService {

    private final PlataformaRepository plataformaRepository;
    private final ApplicationEventPublisher eventos;

    public PlataformaServiceImpl(PlataformaRepository plataformaRepository, ApplicationEventPublisher eventos) {
        this.plataformaRepository = plataformaRepository;
        this.eventos = eventos;
    }

    @Override
    public PlataformaResponseDTO adicionarPlataforma(AdicionarPlataformaRequest request) {
        if (plataformaRepository.findByNome(request.nome()).isPresent()) {
            throw new ConflitoException("Já existe uma plataforma com o nome: " + request.nome());
        }
        Plataforma plataforma = new Plataforma();
        plataforma.setNome(request.nome());
        plataforma.setEntrega(request.entrega() == null || request.entrega());
        return toDTO(plataformaRepository.save(plataforma));
    }

    @Override
    public List<PlataformaResponseDTO> listarPlataformas() {
        List<PlataformaResponseDTO> resultado = new ArrayList<>();
        for (Plataforma plataforma : plataformaRepository.findAll()) {
            resultado.add(toDTO(plataforma));
        }
        return resultado;
    }

    @Override
    public PlataformaResponseDTO atualizarPlataforma(Long id, AtualizarPlataformaRequest request) {
        Plataforma plataforma = buscarEntidadePorId(id);
        if (request.nome() != null) {
            Optional<Plataforma> existente = plataformaRepository.findByNome(request.nome());
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new ConflitoException("Já existe uma plataforma com o nome: " + request.nome());
            }
            plataforma.setNome(request.nome());
        }
        if (request.entrega() != null) {
            if (request.entrega() && ehUnicaPlataformaDeBalcao(plataforma)) {
                throw new ConflitoException("Não é possível marcar como entrega a única plataforma de balcão");
            }
            plataforma.setEntrega(request.entrega());
        }
        return toDTO(plataformaRepository.save(plataforma));
    }

    @Override
    public void excluirPlataforma(Long id) {
        Plataforma plataforma = buscarEntidadePorId(id);
        if (ehUnicaPlataformaDeBalcao(plataforma)) {
            throw new ConflitoException("Não é possível excluir a única plataforma de balcão");
        }
        eventos.publishEvent(new ExclusaoDePlataformaSolicitadaEvent(id));
        plataformaRepository.delete(plataforma);
    }

    @Override
    public Plataforma buscarEntidadePorId(Long id) {
        return plataformaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plataforma não encontrada para o ID informado"));
    }

    @Override
    public Optional<Plataforma> buscarPlataformaDeBalcao() {
        return plataformaRepository.findFirstByEntregaFalseOrderByIdAsc();
    }

    private boolean ehUnicaPlataformaDeBalcao(Plataforma plataforma) {
        return !plataforma.isEntrega() && plataformaRepository.countByEntregaFalse() == 1;
    }

    private PlataformaResponseDTO toDTO(Plataforma plataforma) {
        return new PlataformaResponseDTO(
                plataforma.getId(),
                plataforma.getNome(),
                plataforma.isEntrega()
        );
    }
}
