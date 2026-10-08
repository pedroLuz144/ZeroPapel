package com.goldenpetiscaria.zeropapel.canaldevenda.service;

import com.goldenpetiscaria.zeropapel.canaldevenda.dto.request.AdicionarCanalDeVendaRequest;
import com.goldenpetiscaria.zeropapel.canaldevenda.dto.request.AtualizarCanalDeVendaRequest;
import com.goldenpetiscaria.zeropapel.canaldevenda.dto.response.CanalDeVendaResponseDTO;
import com.goldenpetiscaria.zeropapel.canaldevenda.entity.CanalDeVenda;
import com.goldenpetiscaria.zeropapel.canaldevenda.repository.CanalDeVendaRepository;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.loja.entity.Loja;
import com.goldenpetiscaria.zeropapel.loja.service.LojaService;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import com.goldenpetiscaria.zeropapel.plataforma.service.PlataformaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CanalDeVendaServiceImpl implements CanalDeVendaService {

    private final CanalDeVendaRepository canalDeVendaRepository;
    private final LojaService lojaService;
    private final PlataformaService plataformaService;

    public CanalDeVendaServiceImpl(CanalDeVendaRepository canalDeVendaRepository,
                                   LojaService lojaService,
                                   PlataformaService plataformaService) {
        this.canalDeVendaRepository = canalDeVendaRepository;
        this.lojaService = lojaService;
        this.plataformaService = plataformaService;
    }

    @Override
    @Transactional
    public CanalDeVendaResponseDTO adicionarCanal(AdicionarCanalDeVendaRequest request) {
        Loja loja = lojaService.buscarEntidadePorId(request.lojaId());
        Plataforma plataforma = plataformaService.buscarEntidadePorId(request.plataformaId());
        if (canalDeVendaRepository.findByLojaIdAndPlataformaId(loja.getId(), plataforma.getId()).isPresent()) {
            throw new ConflitoException("A loja " + loja.getNome() + " já tem um canal na plataforma " + plataforma.getNome());
        }
        String merchantId = normalizarMerchantId(request.merchantId());
        recusarMerchantIdDeOutroCanal(merchantId, null);

        CanalDeVenda canal = new CanalDeVenda();
        canal.setLoja(loja);
        canal.setPlataforma(plataforma);
        canal.setTaxaPercentual(request.taxaPercentual());
        canal.setMerchantId(merchantId);
        return toDTO(canalDeVendaRepository.save(canal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanalDeVendaResponseDTO> listarCanais() {
        List<CanalDeVendaResponseDTO> resultado = new ArrayList<>();
        for (CanalDeVenda canal : canalDeVendaRepository.findAllComLojaEPlataforma()) {
            resultado.add(toDTO(canal));
        }
        return resultado;
    }

    @Override
    @Transactional
    public CanalDeVendaResponseDTO atualizarCanal(Long id, AtualizarCanalDeVendaRequest request) {
        CanalDeVenda canal = buscarPorId(id);
        if (request.taxaPercentual() != null) {
            canal.setTaxaPercentual(request.taxaPercentual());
        }
        if (request.merchantId() != null) {
            String merchantId = normalizarMerchantId(request.merchantId());
            recusarMerchantIdDeOutroCanal(merchantId, id);
            canal.setMerchantId(merchantId);
        }
        if (request.ativo() != null) {
            canal.setAtivo(request.ativo());
        }
        return toDTO(canalDeVendaRepository.save(canal));
    }

    @Override
    @Transactional
    public void excluirCanal(Long id) {
        canalDeVendaRepository.delete(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CanalDeVenda buscarCanalAtivo(Long lojaId, Long plataformaId) {
        CanalDeVenda canal = canalDeVendaRepository.findByLojaIdAndPlataformaId(lojaId, plataformaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Canal de venda não encontrado para a loja e a plataforma informadas"));
        if (!canal.isAtivo() || !canal.getLoja().isAtivo()) {
            throw new ConflitoException("O canal " + canal.getPlataforma().getNome()
                    + " da loja " + canal.getLoja().getNome() + " está inativo");
        }
        return canal;
    }

    private CanalDeVenda buscarPorId(Long id) {
        return canalDeVendaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Canal de venda não encontrado para o ID informado"));
    }

    private String normalizarMerchantId(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return null;
        }
        return merchantId.trim();
    }

    private void recusarMerchantIdDeOutroCanal(String merchantId, Long idDoProprioCanal) {
        if (merchantId == null) {
            return;
        }
        Optional<CanalDeVenda> dono = canalDeVendaRepository.findByMerchantId(merchantId);
        if (dono.isPresent() && !dono.get().getId().equals(idDoProprioCanal)) {
            throw new ConflitoException("O merchantId informado já está vinculado a outro canal de venda");
        }
    }

    private CanalDeVendaResponseDTO toDTO(CanalDeVenda canal) {
        return new CanalDeVendaResponseDTO(
                canal.getId(),
                canal.getLoja().getId(),
                canal.getLoja().getNome(),
                canal.getPlataforma().getId(),
                canal.getPlataforma().getNome(),
                canal.getMerchantId(),
                canal.getTaxaPercentual(),
                canal.isAtivo()
        );
    }
}
