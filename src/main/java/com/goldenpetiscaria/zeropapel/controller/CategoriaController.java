package com.goldenpetiscaria.zeropapel.controller;

import com.goldenpetiscaria.zeropapel.domain.service.CategoriaService;
import com.goldenpetiscaria.zeropapel.dto.request.AdicionarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarCategoriaRequest;
import com.goldenpetiscaria.zeropapel.dto.response.CategoriaResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GERENTE')")
    public CategoriaResponseDTO adicionarCategoria(@RequestBody @Valid AdicionarCategoriaRequest request) {
        return service.adicionarCategoria(request);
    }

    @GetMapping
    public List<CategoriaResponseDTO> listarCategorias() {
        return service.listarCategorias();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public CategoriaResponseDTO atualizarCategoria(@PathVariable Long id, @RequestBody @Valid AtualizarCategoriaRequest request) {
        return service.atualizarCategoria(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GERENTE')")
    public void excluirCategoria(@PathVariable Long id) {
        service.excluirCategoria(id);
    }
}
