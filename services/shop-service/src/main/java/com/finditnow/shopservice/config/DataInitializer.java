//package com.finditnow.shopservice.config;
//
//import com.finditnow.shopservice.entity.User;
//import com.finditnow.shopservice.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@Configuration
//@RequiredArgsConstructor
//@Slf4j
//public class DataInitializer {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Bean
//    public CommandLineRunner initData() {
//        return args -> {
//            // Only create users if database is empty
//            if (userRepository.count() == 0) {
//                log.info("Creating test users...");
//
//                // Create admin user
//                User admin = new User();
//                admin.setUsername("admin");
//                admin.setPassword(passwordEncoder.encode("admin123")); // Auto-hashing!
//                admin.setRole("ADMIN");
//                userRepository.save(admin);
//                log.info("Created admin user: admin / admin123");
//
//                // Create shop owner user
//                User shopOwner = new User();
//                shopOwner.setUsername("shop1");
//                shopOwner.setPassword(passwordEncoder.encode("shop123")); // Auto-hashing!
//                shopOwner.setRole("SHOP");
//                userRepository.save(shopOwner);
//                log.info("Created shop owner user: shop1 / shop123");
//
//                log.info("Test users created successfully!");
//            } else {
//                log.info("Users already exist, skipping initialization");
//            }
//        };
//    }
//}
