package com.goldenpetiscaria.zeropapel.item.controller;

import com.goldenpetiscaria.zeropapel.item.dto.request.AdicionarItemRequest;
import com.goldenpetiscaria.zeropapel.item.dto.request.AtualizarItemRequest;
import com.goldenpetiscaria.zeropapel.item.dto.response.ItemResponseDTO;
import com.goldenpetiscaria.zeropapel.item.repository.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/itens")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GERENTE')")
    public ItemResponseDTO adicionarItemAoCardapio(@RequestBody @Valid AdicionarItemRequest request) {
        return service.adicionarItemAoCardapio(request);
    }

    @GetMapping
    public List<ItemResponseDTO> visualizarCardapio() {
        return service.visualizarCardapio();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ItemResponseDTO atualizarItem(@PathVariable Long id, @RequestBody @Valid AtualizarItemRequest request) {
        return service.atualizarItem(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GERENTE')")
    public void excluirItemDoCardapio(@PathVariable Long id) {
        service.excluirItemDoCardapio(id);
    }
}
