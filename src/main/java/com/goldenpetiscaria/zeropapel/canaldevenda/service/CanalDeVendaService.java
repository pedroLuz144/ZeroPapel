package com.goldenpetiscaria.zeropapel.canaldevenda.service;

import com.goldenpetiscaria.zeropapel.canaldevenda.dto.request.AdicionarCanalDeVendaRequest;
import com.goldenpetiscaria.zeropapel.canaldevenda.dto.request.AtualizarCanalDeVendaRequest;
import com.goldenpetiscaria.zeropapel.canaldevenda.dto.response.CanalDeVendaResponseDTO;
import com.goldenpetiscaria.zeropapel.canaldevenda.entity.CanalDeVenda;

import java.util.List;

public interface CanalDeVendaService {
    CanalDeVendaResponseDTO adicionarCanal(AdicionarCanalDeVendaRequest request);
    List<CanalDeVendaResponseDTO> listarCanais();
    CanalDeVendaResponseDTO atualizarCanal(Long id, AtualizarCanalDeVendaRequest request);
    void excluirCanal(Long id);
    CanalDeVenda buscarCanalAtivo(Long lojaId, Long plataformaId);
    CanalDeVenda buscarCanalAtivoPorMerchantId(String merchantId);
    void garantirCanalDeBalcao(Long lojaId);
}
