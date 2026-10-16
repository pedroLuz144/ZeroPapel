package com.goldenpetiscaria.zeropapel.loja.service;

import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.common.exception.RequisicaoInvalidaException;
import com.goldenpetiscaria.zeropapel.loja.dto.request.AdicionarLojaRequest;
import com.goldenpetiscaria.zeropapel.loja.dto.request.AtualizarLojaRequest;
import com.goldenpetiscaria.zeropapel.loja.dto.response.LojaResponseDTO;
import com.goldenpetiscaria.zeropapel.loja.entity.Loja;
import com.goldenpetiscaria.zeropapel.loja.evento.LojaCadastradaEvent;
import com.goldenpetiscaria.zeropapel.loja.repository.LojaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LojaServiceImpl implements LojaService {

    private final LojaRepository lojaRepository;
    private final ApplicationEventPublisher eventos;

    public LojaServiceImpl(LojaRepository lojaRepository, ApplicationEventPublisher eventos) {
        this.lojaRepository = lojaRepository;
        this.eventos = eventos;
    }

    @Override
    @Transactional
    public LojaResponseDTO adicionarLoja(AdicionarLojaRequest request) {
        String nome = request.nome().trim();
        if (lojaRepository.findByNome(nome).isPresent()) {
            throw new ConflitoException("Já existe uma loja com o nome: " + nome);
        }
        Loja loja = new Loja();
        loja.setNome(nome);
        Loja salva = lojaRepository.save(loja);
        eventos.publishEvent(new LojaCadastradaEvent(salva.getId()));
        return toDTO(salva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LojaResponseDTO> listarLojas() {
        List<LojaResponseDTO> resultado = new ArrayList<>();
        for (Loja loja : lojaRepository.findAll()) {
            resultado.add(toDTO(loja));
        }
        return resultado;
    }

    @Override
    @Transactional
    public LojaResponseDTO atualizarLoja(Long id, AtualizarLojaRequest request) {
        Loja loja = buscarEntidadePorId(id);
        if (request.nome() != null) {
            if (request.nome().isBlank()) {
                throw new RequisicaoInvalidaException("O nome da loja não pode ficar em branco");
            }
            String nome = request.nome().trim();
            Optional<Loja> existente = lojaRepository.findByNome(nome);
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new ConflitoException("Já existe uma loja com o nome: " + nome);
            }
            loja.setNome(nome);
        }
        if (request.ativo() != null) {
            if (!request.ativo() && loja.isAtivo() && lojaRepository.countByAtivoTrue() == 1) {
                throw new ConflitoException("Não é possível desativar a única loja ativa");
            }
            loja.setAtivo(request.ativo());
        }
        return toDTO(lojaRepository.save(loja));
    }

    @Override
    @Transactional(readOnly = true)
    public Loja buscarEntidadePorId(Long id) {
        return lojaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja não encontrada para o ID informado"));
    }

    private LojaResponseDTO toDTO(Loja loja) {
        return new LojaResponseDTO(loja.getId(), loja.getNome(), loja.isAtivo());
    }
}
