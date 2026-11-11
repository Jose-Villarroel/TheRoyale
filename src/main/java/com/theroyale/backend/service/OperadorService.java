package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.model.TipoOperador;
import com.theroyale.backend.repository.ItemConsumoRepository;
import com.theroyale.backend.repository.OperadorRepository;
import com.theroyale.backend.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// ===== Reglas del personal del hotel: login, CRUD y operador por defecto =====
// Un operador con consumos o pagos registrados no se puede borrar, para conservar quién hizo cada cargo.
@Service
@Transactional(readOnly = true)
public class OperadorService {

    @Autowired
    private OperadorRepository operadorRepository;

    @Autowired
    private ItemConsumoRepository itemConsumoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    public List<Operador> listarTodos() {
        return operadorRepository.findAllByOrderByNombreAsc();
    }

    public Optional<Operador> autenticar(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }

        return operadorRepository.findByEmail(email.trim().toLowerCase())
                .filter(operador -> password.equals(operador.getPassword()));
    }

    public Optional<Operador> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return operadorRepository.findById(id);
    }

    public Operador obtenerPorId(Long id) {
        return buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operador no encontrado: " + id));
    }

    // /operator no exige login por ahora: si no hay operador en la sesion (o ya no existe),
    // las acciones se atribuyen al primer operador registrado, porque consumos y pagos requieren uno.
    public Operador obtenerOPredeterminado(Long id) {
        return buscarPorId(id)
                .or(() -> operadorRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("No hay operadores registrados en el sistema."));
    }

    @Transactional
    public Operador crear(Operador operador) {
        validarOperador(operador, true);

        String email = normalizarEmail(operador.getEmail());
        if (operadorRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An operator with email " + email + " already exists.");
        }

        operador.setId(null);
        operador.setEmail(email);
        if (operador.getTipo() == null) {
            operador.setTipo(TipoOperador.OPERADOR);
        }
        return operadorRepository.save(operador);
    }

    @Transactional
    public Operador actualizar(Long id, Operador operadorActualizado) {
        Operador existente = obtenerPorId(id);

        validarOperador(operadorActualizado, false);

        String email = normalizarEmail(operadorActualizado.getEmail());
        Optional<Operador> conEseEmail = operadorRepository.findByEmail(email);
        if (conEseEmail.isPresent() && !conEseEmail.get().getId().equals(id)) {
            throw new IllegalArgumentException("An operator with email " + email + " already exists.");
        }

        existente.setNombre(operadorActualizado.getNombre());
        existente.setEmail(email);
        // Password vacio = conservar el actual, igual que en ClienteService.actualizar
        if (!estaVacio(operadorActualizado.getPassword())) {
            existente.setPassword(operadorActualizado.getPassword());
        }
        if (operadorActualizado.getTipo() != null) {
            existente.setTipo(operadorActualizado.getTipo());
        }

        return operadorRepository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        obtenerPorId(id);

        // Los consumos y pagos guardan quien los registro: borrar al operador dejaria huerfano ese historial
        if (itemConsumoRepository.existsByOperadorId(id) || pagoRepository.existsByOperadorId(id)) {
            throw new IllegalStateException(
                    "This operator has charges or payments on record and cannot be deleted.");
        }

        operadorRepository.deleteById(id);
    }

    private void validarOperador(Operador operador, boolean requierePassword) {
        if (operador == null) {
            throw new IllegalArgumentException("Operator is required.");
        }
        if (estaVacio(operador.getNombre()) || estaVacio(operador.getEmail())) {
            throw new IllegalArgumentException("Name and email are required.");
        }
        if (requierePassword && estaVacio(operador.getPassword())) {
            throw new IllegalArgumentException("Password is required.");
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
