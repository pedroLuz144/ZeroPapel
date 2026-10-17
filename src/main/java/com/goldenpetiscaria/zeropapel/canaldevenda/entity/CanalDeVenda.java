package com.goldenpetiscaria.zeropapel.canaldevenda.entity;

import com.goldenpetiscaria.zeropapel.canaldevenda.enumerator.TratamentoTaxaEntrega;
import com.goldenpetiscaria.zeropapel.loja.entity.Loja;
import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "canal_de_venda",
        uniqueConstraints = @UniqueConstraint(name = "uk_canal_de_venda_loja_plataforma",
                columnNames = {"loja_id", "plataforma_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CanalDeVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @ManyToOne
    @JoinColumn(name = "plataforma_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Plataforma plataforma;

    @Column(name = "merchant_id", unique = true)
    private String merchantId;

    @Column(name = "taxa_percentual", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxaPercentual;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tratamento_taxa_entrega", nullable = false, length = 20)
    private TratamentoTaxaEntrega tratamentoTaxaEntrega = TratamentoTaxaEntrega.DESCONTAR;

    @Column(nullable = false)
    private boolean ativo = true;
}
