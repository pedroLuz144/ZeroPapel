package com.goldenpetiscaria.zeropapel.repository;

import com.goldenpetiscaria.zeropapel.domain.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    boolean existsByPlataformaId(Long plataformaId);
    boolean existsByFormaDePagamentoId(Long formaDePagamentoId);

    @Query("SELECT DISTINCT p FROM Pedido p " +
           "JOIN FETCH p.plataforma " +
           "JOIN FETCH p.formaDePagamento " +
           "LEFT JOIN FETCH p.itens ip " +
           "LEFT JOIN FETCH ip.item " +
           "WHERE p.horarioPedido BETWEEN :de AND :ate")
    List<Pedido> findByPeriodoComItens(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);
}
