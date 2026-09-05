package com.goldenpetiscaria.zeropapel.plataforma.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "plataforma")
@Getter
@Setter
@NoArgsConstructor
public class Plataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private BigDecimal taxaPercentual;

    /**
     * Canal de entrega (iFood, AnotaAi) x balcão. Delivery tem etapa EM_ROTA;
     * o PDV cria pedidos na plataforma de balcão (entrega = false).
     * columnDefinition preenche as linhas existentes na migração (ddl-auto=update).
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean entrega = true;
}
