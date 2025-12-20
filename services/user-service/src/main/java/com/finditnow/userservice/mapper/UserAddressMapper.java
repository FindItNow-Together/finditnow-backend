package com.finditnow.userservice.mapper;

import com.finditnow.userservice.dto.UserAddressDto;
import com.finditnow.userservice.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserAddressMapper {
    @Mapping(source = "user.id", target = "userId")
    UserAddressDto toDto(UserAddress userAddress);

    @Mapping(target = "user", ignore = true)
    UserAddress toEntity(UserAddressDto userAddressDto);

    List<UserAddressDto> toDtoList(List<UserAddress> userAddresses);

    @Mapping(target = "user", ignore = true)
    void updateEntityFromDto(UserAddressDto dto, @MappingTarget UserAddress entity);
}
