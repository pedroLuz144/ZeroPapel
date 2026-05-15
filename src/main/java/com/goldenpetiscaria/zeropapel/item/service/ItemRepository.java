package com.goldenpetiscaria.zeropapel.item.service;

import com.goldenpetiscaria.zeropapel.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    boolean existsByCategoriaId(Long categoriaId);
}
