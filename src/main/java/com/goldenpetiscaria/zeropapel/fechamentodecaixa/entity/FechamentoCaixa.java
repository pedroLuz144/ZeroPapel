package com.goldenpetiscaria.zeropapel.fechamentodecaixa.entity;

import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fechamentos_caixa")
@Getter
@Setter
@NoArgsConstructor
public class FechamentoCaixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime de;

    @Column(nullable = false)
    private LocalDateTime ate;

    @Column(name = "gerado_em", nullable = false)
    private LocalDateTime geradoEm;

    @ManyToOne
    @JoinColumn(name = "gerado_por_id", nullable = false)
    private Usuario geradoPor;

    @Column(name = "total_pedidos", nullable = false)
    private Integer totalPedidos;

    @Column(name = "faturamento_bruto", nullable = false)
    private BigDecimal faturamentoBruto;

    @Column(name = "total_taxas", nullable = false)
    private BigDecimal totalTaxas;

    @Column(name = "faturamento_liquido", nullable = false)
    private BigDecimal faturamentoLiquido;

    @Column(name = "ticket_medio", nullable = false)
    private BigDecimal ticketMedio;
}
