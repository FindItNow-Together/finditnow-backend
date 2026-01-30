package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.CategoryResponse;
import com.finditnow.shopservice.dto.PagedResponse;
import com.finditnow.shopservice.dto.ProductRequest;
import com.finditnow.shopservice.dto.ProductResponse;
import com.finditnow.shopservice.entity.Category;
import com.finditnow.shopservice.entity.Product;
import com.finditnow.shopservice.exception.ForbiddenException;
import com.finditnow.shopservice.exception.NotFoundException;
import com.finditnow.shopservice.mapper.ProductMapper;
import com.finditnow.shopservice.entity.Shop;
import com.finditnow.shopservice.entity.ShopInventory;
import com.finditnow.shopservice.repository.CategoryRepository;
import com.finditnow.shopservice.repository.ProductRepository;
import com.finditnow.shopservice.repository.ShopInventoryRepository;
import com.finditnow.shopservice.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ShopInventoryRepository shopInventoryRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse addProduct(ProductRequest request, UUID creatorId) {
        // Global catalog product creation (inventory linking happens separately)
        Category cat = resolveCategory(request);

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(cat);

        Product saved = productRepository.save(product);
        return productMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        List<Product> products = productRepository.findAll();
        return productMapper.toDtoList(products);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByShop(Long shopId) {
        List<ShopInventory> inventories = shopInventoryRepository.findByShopId(shopId);
        return inventories.stream()
                .map(inv -> mapToResponse(inv.getProduct(), inv))
                .collect(Collectors.toList());
    }

    /**
     * Returns all products with pagination support.
     * 
     * @param page The page number (0-indexed)
     * @param size The page size
     * @return PagedResponse with products
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findAll(pageable);
        
        List<ProductResponse> content = productPage.getContent().stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
        
        return new PagedResponse<>(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String query) {
        List<Product> products = productRepository.findByNameContainingIgnoreCase(query);
        return products.stream().map(productMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No product of given id->" + id));
        return productMapper.toDto(product);
    }

    /**
     * Legacy helper: create a product and immediately add it to a shop's inventory.
     * Prefer creating via {@link #addProduct(ProductRequest, UUID)} + inventory APIs instead.
     */
    @Transactional
    public ProductResponse addProduct(ProductRequest request, Long shopId, UUID ownerId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));

        if (!shop.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You don't have permission to add products to this shop");
        }

        Category cat = resolveCategory(request);
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(cat);

        Product savedProduct = productRepository.save(product);

        ShopInventory inventory = new ShopInventory();
        inventory.setProduct(savedProduct);
        inventory.setShop(shop);
        inventory.setPrice(request.getPrice() != null ? request.getPrice() : 0.0f);
        inventory.setStock(request.getStock() != null ? request.getStock() : 0);
        inventory.setReservedStock(0);

        ShopInventory savedInventory = shopInventoryRepository.save(inventory);
        return mapToResponse(savedProduct, savedInventory);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, UUID ownerId, boolean isAdmin) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        if (!isAdmin) {
            // Ownership validation: user must own at least one shop that has this product in inventory
            if (shopInventoryRepository.findByProductIdAndOwnerId(productId, ownerId).isEmpty()) {
                throw new ForbiddenException("You don't have permission to update this product");
            }
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getCategoryId() != null || request.getCategory() != null) {
            product.setCategory(resolveCategory(request));
        }

        Product updated = productRepository.save(product);
        return productMapper.toDto(updated);
    }

    @Transactional
    public void deleteProduct(Long productId, UUID ownerId, boolean isAdmin) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        if (!isAdmin) {
            if (shopInventoryRepository.findByProductIdAndOwnerId(productId, ownerId).isEmpty()) {
                throw new ForbiddenException("You don't have permission to delete this product");
            }
        }

        productRepository.delete(product);
    }

    @Transactional
    public void deleteProducts(List<Long> productIds, UUID ownerId, boolean isAdmin) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Product IDs list cannot be empty");
        }

        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new NotFoundException("One or more products not found");
        }

        if (!isAdmin) {
            // Verify user owns at least one inventory relationship for each product
            for (Product product : products) {
                if (shopInventoryRepository.findByProductIdAndOwnerId(product.getId(), ownerId).isEmpty()) {
                    throw new ForbiddenException(
                            "You don't have permission to delete product with id: " + product.getId());
                }
            }
        }

        productRepository.deleteAll(products);
    }

    private Category resolveCategory(ProductRequest request) {

        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
            try {
                Long categoryId = Long.parseLong(request.getCategoryId());
                return categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid categoryId format");
            }
        }

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            return categoryRepository.findByNameIgnoreCase(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found with name: " + request.getCategory()));
        }

        throw new IllegalArgumentException("Either categoryId or category name must be provided");
    }

    private ProductResponse mapToResponse(Product product, ShopInventory inventory) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setCategory(mapCategory(product.getCategory()));
        response.setImageUrl(product.getImageUrl());
        
//        if (inventory != null) {
//            response.setPrice(inventory.getPrice());
//            response.setStock(inventory.getStock());
//            if (inventory.getShop() != null) {
//                response.setShopId(inventory.getShop().getId());
//            }
//        }
        
        return response;
    }

    private CategoryResponse mapCategory(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(),
                category.getImageUrl(), category.getType());
    }

    private PagedResponse<ProductResponse> mapToPagedResponse(Page<Product> userPage) {
        return new PagedResponse<>(productMapper.toDtoList(userPage.getContent()), userPage.getNumber(),
                userPage.getSize(), userPage.getTotalElements(), userPage.getTotalPages(), userPage.isFirst(),
                userPage.isLast());
    }
}