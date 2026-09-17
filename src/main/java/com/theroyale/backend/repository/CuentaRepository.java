package com.theroyale.backend.repository;

import com.theroyale.backend.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository 
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
}