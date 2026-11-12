package com.theroyale.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// ===== Punto de entrada de The Royale =====
// La aplicación expone únicamente la API REST (api/) que consume el frontend Angular;
// DataLoader carga los datos de prueba en H2 al arrancar.
// @EnableScheduling activa la tarea diaria de clima del plan de visita (plan.clima.cron; "-" la apaga,
// como en el perfil test).
@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
