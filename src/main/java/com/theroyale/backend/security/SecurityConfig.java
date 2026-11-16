package com.theroyale.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
public class SecurityConfig {

    @Autowired
    private JwtAuthEntryPoint authEntryPoint;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/images/**", "/css/**", "/js/**", "/favicon.ico", "/", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tipos-habitacion/**", "/api/habitaciones/**", "/api/servicios/**", "/api/disponibilidad/**", "/api/resenas/**", "/api/platos-menu/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clientes").permitAll() // Registro público de cliente
                        
                        // Rutas de administración
                        .requestMatchers("/api/operadores/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**", "/api/clientes/**").hasAuthority("ROLE_ADMIN")

                        // Rutas de operador
                        .requestMatchers("/api/reservas/buscar-habitacion").hasAnyAuthority("ROLE_OPERADOR", "ROLE_ADMIN")
                        .requestMatchers("/api/reservas/*/cuenta/**").hasAnyAuthority("ROLE_OPERADOR", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservas/*/check-in", "/api/reservas/*/check-out").hasAnyAuthority("ROLE_OPERADOR", "ROLE_ADMIN")

                        // Rutas de cliente y generales autenticadas
                        .requestMatchers(HttpMethod.POST, "/api/reservas").hasAnyAuthority("ROLE_CLIENTE", "ROLE_OPERADOR", "ROLE_ADMIN")
                        .requestMatchers("/api/clientes/**", "/api/reservas/**", "/api/chat/**", "/api/plan-visita/**").authenticated()

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
