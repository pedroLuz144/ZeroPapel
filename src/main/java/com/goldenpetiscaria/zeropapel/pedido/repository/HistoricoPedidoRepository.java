package com.goldenpetiscaria.zeropapel.pedido.repository;

import com.goldenpetiscaria.zeropapel.pedido.entity.HistoricoPedido;
import com.goldenpetiscaria.zeropapel.pedido.enumerator.AcaoHistoricoPedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricoPedidoRepository extends JpaRepository<HistoricoPedido, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<HistoricoPedido> findByPedidoIdOrderByDataHoraAscIdAsc(Long pedidoId);

    Optional<HistoricoPedido> findFirstByPedidoIdAndAcaoOrderByDataHoraDescIdDesc(Long pedidoId, AcaoHistoricoPedido acao);

    List<HistoricoPedido> findByPedidoIdInAndAcao(Collection<Long> pedidoIds, AcaoHistoricoPedido acao);
}
