package com.finditnow.userservice;

import com.finditnow.userservice.config.EnvBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        EnvBootstrap.setEnv();

        SpringApplication.run(UserServiceApplication.class, args);
    }
}

