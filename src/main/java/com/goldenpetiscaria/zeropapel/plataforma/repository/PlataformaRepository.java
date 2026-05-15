package com.goldenpetiscaria.zeropapel.plataforma.repository;

import com.goldenpetiscaria.zeropapel.plataforma.entity.Plataforma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlataformaRepository extends JpaRepository<Plataforma, Long> {
    Optional<Plataforma> findByNome(String nome);
}
