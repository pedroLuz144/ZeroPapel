package com.goldenpetiscaria.zeropapel.pedido.repository;

import com.goldenpetiscaria.zeropapel.pedido.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
           "WHERE p.horarioPedido BETWEEN :de AND :ate " +
           "AND p.status <> com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido.CANCELADO")
    List<Pedido> findFaturaveisDoPeriodoComItens(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);

    @Query("SELECT DISTINCT p FROM Pedido p " +
           "JOIN FETCH p.plataforma " +
           "JOIN FETCH p.formaDePagamento " +
           "LEFT JOIN FETCH p.itens ip " +
           "LEFT JOIN FETCH ip.item " +
           "WHERE p.status NOT IN (com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido.CONCLUIDO, " +
           "                       com.goldenpetiscaria.zeropapel.pedido.enumerator.StatusPedido.CANCELADO) " +
           "   OR p.horarioPedido BETWEEN :de AND :ate")
    List<Pedido> findDoPainelComItens(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Pedido p SET p.fechado = true WHERE p.horarioPedido BETWEEN :de AND :ate")
    int marcarComoFechados(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);
}
