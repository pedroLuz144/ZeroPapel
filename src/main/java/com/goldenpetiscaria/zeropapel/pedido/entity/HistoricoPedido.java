package com.goldenpetiscaria.zeropapel.pedido.entity;

import com.goldenpetiscaria.zeropapel.pedido.enumerator.AcaoHistoricoPedido;
import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_pedido", indexes = @Index(name = "idx_historico_pedido_pedido_id", columnList = "pedido_id"))
@Getter
@Setter
@NoArgsConstructor
public class HistoricoPedido {

    public static final int TAMANHO_DO_DETALHE = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AcaoHistoricoPedido acao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false, length = TAMANHO_DO_DETALHE)
    private String detalhe;
}
