package com.finditnow.shopservice.mapper;

import com.finditnow.shopservice.dto.CategoryRequest;
import com.finditnow.shopservice.dto.CategoryResponse;
import com.finditnow.shopservice.entity.Category;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "name", expression = "java(normalize(request.getName()))")
    @Mapping(target = "products", ignore = true)   // 🔕 warning fix
    @Mapping(target = "shops", ignore = true)
        // 🔕 warning fix
    Category toEntity(CategoryRequest request);

    CategoryResponse toResponse(Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "shops", ignore = true)
    void updateEntity(
            CategoryRequest request,
            @MappingTarget Category category
    );

    default String normalize(String name) {
        return name == null ? null : name.trim();
    }
}

