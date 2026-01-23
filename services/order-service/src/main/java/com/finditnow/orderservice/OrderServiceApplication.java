package com.finditnow.orderservice;

import com.finditnow.database.Database;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        Database.setEnv("order_service");

        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
