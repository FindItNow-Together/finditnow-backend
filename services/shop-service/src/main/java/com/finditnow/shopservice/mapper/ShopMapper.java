package com.finditnow.shopservice.mapper;

import com.finditnow.shopservice.dto.ShopRequest;
import com.finditnow.shopservice.dto.ShopResponse;
import com.finditnow.shopservice.entity.Shop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ShopMapper {
    ShopResponse toDto(Shop shop);

    List<ShopResponse> toDtoList(List<Shop> shops);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shopInventory", ignore = true)
    @Mapping(target = "category", ignore = true)
    Shop toEntity(ShopRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true) // ownership never changes via update
    @Mapping(target = "shopInventory", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntityFromDto(ShopRequest request, @MappingTarget Shop entity);
}
