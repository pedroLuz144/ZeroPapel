package com.goldenpetiscaria.zeropapel.repository;

import com.goldenpetiscaria.zeropapel.domain.entity.FormaDePagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FormaDePagamentoRepository extends JpaRepository<FormaDePagamento, Long> {
    Optional<FormaDePagamento> findByNome(String nome);
}
