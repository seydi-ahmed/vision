package com.exemple.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // permet d'utiliser @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Auth et register accessibles à tous
                        .requestMatchers("/api/auth/login", "/api/users/**").permitAll()

                        // Un visiteur peut consulter produits et quincailleries
                        .requestMatchers(HttpMethod.GET, "/api/quincailleries/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/produits/**").permitAll()

                        // Seuls OWNER peuvent gérer les quincailleries
                        .requestMatchers(HttpMethod.POST, "/api/quincailleries/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/quincailleries/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/quincailleries/**").hasRole("OWNER")

                        // OWNER ou MANAGER peuvent gérer les produits
                        .requestMatchers(HttpMethod.POST, "/api/produits/**").hasAnyRole("OWNER", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/produits/**").hasAnyRole("OWNER", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/produits/**").hasAnyRole("OWNER", "MANAGER")

                        // Tout le reste nécessite un utilisateur connecté
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
