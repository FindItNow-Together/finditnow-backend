package com.finditnow.shopservice.mapper;

import com.finditnow.shopservice.dto.InventoryRequest;
import com.finditnow.shopservice.dto.InventoryResponse;
import com.finditnow.shopservice.entity.ShopInventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ShopInventoryMapper {

    @Mapping(source = "id", target = "id")
    InventoryResponse toDto(ShopInventory inventory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "product", ignore = true)
    ShopInventory toEntity(InventoryRequest request);

    List<InventoryResponse> toDtoList(List<ShopInventory> inventories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "product", ignore = true)
    void updateEntityFromDto(
            InventoryRequest dto,
            @MappingTarget ShopInventory entity
    );
}

