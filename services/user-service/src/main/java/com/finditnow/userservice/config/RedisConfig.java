package com.finditnow.userservice.config;

import com.finditnow.redis.RedisStore;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RedisConfig {
    @Bean
    public RedisStore redisService() {
        return RedisStore.getInstance();
    }
}
