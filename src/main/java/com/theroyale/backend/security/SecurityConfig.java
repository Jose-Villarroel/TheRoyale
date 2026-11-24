package com.theroyale.backend.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
// Habilita @PreAuthorize en los controladores: el del dashboard lo usa como segunda barrera,
// para que un cambio descuidado en los matchers de abajo no exponga las finanzas del hotel.
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthEntryPoint authEntryPoint;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .authorizeHttpRequests(auth -> auth
                        // El streaming SSE del chat termina en un despacho ASYNC: no se vuelve a autorizar
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()

                        // ===== Rutas públicas =====
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/images/**", "/css/**", "/js/**", "/favicon.ico", "/", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tipos-habitacion/**", "/api/habitaciones/**", "/api/servicios/**", "/api/disponibilidad/**", "/api/resenas/**", "/api/menu/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clientes").permitAll() // Registro público de cliente
                        // El concierge atiende a visitantes y clientes; LimiteMensajesService ya limita por IP
                        .requestMatchers("/api/chat/**").permitAll()

                        // ===== Rutas de administración (catálogo, clientes y moderación) =====
                        // Bajo /api/admin/** viven el dashboard de direccion (ingresos, caja y saldos
                        // de los huespedes) y el pronostico de ocupacion con sus recomendaciones de
                        // precio: la informacion mas sensible del sistema, solo para el administrador.
                        // Esta regla debe ir ANTES de anyRequest(), que solo exige estar autenticado.
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/operadores/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**", "/api/menu/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**", "/api/menu/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**", "/api/menu/**", "/api/clientes/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/clientes").hasAuthority("ROLE_ADMIN") // Solo la lista, no /{id} ni /details
                        .requestMatchers(HttpMethod.POST, "/api/resenas/*/ocultar", "/api/resenas/*/mostrar").hasAuthority("ROLE_ADMIN")

                        // ===== Rutas de operador =====
                        .requestMatchers("/api/reservas/buscar-habitacion").hasAnyAuthority("ROLE_OPERADOR", "ROLE_ADMIN")
                        .requestMatchers("/api/reservas/*/cuenta/**").hasAnyAuthority("ROLE_OPERADOR", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/check-in", "/api/reservas/*/check-out", "/api/reservas/*/confirmar").hasAuthority("ROLE_OPERADOR")
                        // "*" es un solo segmento: no alcanza a DELETE /api/reservas/{id}/plan
                        .requestMatchers(HttpMethod.DELETE, "/api/reservas/*").hasAuthority("ROLE_OPERADOR")

                        // ===== Rutas de cliente y generales autenticadas =====
                        // Reseñas y plan de visita son del huésped; el controlador valida que el clienteId sea el del token
                        .requestMatchers(HttpMethod.POST, "/api/resenas").hasAuthority("ROLE_CLIENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/resenas/*").hasAuthority("ROLE_CLIENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/resenas/*").hasAuthority("ROLE_CLIENTE")
                        .requestMatchers("/api/reservas/*/plan/**").hasAuthority("ROLE_CLIENTE")
                        // El cliente edita su perfil (solo el suyo, lo valida ClienteRestController) y el admin cualquiera
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/*").hasAnyAuthority("ROLE_CLIENTE", "ROLE_ADMIN")
                        // El cliente solo puede cancelar las suyas: lo valida ReservaRestController
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/cancelar").hasAnyAuthority("ROLE_CLIENTE", "ROLE_OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/reservas").hasAnyAuthority("ROLE_CLIENTE", "ROLE_OPERADOR", "ROLE_ADMIN")
                        // La pasarela Wompi (/api/pagos/wompi/**) la usa el cliente al confirmar su reserva
                        .requestMatchers("/api/clientes/**", "/api/reservas/**", "/api/pagos/**").authenticated()

                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
}
