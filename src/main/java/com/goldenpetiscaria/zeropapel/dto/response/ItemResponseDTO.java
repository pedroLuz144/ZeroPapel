package com.goldenpetiscaria.zeropapel.dto.response;

import com.goldenpetiscaria.zeropapel.domain.enums.Status;

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
