package com.finditnow.shopservice.config;

import com.finditnow.shopservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - search and categories
                        .requestMatchers(
                                "/search/**",
                                "/categories/**",
                                "/api/search/**",
                                "/api/categories/**",
                                "/api/shops/search"
                        ).permitAll()

                        // Allow public READ access to shops, products, and inventory
                        .requestMatchers("GET", "/shop/**", "/api/shop/**").permitAll()
                        .requestMatchers("GET", "/product/**", "/api/product/**").permitAll()

                        // Require authentication for CREATE/UPDATE/DELETE operations
                        .requestMatchers("POST", "/shop/**", "/api/shop/**").hasAnyRole("SHOP", "ADMIN")
                        .requestMatchers("PUT", "/shop/**", "/api/shop/**").hasAnyRole("SHOP", "ADMIN")
                        .requestMatchers("DELETE", "/shop/**", "/api/shop/**").hasAnyRole("SHOP", "ADMIN")
                        .requestMatchers("POST", "/product/**", "/api/product/**").hasAnyRole("SHOP", "ADMIN")
                        .requestMatchers("PUT", "/product/**", "/api/product/**").hasAnyRole("SHOP", "ADMIN")
                        .requestMatchers("DELETE", "/product/**", "/api/product/**").hasAnyRole("SHOP", "ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(Arrays.asList("*"));
//        configuration.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
}