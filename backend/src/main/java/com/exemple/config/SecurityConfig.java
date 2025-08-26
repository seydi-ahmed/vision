package com.exemple.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // permet d'utiliser @PreAuthorize dans les contrôleurs/services
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Autoriser les visiteurs (non authentifiés) à faire seulement des GET
                        .requestMatchers("/api/quincailleries/**", "/api/produits/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll() // pour tester facilement la création
                                                                      // d’utilisateurs
                        // Toute autre requête (PUT, POST, DELETE) exige une authentification
                        .anyRequest().authenticated())
                .httpBasic(httpBasic -> httpBasic.disable()) // on ne veut plus Basic Auth
                .formLogin(form -> form.disable()); // pas de formulaire

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
