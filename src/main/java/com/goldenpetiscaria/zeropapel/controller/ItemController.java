package com.goldenpetiscaria.zeropapel.controller;

import com.goldenpetiscaria.zeropapel.domain.entity.Item;
import com.goldenpetiscaria.zeropapel.domain.service.ItemServiceImpl;
import com.goldenpetiscaria.zeropapel.dto.request.AdicionarItemRequest;
import com.goldenpetiscaria.zeropapel.dto.request.AtualizarItemRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/itens")
public class ItemController {

    private final ItemServiceImpl service;

    public ItemController(ItemServiceImpl service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasRole('GERENTE')")
    public Item adicionarItemAoCardapio(@RequestBody @Valid AdicionarItemRequest request) {
        return service.adicionarItemAoCardapio(request);
    }

    @GetMapping
    public List<Item> visualizarCardapio() {
        return service.visualizarCardapio();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public Item atualizarItem(@PathVariable Long id, @RequestBody @Valid AtualizarItemRequest request) {
        return service.atualizarItem(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GERENTE')")
    public void excluirItemDoCardapio(@PathVariable Long id) {
        service.excluirItemDoCardapio(id);
    }

}
