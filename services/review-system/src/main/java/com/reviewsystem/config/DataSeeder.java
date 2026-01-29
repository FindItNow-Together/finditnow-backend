package com.reviewsystem.config;

import com.reviewsystem.entity.Product;
import com.reviewsystem.entity.Shop;
import com.reviewsystem.repository.ProductRepository;
import com.reviewsystem.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Override
    public void run(String... args) throws Exception {
        seedShops();
        seedProducts();
    }

    private void seedShops() {
        if (shopRepository.count() == 0) {
            Shop shop1 = new Shop();
            shop1.setName("Tech Haven");
            shop1.setDescription("Premium electronics and gadgets.");
            shop1.setLogoUrl("/placeholder-shop-1.jpg");
            shop1.setOwnerId(1L);

            Shop shop2 = new Shop();
            shop2.setName("Fashion Forward");
            shop2.setDescription("Trendy clothing for everyone.");
            shop2.setLogoUrl("/placeholder-shop-2.jpg");
            shop2.setOwnerId(2L);

            Shop shop3 = new Shop();
            shop3.setName("Home Essentials");
            shop3.setDescription("Everything you need for your home.");
            shop3.setLogoUrl("/placeholder-shop-3.jpg");
            shop3.setOwnerId(3L);

            shopRepository.saveAll(Arrays.asList(shop1, shop2, shop3));
            System.out.println("Seeded 3 dummy shops.");
        }
    }

    private void seedProducts() {
        if (productRepository.count() == 0) {
            Product p1 = new Product();
            p1.setName("Wireless Noise-Canceling Headphones");
            p1.setDescription("Experience silence with our top-tier noise-canceling headphones.");
            p1.setPrice(new BigDecimal("299.99"));
            p1.setImageUrl("/placeholder-product-1.jpg");
            p1.setShopId(1L);

            Product p2 = new Product();
            p2.setName("Smartphone X");
            p2.setDescription("The latest smartphone with 5G connectivity.");
            p2.setPrice(new BigDecimal("999.99"));
            p2.setImageUrl("/placeholder-product-2.jpg");
            p2.setShopId(1L);

            Product p3 = new Product();
            p3.setName("Denim Jacket");
            p3.setDescription("Classic denim jacket for all seasons.");
            p3.setPrice(new BigDecimal("79.50"));
            p3.setImageUrl("/placeholder-product-3.jpg");
            p3.setShopId(2L);

            Product p4 = new Product();
            p4.setName("Coffee Maker");
            p4.setDescription("Brew the perfect cup every morning.");
            p4.setPrice(new BigDecimal("120.00"));
            p4.setImageUrl("/placeholder-product-4.jpg");
            p4.setShopId(3L);

            productRepository.saveAll(Arrays.asList(p1, p2, p3, p4));
            System.out.println("Seeded 4 dummy products.");
        }
    }
}
