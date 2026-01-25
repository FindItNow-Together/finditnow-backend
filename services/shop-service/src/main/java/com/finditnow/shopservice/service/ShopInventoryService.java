package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.InventoryRequest;
import com.finditnow.shopservice.dto.InventoryResponse;
import com.finditnow.shopservice.mapper.ShopInventoryMapper;
import com.finditnow.shopservice.repository.ShopInventoryRepository;
import org.springframework.stereotype.Service;

import com.finditnow.shopservice.dto.ProductRequest;
import com.finditnow.shopservice.dto.ProductResponse;
import com.finditnow.shopservice.entity.Product;
import com.finditnow.shopservice.entity.Shop;
import com.finditnow.shopservice.entity.ShopInventory;
import com.finditnow.shopservice.exception.NotFoundException;
import com.finditnow.shopservice.repository.ProductRepository;
import com.finditnow.shopservice.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

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
}
