package com.goldenpetiscaria.zeropapel.domain.service.implementacoes;

import com.goldenpetiscaria.zeropapel.domain.entity.Plataforma;
import com.goldenpetiscaria.zeropapel.domain.service.contratos.PlataformaService;
import com.goldenpetiscaria.zeropapel.dto.request.AdicionarPlataformaRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarPlataformaRequest;
import com.goldenpetiscaria.zeropapel.dto.response.PlataformaResponseDTO;
import com.goldenpetiscaria.zeropapel.infra.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.infra.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.repository.PedidoRepository;
import com.goldenpetiscaria.zeropapel.repository.PlataformaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PlataformaServiceImpl implements PlataformaService {

    private final PlataformaRepository plataformaRepository;
    private final PedidoRepository pedidoRepository;

    public PlataformaServiceImpl(PlataformaRepository plataformaRepository, PedidoRepository pedidoRepository) {
        this.plataformaRepository = plataformaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public PlataformaResponseDTO adicionarPlataforma(AdicionarPlataformaRequest request) {
        if (plataformaRepository.findByNome(request.nome()).isPresent()) {
            throw new ConflitoException("Já existe uma plataforma com o nome: " + request.nome());
        }
        Plataforma plataforma = new Plataforma();
        plataforma.setNome(request.nome());
        plataforma.setTaxaPercentual(request.taxaPercentual());
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
        Plataforma plataforma = plataformaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plataforma não encontrada para o ID informado"));
        if (request.nome() != null) {
            Optional<Plataforma> existente = plataformaRepository.findByNome(request.nome());
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new ConflitoException("Já existe uma plataforma com o nome: " + request.nome());
            }
            plataforma.setNome(request.nome());
        }
        if (request.taxaPercentual() != null) {
            plataforma.setTaxaPercentual(request.taxaPercentual());
        }
        return toDTO(plataformaRepository.save(plataforma));
    }

    @Override
    public void excluirPlataforma(Long id) {
        Plataforma plataforma = plataformaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plataforma não encontrada para o ID informado"));
        if (pedidoRepository.existsByPlataformaId(id)) {
            throw new ConflitoException("Não é possível excluir a plataforma pois existem pedidos vinculados a ela");
        }
        plataformaRepository.delete(plataforma);
    }

    private PlataformaResponseDTO toDTO(Plataforma plataforma) {
        return new PlataformaResponseDTO(plataforma.getId(), plataforma.getNome(), plataforma.getTaxaPercentual());
    }
}
