package com.finditnow.deliveryservice;

import com.finditnow.config.Config;
import com.finditnow.database.Database;
import com.finditnow.interservice.InterServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeliveryServiceApplication {
    public static void main(String[] args) {
        Database.setEnv("delivery_db");

        InterServiceClient.init("delivery-service", Config.get("DELIVERY_SERVICE_SECRET","verylongunimaginablesecret"));

        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
