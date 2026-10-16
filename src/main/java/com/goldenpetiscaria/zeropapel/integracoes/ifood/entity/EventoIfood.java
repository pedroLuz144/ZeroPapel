package com.goldenpetiscaria.zeropapel.integracoes.ifood.entity;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.enumerator.SituacaoEventoIfood;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ifood_evento")
@Getter
@Setter
@NoArgsConstructor
public class EventoIfood {

    public static final int TAMANHO_MAXIMO_DO_PAYLOAD = 8000;
    public static final int TAMANHO_MAXIMO_DO_DETALHE = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, unique = true, length = 64)
    private String eventoId;

    @Column(length = 64)
    private String codigo;

    @Column(name = "pedido_id")
    private String pedidoId;

    @Column(name = "merchant_id")
    private String merchantId;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(nullable = false, length = TAMANHO_MAXIMO_DO_PAYLOAD)
    private String payload;

    @Column(name = "recebido_em", nullable = false)
    private LocalDateTime recebidoEm;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SituacaoEventoIfood situacao = SituacaoEventoIfood.PENDENTE;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "processado_em")
    private LocalDateTime processadoEm;

    @Column(length = TAMANHO_MAXIMO_DO_DETALHE)
    private String detalhe;
}
