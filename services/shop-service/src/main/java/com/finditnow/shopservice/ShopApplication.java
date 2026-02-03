package com.finditnow.shopservice;

import com.finditnow.config.Config;
import com.finditnow.database.Database;
import com.finditnow.interservice.InterServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShopApplication {

    public static void main(String[] args) {
        Database.setEnv("shop_service");

        InterServiceClient.init("shop-service", Config.get("SHOP_SERVICE_SECRET","verylongunimaginablesecret"));
        
        SpringApplication.run(ShopApplication.class, args);
    }

}

