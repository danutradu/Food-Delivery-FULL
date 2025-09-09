package com.example.food.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()) // CSRF disabled for stateless JWT-based API
        .authorizeHttpRequests(reg -> reg
            .requestMatchers("/actuator/**", "/auth/**", "/.well-known/jwks.json").permitAll()
            .anyRequest().authenticated());
    return http.build();
  }
}
