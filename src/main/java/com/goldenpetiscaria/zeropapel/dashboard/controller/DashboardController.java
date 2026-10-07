package com.goldenpetiscaria.zeropapel.dashboard.controller;

import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response.FechamentoResponseDTO;
import com.goldenpetiscaria.zeropapel.common.consulta.Periodo;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.service.FechamentoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Visão em tempo real do caixa. Recalcula o consolidado a partir dos pedidos
 * atuais, sem persistir — ao contrário de {@code POST /fechamento}, que congela
 * um registro. Sem parâmetros, usa o dia de hoje (00:00 → agora).
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private static final int LIMITE_DE_DIAS_DO_CONSOLIDADO = 92;

    private final FechamentoService fechamentoService;

    public DashboardController(FechamentoService fechamentoService) {
        this.fechamentoService = fechamentoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('GERENTE')")
    public FechamentoResponseDTO consolidadoDoPeriodo(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ate) {

        Periodo periodo = Periodo.doDiaCorrenteSeAusente(de, ate, LIMITE_DE_DIAS_DO_CONSOLIDADO);

        return fechamentoService.calcularPrevia(periodo.de(), periodo.ate());
    }
}
