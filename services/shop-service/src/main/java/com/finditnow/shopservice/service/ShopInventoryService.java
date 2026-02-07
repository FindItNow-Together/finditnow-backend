package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.*;
import com.finditnow.shopservice.entity.Product;
import com.finditnow.shopservice.entity.Shop;
import com.finditnow.shopservice.entity.ShopInventory;
import com.finditnow.shopservice.exception.NotFoundException;
import com.finditnow.shopservice.mapper.ShopInventoryMapper;
import com.finditnow.shopservice.repository.ProductRepository;
import com.finditnow.shopservice.repository.ShopInventoryRepository;
import com.finditnow.shopservice.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopInventoryService {
    private final ShopInventoryRepository shopInventoryRepository;
    private final ShopInventoryMapper inventoryMapper;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public List<InventoryResponse> getInventory(long shopId) {
        return inventoryMapper.toDtoList(shopInventoryRepository.findByShopId(shopId));
    }

    @Transactional
    public InventoryResponse addExistingProduct(Long shopId, InventoryRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));

        if (request.getProduct() == null || request.getProduct().getId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }

        Product product = productRepository.findById(request.getProduct().getId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProduct().getId()));

        return createInventoryItem(shop, product, request.getStock(), request.getPrice(), request.getReservedStock());
    }

    @Transactional
    public InventoryResponse addNewProduct(Long shopId, ProductRequest productRequest, int stock, float price) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));
        ProductResponse productResp = productService.addProduct(productRequest, shop.getOwnerId());

        Product product = productRepository.findById(productResp.getId())
                .orElseThrow(() -> new IllegalStateException("Product Just Created not found"));

        return createInventoryItem(shop, product, stock, price, 0);
    }

    private InventoryResponse createInventoryItem(Shop shop, Product product, int stock, float price, int reserved) {
        ShopInventory inventory = new ShopInventory();
        inventory.setShop(shop);
        inventory.setProduct(product);
        inventory.setStock(stock);
        inventory.setPrice(price);
        inventory.setReservedStock(reserved);

        ShopInventory saved = shopInventoryRepository.save(inventory);
        return inventoryMapper.toDto(saved);
    }

    public InventoryResponse findById(long id) {
        return inventoryMapper.toDto(shopInventoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No inventory by id->" + id)));
    }

    public List<InventoryResponse> findAllByShopId(long shopId) {
        return inventoryMapper.toDtoList(shopInventoryRepository.findByShopId(shopId));
    }

    public List<InventoryResponse> searchByProductName(String prodName) {
        return inventoryMapper.toDtoList(shopInventoryRepository.searchByProductName(prodName));
    }

    private Shop requireShop(Long shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));
    }

    private ShopInventory requireInventory(Long inventoryId) {
        return shopInventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory not found with id: " + inventoryId));
    }

    private void requireOwnerOrAdmin(Shop shop, UUID userId, boolean isAdmin) {
        if (isAdmin) return;
        if (shop.getOwnerId() == null || !shop.getOwnerId().equals(userId)) {
            throw new IllegalStateException("Not allowed to modify inventory for this shop");
        }
    }

    @Transactional
    public InventoryResponse addInventory(Long shopId, AddInventoryRequest request, UUID userId, boolean isAdmin) {
        Shop shop = requireShop(shopId);
        requireOwnerOrAdmin(shop, userId, isAdmin);

        Product product = requireProduct(request.getProductId());

        boolean exists = shopInventoryRepository.findByShopId(shopId).stream()
                .anyMatch(inv -> inv.getProduct() != null && inv.getProduct().getId().equals(product.getId()));
        if (exists) {
            throw new IllegalStateException("Product already exists in shop inventory");
        }

        return createInventoryItem(shop, product, request.getStock(), request.getPrice(), 0);
    }

    @Transactional
    public InventoryResponse updateInventory(Long inventoryId, UpdateInventoryRequest request, UUID userId, boolean isAdmin) {
        ShopInventory inv = requireInventory(inventoryId);
        requireOwnerOrAdmin(inv.getShop(), userId, isAdmin);

        if (request.getStock() != null) {
            if (request.getStock() < inv.getReservedStock()) {
                throw new IllegalStateException("Stock cannot be less than reserved stock");
            }
            inv.setStock(request.getStock());
        }
        if (request.getPrice() != null) {
            inv.setPrice(request.getPrice());
        }

        return inventoryMapper.toDto(shopInventoryRepository.save(inv));
    }

    @Transactional
    public void deleteInventory(Long inventoryId, UUID userId, boolean isAdmin) {
        ShopInventory inv = requireInventory(inventoryId);
        requireOwnerOrAdmin(inv.getShop(), userId, isAdmin);
        shopInventoryRepository.delete(inv);
    }

    @Transactional
    public InventoryResponse reserveStock(Long inventoryId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }

        ShopInventory inv = requireInventory(inventoryId);
        int available = inv.getStock() - inv.getReservedStock();
        if (quantity > available) {
            throw new IllegalStateException("Insufficient stock available");
        }

        inv.setReservedStock(inv.getReservedStock() + quantity);
        return inventoryMapper.toDto(shopInventoryRepository.save(inv));
    }

    @Transactional
    public InventoryResponse releaseStock(Long inventoryId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }

        ShopInventory inv = requireInventory(inventoryId);
        int newReserved = inv.getReservedStock() - quantity;
        if (newReserved < 0) {
            throw new IllegalStateException("reservedStock cannot become negative");
        }

        inv.setReservedStock(newReserved);
        return inventoryMapper.toDto(shopInventoryRepository.save(inv));
    }

    public List<InventoryResponse> searchByProductNameShopId(String prodName, Long shopId) {
        return inventoryMapper.toDtoList(shopInventoryRepository.searchByProductNameAndShopId(prodName, shopId));
    }

    public List<InventoryResponse> searchByShopId(Long shopId) {
        return inventoryMapper.toDtoList(shopInventoryRepository.findByShopId(shopId));
    }
}
