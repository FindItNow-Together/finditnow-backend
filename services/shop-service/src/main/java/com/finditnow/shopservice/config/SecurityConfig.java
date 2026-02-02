package com.finditnow.shopservice.config;

import com.finditnow.shopservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                // Disable HTTP Basic authentication (removes default password requirement)
                .httpBasic(httpBasic -> httpBasic.disable())
                // Disable form login (removes default login page)
                .formLogin(formLogin -> formLogin.disable()).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(auth -> auth
                        // ============ PUBLIC ENDPOINTS ============
                        // Search and categories - fully public
                        .requestMatchers("/search/**", "/categories/**").permitAll()

                        // Shop search endpoint - public
                        .requestMatchers("/shops/search").permitAll()

                        // ============ SHOP ENDPOINTS ============
                        // Public READ access to shops
                        .requestMatchers(HttpMethod.GET, "/shop/**").permitAll()

                        // Require authentication for CREATE/UPDATE/DELETE on shops
                        .requestMatchers(HttpMethod.POST, "/shop/**").hasAnyRole("SHOP", "ADMIN").requestMatchers(HttpMethod.PUT, "/shop/**").hasAnyRole("SHOP", "ADMIN").requestMatchers(HttpMethod.DELETE, "/shop/**").hasAnyRole("SHOP", "ADMIN")

                        // ============ PRODUCT ENDPOINTS ============
                        // Public READ access to products
                        .requestMatchers(HttpMethod.GET, "/product/**").permitAll()

                        // Require authentication for CREATE/UPDATE/DELETE on products
                        .requestMatchers(HttpMethod.POST, "/product/**").hasAnyRole("SHOP", "ADMIN").requestMatchers(HttpMethod.PUT, "/product/**").hasAnyRole("SHOP", "ADMIN").requestMatchers(HttpMethod.DELETE, "/product/**").hasAnyRole("SHOP", "ADMIN")

                        // ============ CART ENDPOINTS ============
                        // Cart requires authentication for ALL operations
                        // (removed .permitAll() for cart - uses @PreAuthorize in controller)

                        // ============ INTERNAL SERVICE ENDPOINTS ============
                        // Internal endpoints require SERVICE role
                        .requestMatchers("/cart/*/internal/**").hasRole("SERVICE")

                        // ============ DEFAULT ============
                        // All other requests require authentication
                        .anyRequest().authenticated()).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}