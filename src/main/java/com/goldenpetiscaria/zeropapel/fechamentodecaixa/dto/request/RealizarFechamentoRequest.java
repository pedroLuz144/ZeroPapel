package com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RealizarFechamentoRequest(
        @NotNull LocalDateTime de,
        @NotNull LocalDateTime ate
) {}
