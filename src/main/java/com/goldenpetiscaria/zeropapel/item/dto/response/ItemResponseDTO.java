package com.goldenpetiscaria.zeropapel.item.dto.response;

import com.goldenpetiscaria.zeropapel.item.enumerator.Status;

import java.math.BigDecimal;

public record ItemResponseDTO(
        Long id,
        String nome,
        Long categoriaId,
        String categoriaNome,
        BigDecimal preco,
        Status status,
        String descricao
) {}
