package com.theroyale.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.theroyale.backend.model.ItemConsumo;

// ===== Acceso a datos de los consumos cargados a las cuentas =====
// Las consultas exists* permiten impedir el borrado de servicios u operadores con historial.
@Repository 
public interface ItemConsumoRepository extends JpaRepository<ItemConsumo, Long> {

    boolean existsByServicioId(Long servicioId);

    boolean existsByOperadorId(Long operadorId);
}
