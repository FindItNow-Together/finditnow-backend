package com.finditnow.shopservice.config;

import com.finditnow.userservice.security.JwtAuthFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<Filter> jwtFilter(JwtAuthFilter filter) {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);

        // Protect API routes but let public routes pass
        bean.addUrlPatterns("/users/*");

        return bean;
    }
}