package com.goldenpetiscaria.zeropapel.repository;

import com.goldenpetiscaria.zeropapel.domain.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    boolean existsByCategoriaId(Long categoriaId);
}
