package com.finditnow.orderservice.config;

import com.finditnow.jwt.JwtService;
import com.finditnow.orderservice.security.JwtAuthFilter;
import com.finditnow.redis.RedisStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
    @Bean
    public JwtService jwtService() {
        return new JwtService();
    }

    @Bean
    public RedisStore redisService() {
        return RedisStore.getInstance();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService, RedisStore redisService) {
        return new JwtAuthFilter(jwtService, redisService);
    }
}
