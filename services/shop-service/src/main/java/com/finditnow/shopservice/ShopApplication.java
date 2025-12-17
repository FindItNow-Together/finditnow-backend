package com.finditnow.shopservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.finditnow.database.Database;

@SpringBootApplication
public class ShopApplication {

    public static void main(String[] args) {
        Database.setEnv("shop_service");
        
        SpringApplication.run(ShopApplication.class, args);
    }

}

