package com.goldenpetiscaria.zeropapel.repository;

import com.goldenpetiscaria.zeropapel.domain.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    boolean existsByPlataformaId(Long plataformaId);
    boolean existsByFormaDePagamentoId(Long formaDePagamentoId);
}
