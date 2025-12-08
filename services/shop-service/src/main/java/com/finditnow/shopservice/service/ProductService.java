package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.ProductRequest;
import com.finditnow.shopservice.dto.ProductResponse;
import com.finditnow.shopservice.entity.Product;
import com.finditnow.shopservice.entity.Shop;
import com.finditnow.shopservice.exception.ForbiddenException;
import com.finditnow.shopservice.exception.NotFoundException;
import com.finditnow.shopservice.repository.ProductRepository;
import com.finditnow.shopservice.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final ShopService shopService;

    @Transactional
    public ProductResponse addProduct(Long shopId, ProductRequest request, Long ownerId) {
        // Validate that the shop belongs to the owner
        if (!shopService.isOwner(shopId, ownerId)) {
            throw new ForbiddenException("You don't have permission to add products to this shop");
        }

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));

        Product product = new Product();
        product.setShop(shop);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByShop(Long shopId) {
        // Verify shop exists
        shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));

        List<Product> products = productRepository.findByShopId(shopId);
        return products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, Long ownerId) {
        Product product = productRepository.findByIdAndShopOwnerId(productId, ownerId)
                .orElseThrow(() -> {
                    // Check if product exists at all
                    if (!productRepository.existsById(productId)) {
                        return new NotFoundException("Product not found with id: " + productId);
                    }
                    // Product exists but user doesn't own it
                    return new ForbiddenException("You don't have permission to update this product");
                });

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long productId, Long ownerId) {
        Product product = productRepository.findByIdAndShopOwnerId(productId, ownerId)
                .orElseThrow(() -> {
                    // Check if product exists at all
                    if (!productRepository.existsById(productId)) {
                        return new NotFoundException("Product not found with id: " + productId);
                    }
                    // Product exists but user doesn't own it
                    return new ForbiddenException("You don't have permission to delete this product");
                });

        productRepository.delete(product);
    }

    @Transactional
    public void deleteProducts(List<Long> productIds, Long ownerId) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Product IDs list cannot be empty");
        }

        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new NotFoundException("One or more products not found");
        }

        // Verify ownership for all products
        for (Product product : products) {
            if (!product.getShop().getOwnerId().equals(ownerId)) {
                throw new ForbiddenException("You don't have permission to delete product: " + product.getName());
            }
        }

        productRepository.deleteAll(products);
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getShop().getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory());
    }
}