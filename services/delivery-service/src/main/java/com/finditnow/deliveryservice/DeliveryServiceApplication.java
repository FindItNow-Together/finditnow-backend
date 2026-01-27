package com.finditnow.deliveryservice;

import com.finditnow.database.Database;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class DeliveryServiceApplication {
    public static void main(String[] args) {
        Database.setEnv("delivery_db");
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
