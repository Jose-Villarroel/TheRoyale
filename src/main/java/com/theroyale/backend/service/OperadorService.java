package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.repository.OperadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OperadorService {

    private final OperadorRepository operadorRepository;

    public OperadorService(OperadorRepository operadorRepository) {
        this.operadorRepository = operadorRepository;
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
}
