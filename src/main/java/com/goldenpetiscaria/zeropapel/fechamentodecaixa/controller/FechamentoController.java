package com.goldenpetiscaria.zeropapel.fechamentodecaixa.controller;

import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response.FechamentoCaixaListagemDTO;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.response.FechamentoResponseDTO;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.service.FechamentoService;
import com.goldenpetiscaria.zeropapel.fechamentodecaixa.dto.request.RealizarFechamentoRequest;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fechamento")
public class FechamentoController {

    private final FechamentoService service;

    public FechamentoController(FechamentoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GERENTE')")
    public FechamentoResponseDTO realizarFechamento(
            @RequestBody @Valid RealizarFechamentoRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        return service.realizarFechamento(request, usuario);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public FechamentoResponseDTO buscarFechamento(@PathVariable Long id) {
        return service.buscarFechamentoPorId(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('GERENTE')")
    public List<FechamentoCaixaListagemDTO> listarFechamentos() {
        return service.listarFechamentos();
    }
}
