package com.theroyale.backend;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// ===== Carga datos iniciales de prueba cada vez que la aplicación arranca =====
// Necesario porque ddl-auto=create-drop borra y recrea la base de datos en cada reinicio.
// Nota: este es el único archivo del proyecto donde se accede directo a los Repository,
// saltándose la capa de Service — es una excepción intencional solo para este propósito.
@Component
@Transactional
public class DataLoader implements CommandLineRunner {

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Override
    public void run(String... args) throws Exception {
        cargarTiposHabitacion();
        cargarClientes();
        cargarHabitaciones();
    }

    // ===== 5 tipos de habitación =====
    private void cargarTiposHabitacion() {
        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Normal")
                .descripcion("Elegant essentials for a refined stay. 1-2 guests, double bed, private bathroom, premium Wi-Fi.")
                .imagenUrl("/images/suite-3.webp")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Executive")
                .descripcion("Designed for those who work while they travel. 1-2 guests, executive workspace, king bed, premium Wi-Fi.")
                .imagenUrl("/images/suite-1.webp")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("VIP")
                .descripcion("A private space to relax and unwind. Up to 3 guests, separate living area, premium amenities, city views.")
                .imagenUrl("/images/suite-2.webp")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Luxury")
                .descripcion("The ultimate expression of The Royale. Up to 4 guests, full suite, jacuzzi, privileged city view.")
                .imagenUrl("/images/suite-4.webp")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Presidential Suite")
                .descripcion("Unmatched exclusivity. Up to 6 guests, private terrace, butler service, panoramic city view, private dining.")
                .imagenUrl("/images/luxury 1.jpg")
                .build());
    }

    // ===== 10 clientes =====
    private void cargarClientes() {
        clienteRepository.save(Cliente.builder()
                .nombre("Carlos").apellido("Mendoza").email("carlos.mendoza@email.com")
                .password("pass1234").telefono("+1-555-0101").fechaRegistro(LocalDate.of(2025, 1, 15))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Ana").apellido("García").email("ana.garcia@email.com")
                .password("pass1234").telefono("+1-555-0102").fechaRegistro(LocalDate.of(2025, 2, 20))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Luis").apellido("Hernández").email("luis.hernandez@email.com")
                .password("pass1234").telefono("+1-555-0103").fechaRegistro(LocalDate.of(2025, 3, 5))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Sofia").apellido("Torres").email("sofia.torres@email.com")
                .password("pass1234").telefono("+1-555-0104").fechaRegistro(LocalDate.of(2025, 3, 18))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Miguel").apellido("Ramírez").email("miguel.ramirez@email.com")
                .password("pass1234").telefono("+1-555-0105").fechaRegistro(LocalDate.of(2025, 4, 2))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Isabella").apellido("López").email("isabella.lopez@email.com")
                .password("pass1234").telefono("+1-555-0106").fechaRegistro(LocalDate.of(2025, 4, 25))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Andrés").apellido("Martínez").email("andres.martinez@email.com")
                .password("pass1234").telefono("+1-555-0107").fechaRegistro(LocalDate.of(2025, 5, 10))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Valentina").apellido("Rodríguez").email("valentina.rodriguez@email.com")
                .password("pass1234").telefono("+1-555-0108").fechaRegistro(LocalDate.of(2025, 6, 1))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Jorge").apellido("Sánchez").email("jorge.sanchez@email.com")
                .password("pass1234").telefono("+1-555-0109").fechaRegistro(LocalDate.of(2025, 7, 14))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Camila").apellido("Díaz").email("camila.diaz@email.com")
                .password("pass1234").telefono("+1-555-0110").fechaRegistro(LocalDate.of(2025, 8, 22))
                .build());
    }

    // ===== 50 habitaciones (10 por cada uno de los 5 tipos) =====
    private void cargarHabitaciones() {
        // Recupera los tipos ya guardados, en el mismo orden en que se insertaron arriba
        TipoHabitacion normal = tipoHabitacionRepository.findAll().get(0);
        TipoHabitacion executive = tipoHabitacionRepository.findAll().get(1);
        TipoHabitacion vip = tipoHabitacionRepository.findAll().get(2);
        TipoHabitacion luxury = tipoHabitacionRepository.findAll().get(3);
        TipoHabitacion presidential = tipoHabitacionRepository.findAll().get(4);

        // ----- Normal: piso 1-2, $120-$140 -----
        guardarHabitacion("101", normal, 120.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("102", normal, 120.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("103", normal, 125.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("104", normal, 125.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("105", normal, 130.00, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("106", normal, 130.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("107", normal, 135.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("108", normal, 135.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("109", normal, 140.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("110", normal, 140.00, EstadoHabitacion.DISPONIBLE);

        // ----- Executive: piso 3-4, $190-$220 -----
        guardarHabitacion("301", executive, 190.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("302", executive, 190.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("303", executive, 195.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("304", executive, 195.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("305", executive, 200.00, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("306", executive, 200.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("307", executive, 205.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("308", executive, 210.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("309", executive, 215.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("310", executive, 220.00, EstadoHabitacion.DISPONIBLE);

        // ----- VIP: piso 5-6, $280-$320 -----
        guardarHabitacion("501", vip, 280.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("502", vip, 280.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("503", vip, 285.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("504", vip, 290.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("505", vip, 295.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("506", vip, 295.00, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("507", vip, 300.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("508", vip, 305.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("509", vip, 310.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("510", vip, 320.00, EstadoHabitacion.DISPONIBLE);

        // ----- Luxury: piso 7-8, $350-$420 -----
        guardarHabitacion("701", luxury, 350.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("702", luxury, 355.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("703", luxury, 360.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("704", luxury, 370.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("705", luxury, 380.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("706", luxury, 380.00, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("707", luxury, 390.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("708", luxury, 400.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("709", luxury, 410.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("710", luxury, 420.00, EstadoHabitacion.DISPONIBLE);

        // ----- Presidential Suite: piso 9-10, $800-$1200 -----
        guardarHabitacion("901", presidential, 800.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("902", presidential, 850.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("903", presidential, 900.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("904", presidential, 950.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("905", presidential, 1000.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("906", presidential, 1000.00, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("907", presidential, 1050.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("908", presidential, 1100.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("909", presidential, 1150.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("910", presidential, 1200.00, EstadoHabitacion.DISPONIBLE);
    }

    // ===== Método de ayuda para no repetir el .builder() 50 veces =====
    private void guardarHabitacion(String numero, TipoHabitacion tipo, double precio, EstadoHabitacion estado) {
        habitacionRepository.save(Habitacion.builder()
                .numero(numero)
                .tipoHabitacion(tipo)
                .precio(precio)
                .estado(estado)
                .build());
    }
}