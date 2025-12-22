package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.InventoryResponse;
import com.finditnow.shopservice.mapper.ShopInventoryMapper;
import com.finditnow.shopservice.repository.ShopInventoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ShopInventoryService {
    private final ShopInventoryRepository shopInventoryRepository;
    private final ShopInventoryMapper inventoryMapper;

    public ShopInventoryService(ShopInventoryRepository shopInventoryRepository, ShopInventoryMapper inventoryMapper) {
        this.shopInventoryRepository = shopInventoryRepository;
        this.inventoryMapper = inventoryMapper;
    }

    public InventoryResponse findById(long id) {
        return inventoryMapper.toDto(shopInventoryRepository.findById(id).orElseThrow(() -> new NoSuchElementException("No inventory by id->" + id)));
    }

    public List<InventoryResponse> findAllByShopId(long shopId) {
        return inventoryMapper.toDtoList(shopInventoryRepository.findByShopId(shopId));
    }

    public List<InventoryResponse> searchByProductName(String prodName) {
        return inventoryMapper.toDtoList(shopInventoryRepository.searchByProductName(prodName));
    }
}
