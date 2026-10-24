package com.goldenpetiscaria.zeropapel.canaldevenda.dto.response;

import com.goldenpetiscaria.zeropapel.canaldevenda.enumerator.TratamentoTaxaEntrega;
import java.math.BigDecimal;

public record CanalDeVendaResponseDTO(
        Long id,
        Long lojaId,
        String lojaNome,
        Long plataformaId,
        String plataformaNome,
        String merchantId,
        BigDecimal taxaPercentual,
        TratamentoTaxaEntrega tratamentoTaxaEntrega,
        boolean ativo
) {}
