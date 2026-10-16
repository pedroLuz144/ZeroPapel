package com.goldenpetiscaria.zeropapel.pedido.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RegistrarPedidoExternoRequest(
        @NotNull(message = "A loja é obrigatória")
        Long lojaId,

        @NotNull(message = "A plataforma é obrigatória")
        Long plataformaId,

        @NotBlank(message = "O identificador do pedido na plataforma é obrigatório")
        @Size(max = 255, message = "O identificador do pedido na plataforma deve ter no máximo 255 caracteres")
        String plataformaPedidoId,

        @Size(max = 255, message = "O nome do cliente deve ter no máximo 255 caracteres")
        String nomeCliente,

        @NotNull(message = "A forma de pagamento é obrigatória")
        Long formaDePagamentoId,

        LocalDateTime horarioPedido,

        @DecimalMin(value = "0.0", message = "A taxa de entrega não pode ser negativa")
        BigDecimal taxaEntrega,

        boolean entregaPelaPlataforma,

        @NotEmpty(message = "O pedido precisa ter pelo menos um item")
        @Valid
        List<@NotNull(message = "O item do pedido não pode ser nulo") ItemPedidoExternoRequest> itens
) {}
