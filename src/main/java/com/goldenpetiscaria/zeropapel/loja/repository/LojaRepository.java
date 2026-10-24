package com.goldenpetiscaria.zeropapel.loja.repository;

import com.goldenpetiscaria.zeropapel.loja.entity.Loja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LojaRepository extends JpaRepository<Loja, Long> {
    Optional<Loja> findByNome(String nome);
    long countByAtivoTrue();
}
