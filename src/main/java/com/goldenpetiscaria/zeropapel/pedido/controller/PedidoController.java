package com.goldenpetiscaria.zeropapel.pedido.controller;

import com.goldenpetiscaria.zeropapel.pedido.dto.response.PedidoResponseDTO;
import com.goldenpetiscaria.zeropapel.pedido.service.PedidoService;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AdicionarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarPedidoRequest;
import com.goldenpetiscaria.zeropapel.pedido.dto.request.AtualizarStatusPedidoRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponseDTO registrarPedido(@RequestBody @Valid AdicionarPedidoRequest request) {
        return service.registrarPedido(request);
    }

    @GetMapping
    public List<PedidoResponseDTO> listarPedidos(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ate) {
        return service.listarPedidos(de, ate);
    }

    @GetMapping("/{id}")
    public PedidoResponseDTO buscarPedidoPorId(@PathVariable Long id) {
        return service.buscarPedidoPorId(id);
    }

    @PatchMapping("/{id}")
    public PedidoResponseDTO atualizarPedido(@PathVariable Long id, @RequestBody @Valid AtualizarPedidoRequest request) {
        return service.atualizarPedido(id, request);
    }

    @PatchMapping("/{id}/status")
    public PedidoResponseDTO atualizarStatus(@PathVariable Long id, @RequestBody @Valid AtualizarStatusPedidoRequest request) {
        return service.atualizarStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GERENTE')")
    public void excluirPedido(@PathVariable Long id) {
        service.excluirPedido(id);
    }
}
