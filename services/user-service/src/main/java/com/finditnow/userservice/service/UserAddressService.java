package com.finditnow.userservice.service;

import com.finditnow.userservice.dao.UserAddressDao;
import com.finditnow.userservice.dao.UserDao;
import com.finditnow.userservice.dto.PagedResponse;
import com.finditnow.userservice.dto.UserAddressDto;
import com.finditnow.userservice.entity.User;
import com.finditnow.userservice.entity.UserAddress;
import com.finditnow.userservice.exception.ResourceNotFoundException;
import com.finditnow.userservice.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAddressService {
    private final UserAddressDao userAddressDao;
    private final UserDao userDao;
    private final UserAddressMapper userAddressMapper;

    @Transactional
    public UserAddressDto createAddress(UUID userId, UserAddressDto addressDto) {
        User user = userDao.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserAddress address = userAddressMapper.toEntity(addressDto);

        address.setUser(user);

        // full address
        address.setFullAddress(buildFullAddress(address));

        // Handle primary address logic
        if (address.isPrimary()) {
            clearPrimaryAddressForUser(userId);
        }

        UserAddress savedAddress = userAddressDao.save(address);
        return userAddressMapper.toDto(savedAddress);
    }

    @Transactional(readOnly = true)
    public UserAddressDto getAddressById(UUID id) {
        UserAddress address = userAddressDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
        return userAddressMapper.toDto(address);
    }

    @Transactional(readOnly = true)
    public List<UserAddressDto> getAddressesByUserId(UUID userId) {
        if (!userDao.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        List<UserAddress> addresses = userAddressDao.findByUserId(userId);
        return userAddressMapper.toDtoList(addresses);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserAddressDto> getAddressesByUserId(UUID userId, int page, int size) {
        if (!userDao.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<UserAddress> addressPage = userAddressDao.findByUserId(userId, pageable);

        return new PagedResponse<>(userAddressMapper.toDtoList(addressPage.getContent()), addressPage.getNumber(), addressPage.getSize(), addressPage.getTotalElements(), addressPage.getTotalPages(), addressPage.isFirst(), addressPage.isLast());
    }

    @Transactional(readOnly = true)
    public UserAddressDto getPrimaryAddress(UUID userId) {
        if (!userDao.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        UserAddress address = userAddressDao.findPrimaryAddressByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("No primary address found for user: " + userId));
        return userAddressMapper.toDto(address);
    }

    @Transactional
    public UserAddressDto updateAddress(UUID id, UserAddressDto addressDto) {
        UserAddress existingAddress = userAddressDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));

        UUID userId = existingAddress.getUser().getId();

        // Handle primary address logic
        if (addressDto.isPrimary() && !existingAddress.isPrimary()) {
            clearPrimaryAddressForUser(userId);
        }

        userAddressMapper.updateEntityFromDto(addressDto, existingAddress);
        existingAddress.setFullAddress(buildFullAddress(existingAddress));

        UserAddress updatedAddress = userAddressDao.save(existingAddress);
        return userAddressMapper.toDto(updatedAddress);
    }

    @Transactional
    public void deleteAddress(UUID id) {
        UserAddress address = userAddressDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
        userAddressDao.delete(address);
    }

    private void clearPrimaryAddressForUser(UUID userId) {
        Optional<UserAddress> existingPrimary = userAddressDao.findPrimaryAddressByUserId(userId);
        existingPrimary.ifPresent(address -> {
            address.setPrimary(false);
            userAddressDao.save(address);
        });
    }

    private String buildFullAddress(UserAddress address) {
        StringBuilder sb = new StringBuilder();
        sb.append(address.getLine1());
        if (address.getLine2() != null && !address.getLine2().isEmpty()) {
            sb.append(", ").append(address.getLine2());
        }
        sb.append(", ").append(address.getCity());
        sb.append(", ").append(address.getState());
        sb.append(", ").append(address.getCountry());
        sb.append(" - ").append(address.getPostalCode());
        return sb.toString();
    }
}
