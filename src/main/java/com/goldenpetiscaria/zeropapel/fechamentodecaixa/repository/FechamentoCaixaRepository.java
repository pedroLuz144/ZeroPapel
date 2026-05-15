package com.goldenpetiscaria.zeropapel.fechamentodecaixa.repository;

import com.goldenpetiscaria.zeropapel.fechamentodecaixa.entity.FechamentoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FechamentoCaixaRepository extends JpaRepository<FechamentoCaixa, Long> {

    @Query("SELECT COUNT(f) FROM FechamentoCaixa f WHERE f.de < :ate AND f.ate > :de")
    long countByPeriodoSobreposto(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);
}
