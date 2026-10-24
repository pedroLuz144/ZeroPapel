package com.goldenpetiscaria.zeropapel.canaldevenda.controller;

import com.goldenpetiscaria.zeropapel.canaldevenda.dto.request.AdicionarCanalDeVendaRequest;
import com.goldenpetiscaria.zeropapel.canaldevenda.dto.request.AtualizarCanalDeVendaRequest;
import com.goldenpetiscaria.zeropapel.canaldevenda.dto.response.CanalDeVendaResponseDTO;
import com.goldenpetiscaria.zeropapel.canaldevenda.service.CanalDeVendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/canaisDeVenda")
public class CanalDeVendaController {

    private final CanalDeVendaService service;

    public CanalDeVendaController(CanalDeVendaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GERENTE')")
    public CanalDeVendaResponseDTO adicionarCanal(@RequestBody @Valid AdicionarCanalDeVendaRequest request) {
        return service.adicionarCanal(request);
    }

    @GetMapping
    public List<CanalDeVendaResponseDTO> listarCanais() {
        return service.listarCanais();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public CanalDeVendaResponseDTO atualizarCanal(@PathVariable Long id, @RequestBody @Valid AtualizarCanalDeVendaRequest request) {
        return service.atualizarCanal(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GERENTE')")
    public void excluirCanal(@PathVariable Long id) {
        service.excluirCanal(id);
    }
}
