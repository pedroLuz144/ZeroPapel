package com.goldenpetiscaria.zeropapel.loja.controller;

import com.goldenpetiscaria.zeropapel.loja.dto.request.AdicionarLojaRequest;
import com.goldenpetiscaria.zeropapel.loja.dto.request.AtualizarLojaRequest;
import com.goldenpetiscaria.zeropapel.loja.dto.response.LojaResponseDTO;
import com.goldenpetiscaria.zeropapel.loja.service.LojaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lojas")
public class LojaController {

    private final LojaService service;

    public LojaController(LojaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GERENTE')")
    public LojaResponseDTO adicionarLoja(@RequestBody @Valid AdicionarLojaRequest request) {
        return service.adicionarLoja(request);
    }

    @GetMapping
    public List<LojaResponseDTO> listarLojas() {
        return service.listarLojas();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public LojaResponseDTO atualizarLoja(@PathVariable Long id, @RequestBody @Valid AtualizarLojaRequest request) {
        return service.atualizarLoja(id, request);
    }
}
