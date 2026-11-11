package com.theroyale.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// ===== Punto de entrada de The Royale =====
// Una sola aplicación Spring Boot sirve las vistas Thymeleaf (controller/) y la API REST
// que consume el frontend Angular (api/); DataLoader carga los datos de prueba en H2 al arrancar.
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
