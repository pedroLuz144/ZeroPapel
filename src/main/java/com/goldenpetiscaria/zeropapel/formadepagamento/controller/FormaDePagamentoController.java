package com.goldenpetiscaria.zeropapel.formadepagamento.controller;

import com.goldenpetiscaria.zeropapel.formadepagamento.dto.request.AdicionarFormaDePagamentoRequest;
import com.goldenpetiscaria.zeropapel.formadepagamento.dto.request.AtualizarFormaDePagamentoRequest;
import com.goldenpetiscaria.zeropapel.formadepagamento.dto.response.FormaDePagamentoResponseDTO;
import com.goldenpetiscaria.zeropapel.formadepagamento.service.FormaDePagamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/formasDePagamento")
public class FormaDePagamentoController {

    private final FormaDePagamentoService service;

    public FormaDePagamentoController(FormaDePagamentoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GERENTE')")
    public FormaDePagamentoResponseDTO adicionarFormaDePagamento(@RequestBody @Valid AdicionarFormaDePagamentoRequest request) {
        return service.adicionarFormaDePagamento(request);
    }

    @GetMapping
    public List<FormaDePagamentoResponseDTO> listarFormasDePagamento() {
        return service.listarFormasDePagamento();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public FormaDePagamentoResponseDTO atualizarFormaDePagamento(@PathVariable Long id, @RequestBody @Valid AtualizarFormaDePagamentoRequest request) {
        return service.atualizarFormaDePagamento(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GERENTE')")
    public void excluirFormaDePagamento(@PathVariable Long id) {
        service.excluirFormaDePagamento(id);
    }
}
