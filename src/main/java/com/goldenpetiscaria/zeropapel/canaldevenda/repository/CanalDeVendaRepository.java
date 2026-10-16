package com.goldenpetiscaria.zeropapel.canaldevenda.repository;

import com.goldenpetiscaria.zeropapel.canaldevenda.entity.CanalDeVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CanalDeVendaRepository extends JpaRepository<CanalDeVenda, Long> {
    Optional<CanalDeVenda> findByLojaIdAndPlataformaId(Long lojaId, Long plataformaId);
    Optional<CanalDeVenda> findByMerchantId(String merchantId);
    long countByLojaIdAndAtivoTrueAndPlataformaEntregaFalse(Long lojaId);

    @Query("SELECT c FROM CanalDeVenda c " +
           "JOIN FETCH c.loja l " +
           "JOIN FETCH c.plataforma p " +
           "ORDER BY l.nome, p.nome")
    List<CanalDeVenda> findAllComLojaEPlataforma();
}
