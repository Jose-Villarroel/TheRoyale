package com.theroyale.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.theroyale.backend.model.ItemConsumo;

@Repository 
public interface ItemConsumoRepository extends JpaRepository<ItemConsumo, Long> {
}