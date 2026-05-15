package com.goldenpetiscaria.zeropapel.plataforma.controller;

import com.goldenpetiscaria.zeropapel.plataforma.dto.response.PlataformaResponseDTO;
import com.goldenpetiscaria.zeropapel.plataforma.service.PlataformaService;
import com.goldenpetiscaria.zeropapel.plataforma.dto.request.AdicionarPlataformaRequest;
import com.goldenpetiscaria.zeropapel.plataforma.dto.request.AtualizarPlataformaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plataformas")
public class PlataformaController {

    private final PlataformaService service;

    public PlataformaController(PlataformaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GERENTE')")
    public PlataformaResponseDTO adicionarPlataforma(@RequestBody @Valid AdicionarPlataformaRequest request) {
        return service.adicionarPlataforma(request);
    }

    @GetMapping
    public List<PlataformaResponseDTO> listarPlataformas() {
        return service.listarPlataformas();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public PlataformaResponseDTO atualizarPlataforma(@PathVariable Long id, @RequestBody @Valid AtualizarPlataformaRequest request) {
        return service.atualizarPlataforma(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GERENTE')")
    public void excluirPlataforma(@PathVariable Long id) {
        service.excluirPlataforma(id);
    }
}
