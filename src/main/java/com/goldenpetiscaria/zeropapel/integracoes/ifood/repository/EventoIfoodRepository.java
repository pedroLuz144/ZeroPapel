package com.goldenpetiscaria.zeropapel.integracoes.ifood.repository;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.entity.EventoIfood;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.enumerator.SituacaoEventoIfood;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoIfoodRepository extends JpaRepository<EventoIfood, Long> {
    boolean existsByEventoId(String eventoId);
    List<EventoIfood> findTop200BySituacaoOrderByCriadoEmAscIdAsc(SituacaoEventoIfood situacao);
}
