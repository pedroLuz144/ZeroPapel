package com.goldenpetiscaria.zeropapel.pedido.entity;

import com.goldenpetiscaria.zeropapel.formadepagamento.entity.FormaDePagamento;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plataforma_id", nullable = false)
    private Plataforma plataforma;

    private String nomeCliente;

    @Column(name = "horario_pedido", nullable = false)
    private LocalDateTime horarioPedido;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens;

    @ManyToOne
    @JoinColumn(name = "forma_de_pagamento_id", nullable = false)
    private FormaDePagamento formaDePagamento;

    @Column(nullable = false)
    private BigDecimal valor;
}
