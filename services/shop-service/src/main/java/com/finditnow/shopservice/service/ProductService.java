package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.PagedResponse;
import com.finditnow.shopservice.dto.ProductRequest;
import com.finditnow.shopservice.dto.ProductResponse;
import com.finditnow.shopservice.entity.Category;
import com.finditnow.shopservice.entity.Product;
import com.finditnow.shopservice.exception.ForbiddenException;
import com.finditnow.shopservice.exception.NotFoundException;
import com.finditnow.shopservice.mapper.ProductMapper;
import com.finditnow.shopservice.repository.CategoryRepository;
import com.finditnow.shopservice.repository.ProductRepository;
import com.finditnow.shopservice.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final ShopService shopService;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse addProduct(Long shopId, ProductRequest request, UUID ownerId) {
        Category cat = resolveCategory(request);
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(cat);

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(long id) {
        return productMapper.toDto(productRepository.findById(id).orElseThrow(() -> new NoSuchElementException("No product of given id->" + id)));
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, UUID ownerId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> {
            // Check if product exists at all
            if (!productRepository.existsById(productId)) {
                return new NotFoundException("Product not found with id: " + productId);
            }
            // Product exists but user doesn't own it
            return new ForbiddenException("You don't have permission to update this product");
        });

        Category category = resolveCategory(request);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long productId, UUID ownerId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> {
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
    public void deleteProducts(List<Long> productIds, UUID ownerId) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Product IDs list cannot be empty");
        }

        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new NotFoundException("One or more products not found");
        }

        productRepository.deleteAll(products);
    }

    private Category resolveCategory(ProductRequest request) {

        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
            try {
                Long categoryId = Long.parseLong(request.getCategoryId());
                return categoryRepository.findById(categoryId)
                        .orElseThrow(() ->
                                new NotFoundException("Category not found with id: " + categoryId));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid categoryId format");
            }
        }

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            return categoryRepository.findByNameIgnoreCase(request.getCategory())
                    .orElseThrow(() ->
                            new NotFoundException("Category not found with name: " + request.getCategory()));
        }

        throw new IllegalArgumentException("Either categoryId or category name must be provided");
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getCategory(), product.getImageUrl());
    }

    private PagedResponse<ProductResponse> mapToPagedResponse(Page<Product> userPage) {
        return new PagedResponse<>(productMapper.toDtoList(userPage.getContent()), userPage.getNumber(), userPage.getSize(), userPage.getTotalElements(), userPage.getTotalPages(), userPage.isFirst(), userPage.isLast());
    }
}