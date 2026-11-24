package com.theroyale.backend.security;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ===== Pruebas de las reglas reales del SecurityFilterChain (SecurityConfig) =====
// Contexto completo con el filtro JWT y H2 del perfil "test" (sin DataLoader). El usuario se simula
// con @WithMockUser(authorities = ...), que deja la autenticación en el SecurityContextHolder igual que
// el JwtAuthenticationFilter. Cuando la base está vacía, "permitido" significa que la regla deja pasar
// la petición (no 401/403), aunque el controlador luego responda 404 o 409.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityRulesTest {

    private static final String EMAIL_CLIENTE = "cliente.reglas@email.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;

    // Cliente real (con su UserEntity) cuyo email coincide con el usuario simulado de @WithMockUser
    private Long clienteId;

    @BeforeEach
    void crearClienteDePrueba() {
        Cliente cliente = clienteService.crear(Cliente.builder()
                .nombre("Carla")
                .apellido("Reglas")
                .email(EMAIL_CLIENTE)
                .password("pass1234")
                .cedula("REGLAS-001")
                .build());
        clienteId = cliente.getId();
    }

    // ===== Anónimo =====
    @Nested
    class Anonimo {

        // ===== El dashboard de direccion no se consulta sin sesion =====
        @Test
        void dashboardDeAdmin_sinToken_retorna401() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/resumen")).andExpect(status().isUnauthorized());
        }

        @Test
        void reservas_sinToken_retorna401() throws Exception {
            mockMvc.perform(get("/api/reservas")).andExpect(status().isUnauthorized());
        }

        @Test
        void menu_esPublico() throws Exception {
            mockMvc.perform(get("/api/menu")).andExpect(status().isOk());
        }

        @Test
        void estadoDelChat_esPublico() throws Exception {
            mockMvc.perform(get("/api/chat/estado")).andExpect(status().isOk());
        }

        // Token válido de un usuario que ya no existe (p. ej. cambió su email): 401 limpio, no 500
        @Test
        void tokenDeUsuarioInexistente_retorna401EnRutaProtegidaY200EnPublica() throws Exception {
            String token = jwtTokenGenerator.generateToken(
                    new UsernamePasswordAuthenticationToken("ya.no.existe@email.com", null, List.of()));

            mockMvc.perform(get("/api/reservas").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/menu").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    // ===== Cliente =====
    @Nested
    @WithMockUser(username = EMAIL_CLIENTE, authorities = "ROLE_CLIENTE")
    class ClienteAutenticado {

        // ===== Ingresos, caja y saldos de los huespedes no son datos del huesped =====
        // Sin la regla explicita de /api/admin/**, esta peticion caeria en
        // anyRequest().authenticated() y devolveria 200 a cualquier cliente con sesion.
        @Test
        void dashboardDeAdmin_retorna403() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/resumen")).andExpect(status().isForbidden());
        }

        @Test
        void crearPlato_retorna403() throws Exception {
            mockMvc.perform(post("/api/menu")).andExpect(status().isForbidden());
        }

        @Test
        void checkIn_retorna403() throws Exception {
            mockMvc.perform(post("/api/reservas/1/check-in")).andExpect(status().isForbidden());
        }

        @Test
        void checkOut_retorna403() throws Exception {
            mockMvc.perform(post("/api/reservas/1/check-out")).andExpect(status().isForbidden());
        }

        @Test
        void confirmar_retorna403() throws Exception {
            mockMvc.perform(post("/api/reservas/1/confirmar")).andExpect(status().isForbidden());
        }

        @Test
        void eliminarReserva_retorna403() throws Exception {
            mockMvc.perform(delete("/api/reservas/1")).andExpect(status().isForbidden());
        }

        @Test
        void listarClientes_retorna403() throws Exception {
            mockMvc.perform(get("/api/clientes")).andExpect(status().isForbidden());
        }

        @Test
        void ocultarResena_retorna403() throws Exception {
            mockMvc.perform(post("/api/resenas/1/ocultar")).andExpect(status().isForbidden());
        }

        @Test
        void mostrarResena_retorna403() throws Exception {
            mockMvc.perform(post("/api/resenas/1/mostrar")).andExpect(status().isForbidden());
        }

        @Test
        void reservasDeOtroCliente_retorna403() throws Exception {
            mockMvc.perform(get("/api/reservas").param("clienteId", String.valueOf(clienteId + 1000)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void reservasSinClienteId_retorna403() throws Exception {
            mockMvc.perform(get("/api/reservas")).andExpect(status().isForbidden());
        }

        @Test
        void susPropiasReservas_retorna200() throws Exception {
            mockMvc.perform(get("/api/reservas").param("clienteId", String.valueOf(clienteId)))
                    .andExpect(status().isOk());
        }

        // Las rutas de un solo segmento no deben atrapar las anidadas: /plan sigue siendo del cliente
        @Test
        void eliminarSuPlan_noLoBloqueaLaReglaDeEliminarReserva() throws Exception {
            mockMvc.perform(delete("/api/reservas/1/plan")).andExpect(permitido());
        }

        @Test
        void cancelar_loPermiteLaRegla() throws Exception {
            mockMvc.perform(post("/api/reservas/1/cancelar")).andExpect(permitido());
        }

        @Test
        void susDetalles_retorna200() throws Exception {
            mockMvc.perform(get("/api/clientes/details")).andExpect(status().isOk());
        }
        // ===== Datos de otros clientes: el clienteId de la petición debe ser el del token =====
        @Test
        void reservarAnombreDeOtroCliente_retorna403() throws Exception {
            mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"clienteId\":" + otroClienteId() + ",\"tipoHabitacionId\":1}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void verPerfilDeOtroCliente_retorna403() throws Exception {
            mockMvc.perform(get("/api/clientes/" + otroClienteId())).andExpect(status().isForbidden());
        }

        @Test
        void verSuPerfil_retorna200() throws Exception {
            mockMvc.perform(get("/api/clientes/" + clienteId)).andExpect(status().isOk());
        }

        @Test
        void editarPerfilDeOtroCliente_retorna403() throws Exception {
            mockMvc.perform(put("/api/clientes/" + otroClienteId()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"X\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void crearResenaComoOtroCliente_retorna403() throws Exception {
            mockMvc.perform(post("/api/resenas").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reservaId\":1,\"clienteId\":" + otroClienteId() + ",\"calificacion\":5}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void editarResenaComoOtroCliente_retorna403() throws Exception {
            mockMvc.perform(put("/api/resenas/1").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"clienteId\":" + otroClienteId() + ",\"calificacion\":4}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void eliminarResenaComoOtroCliente_retorna403() throws Exception {
            mockMvc.perform(delete("/api/resenas/1").param("clienteId", String.valueOf(otroClienteId())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void verPlanDeOtroCliente_retorna403() throws Exception {
            mockMvc.perform(get("/api/reservas/1/plan").param("clienteId", String.valueOf(otroClienteId())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void eliminarPlanDeOtroCliente_retorna403() throws Exception {
            mockMvc.perform(delete("/api/reservas/1/plan").param("clienteId", String.valueOf(otroClienteId())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void agregarBloqueDelPlanDeOtroCliente_retorna403() throws Exception {
            mockMvc.perform(post("/api/reservas/1/plan/bloques/b1/agregar-a-cuenta").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"clienteId\":" + otroClienteId() + "}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===== Operador =====
    @Nested
    @WithMockUser(username = "operador.reglas@theroyale.com", authorities = "ROLE_OPERADOR")
    class OperadorAutenticado {

        // ===== El operador opera, no dirige: no ve las finanzas del hotel =====
        @Test
        void dashboardDeAdmin_retorna403() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/resumen")).andExpect(status().isForbidden());
        }

        @Test
        void checkIn_loPermiteLaRegla() throws Exception {
            mockMvc.perform(post("/api/reservas/1/check-in")).andExpect(permitido());
        }

        @Test
        void checkOut_loPermiteLaRegla() throws Exception {
            mockMvc.perform(post("/api/reservas/1/check-out")).andExpect(permitido());
        }

        @Test
        void eliminarReserva_loPermiteLaRegla() throws Exception {
            mockMvc.perform(delete("/api/reservas/1")).andExpect(permitido());
        }

        @Test
        void verCuenta_loPermiteLaRegla() throws Exception {
            mockMvc.perform(get("/api/reservas/1/cuenta")).andExpect(permitido());
        }

        @Test
        void todasLasReservas_retorna200() throws Exception {
            mockMvc.perform(get("/api/reservas")).andExpect(status().isOk());
        }

        @Test
        void crearPlato_retorna403() throws Exception {
            mockMvc.perform(post("/api/menu")).andExpect(status().isForbidden());
        }

        @Test
        void listarClientes_retorna403() throws Exception {
            mockMvc.perform(get("/api/clientes")).andExpect(status().isForbidden());
        }
        // Reseñas, plan de visita y edición de perfiles no son tareas del operador
        @Test
        void crearResena_retorna403() throws Exception {
            mockMvc.perform(post("/api/resenas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void verPlan_retorna403() throws Exception {
            mockMvc.perform(get("/api/reservas/1/plan").param("clienteId", "1")).andExpect(status().isForbidden());
        }

        @Test
        void editarCliente_retorna403() throws Exception {
            mockMvc.perform(put("/api/clientes/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===== Admin =====
    @Nested
    @WithMockUser(username = "admin.reglas@theroyale.com", authorities = "ROLE_ADMIN")
    class AdminAutenticado {

        // ===== El administrador si entra, y con la base vacia responde ceros, no un error =====
        @Test
        void dashboardDeAdmin_retorna200() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/resumen")).andExpect(status().isOk());
        }

        @Test
        void dashboardDeAdmin_conFechasInvertidas_retorna400() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/resumen")
                            .param("desde", "2026-03-10")
                            .param("hasta", "2026-03-01"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void todasLasReservas_retorna200() throws Exception {
            mockMvc.perform(get("/api/reservas")).andExpect(status().isOk());
        }

        @Test
        void listarClientes_retorna200() throws Exception {
            mockMvc.perform(get("/api/clientes")).andExpect(status().isOk());
        }

        @Test
        void checkIn_retorna403() throws Exception {
            mockMvc.perform(post("/api/reservas/1/check-in")).andExpect(status().isForbidden());
        }

        @Test
        void confirmar_retorna403() throws Exception {
            mockMvc.perform(post("/api/reservas/1/confirmar")).andExpect(status().isForbidden());
        }
        // El admin gestiona clientes: puede ver y editar cualquiera (la regla no lo bloquea)
        @Test
        void verYEditarCualquierCliente_loPermiteLaRegla() throws Exception {
            mockMvc.perform(get("/api/clientes/" + clienteId)).andExpect(status().isOk());
            mockMvc.perform(put("/api/clientes/" + clienteId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(permitido());
        }

        @Test
        void eliminarResena_retorna403() throws Exception {
            mockMvc.perform(delete("/api/resenas/1").param("clienteId", "1")).andExpect(status().isForbidden());
        }

        // Pronóstico de ocupación y precios dinámicos: solo el admin
        @Test
        void ocupacionYPrecios_loPermiteLaRegla() throws Exception {
            mockMvc.perform(get("/api/admin/ocupacion").param("dias", "30")).andExpect(status().isOk());
            mockMvc.perform(get("/api/admin/temporadas")).andExpect(status().isOk());
            mockMvc.perform(get("/api/admin/precios/tarifas")).andExpect(status().isOk());
        }
    }

    // ===== /api/admin/** es exclusivo del admin =====
    @Nested
    class PanelAdminPrivado {

        @Test
        void ocupacion_sinToken_retorna401() throws Exception {
            mockMvc.perform(get("/api/admin/ocupacion")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = EMAIL_CLIENTE, authorities = "ROLE_CLIENTE")
        void ocupacion_comoCliente_retorna403() throws Exception {
            mockMvc.perform(get("/api/admin/ocupacion")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "operador.reglas@theroyale.com", authorities = "ROLE_OPERADOR")
        void aplicarPrecio_comoOperador_retorna403() throws Exception {
            mockMvc.perform(post("/api/admin/precios/recomendaciones/1/aplicar")).andExpect(status().isForbidden());
        }
    }

    // Un id distinto al del cliente simulado (no hace falta que exista: se compara con el del token)
    private Long otroClienteId() {
        return clienteId + 1000;
    }

    // La regla de seguridad dejó pasar la petición; el resultado de negocio (404, 409...) no importa aquí
    private static ResultMatcher permitido() {
        return resultado -> assertThat(resultado.getResponse().getStatus()).isNotIn(401, 403);
    }
}
