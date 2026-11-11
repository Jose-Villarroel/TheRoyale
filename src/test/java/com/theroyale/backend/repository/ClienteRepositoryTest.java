package com.theroyale.backend.repository;

import com.theroyale.backend.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

// ===== Pruebas del CRUD de ClienteRepository =====
// @DataJpaTest levanta solo la capa JPA con una base H2 propia de las pruebas, distinta a la de la
// aplicación, y no ejecuta el DataLoader. Cada prueba corre en una transacción que se deshace al
// terminar, así que las pruebas no se afectan entre sí.
@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    // Permite forzar la escritura en la base y vaciar la caché de la sesión (ver pruebas de update y delete)
    @Autowired
    private TestEntityManager entityManager;

    // Cliente guardado en el setUp, usado como referencia en varias pruebas
    private Cliente carlos;

    // ===== Datos comunes: se ejecuta antes de CADA prueba (la base queda con 3 clientes) =====
    @BeforeEach
    void setUp() {
        carlos = clienteRepository.save(crearCliente("Carlos", "Mendoza", "carlos.mendoza@email.com", "1001001001"));
        clienteRepository.save(crearCliente("Ana", "García", "ana.garcia@email.com", "1001001002"));
        clienteRepository.save(crearCliente("Luis", "Hernández", "luis.hernandez@email.com", "1001001003"));
    }

    // ===== CREATE =====
    @Test
    void clienteRepository_save_retornaClienteConId() {
        // Arrange: un cliente nuevo, que todavía no tiene id
        Cliente nuevo = crearCliente("Sofia", "Torres", "sofia.torres@email.com", "1001001004");

        // Act
        Cliente guardado = clienteRepository.save(nuevo);

        // Assert: la base le asignó un id y conservó los datos enviados
        assertThat(guardado).isNotNull();
        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getEmail()).isEqualTo("sofia.torres@email.com");
        assertThat(guardado.getCedula()).isEqualTo("1001001004");
    }

    // ===== READ: todos =====
    @Test
    void clienteRepository_findAll_retornaTodosLosClientes() {
        // Arrange: el setUp ya guardó 3 clientes

        // Act
        List<Cliente> clientes = clienteRepository.findAll();

        // Assert
        assertThat(clientes).isNotNull();
        assertThat(clientes).hasSize(3);
        assertThat(clientes).extracting(Cliente::getEmail)
                .contains("carlos.mendoza@email.com", "ana.garcia@email.com", "luis.hernandez@email.com");
    }

    // ===== READ: uno por id =====
    @Test
    void clienteRepository_findById_idExistente_retornaElCliente() {
        // Arrange: id de un cliente guardado en el setUp
        Long idExistente = carlos.getId();

        // Act
        Optional<Cliente> encontrado = clienteRepository.findById(idExistente);

        // Assert
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNombre()).isEqualTo("Carlos");
        assertThat(encontrado.get().getApellido()).isEqualTo("Mendoza");
    }

    // ===== READ: caso en que las cosas salen "mal" =====
    @Test
    void clienteRepository_findById_idInexistente_retornaVacio() {
        // Arrange: un id que no existe
        Long idInexistente = -1L;

        // Act
        Optional<Cliente> encontrado = clienteRepository.findById(idInexistente);

        // Assert: un Optional sin valor se verifica con isEmpty(), no con isNull()
        assertThat(encontrado).isEmpty();
    }

    // ===== UPDATE =====
    @Test
    void clienteRepository_save_clienteExistente_actualizaSusDatos() {
        // Arrange: se trae un cliente y se le cambia el teléfono
        Cliente aActualizar = clienteRepository.findById(carlos.getId()).orElseThrow();
        aActualizar.setTelefono("+1-555-9999");

        // Act
        clienteRepository.save(aActualizar);
        // Se fuerza la escritura y se vacía la caché de la sesión: sin esto, el findById siguiente
        // devolvería el mismo objeto de memoria y la prueba pasaría aunque la base no cambiara
        entityManager.flush();
        entityManager.clear();
        Cliente recargado = clienteRepository.findById(carlos.getId()).orElseThrow();

        // Assert: el teléfono cambió y el resto de datos sigue igual
        assertThat(recargado.getTelefono()).isEqualTo("+1-555-9999");
        assertThat(recargado.getEmail()).isEqualTo("carlos.mendoza@email.com");
        assertThat(clienteRepository.findAll()).hasSize(3);
    }

    // ===== DELETE =====
    @Test
    void clienteRepository_deleteById_clienteExistente_yaNoSeEncuentra() {
        // Arrange: id de un cliente que sí existe
        Long idABorrar = carlos.getId();

        // Act
        clienteRepository.deleteById(idABorrar);
        entityManager.flush();
        entityManager.clear();

        // Assert: ya no se encuentra y quedan los otros 2
        assertThat(clienteRepository.findById(idABorrar)).isEmpty();
        assertThat(clienteRepository.findAll()).hasSize(2);
    }

    // ===== Método de ayuda: evita repetir el builder en cada prueba =====
    // Cada cliente necesita email y cédula distintos porque ambos son únicos
    private Cliente crearCliente(String nombre, String apellido, String email, String cedula) {
        return Cliente.builder()
                .nombre(nombre)
                .apellido(apellido)
                .email(email)
                .password("pass1234")
                .cedula(cedula)
                .telefono("+1-555-0100")
                .fechaRegistro(LocalDate.of(2025, 1, 15))
                .build();
    }
}