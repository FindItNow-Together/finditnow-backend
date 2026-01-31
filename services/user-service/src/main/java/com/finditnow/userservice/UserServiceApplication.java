package com.finditnow.userservice;

import com.finditnow.config.Config;
import com.finditnow.database.Database;
import com.finditnow.interservice.InterServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        Database.setEnv("user_service");
        InterServiceClient.init("user-service", Config.get("USER_SERVICE_SECRET", "verylongunimaginablesecret"));

        SpringApplication.run(UserServiceApplication.class, args);
    }
}

