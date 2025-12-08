package com.finditnow.userservice;

import com.finditnow.database.Database;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        Database.setEnv("user_service");

        SpringApplication.run(UserServiceApplication.class, args);
    }
}

