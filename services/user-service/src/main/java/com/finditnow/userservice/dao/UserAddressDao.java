package com.finditnow.userservice.dao;

import com.finditnow.userservice.entity.UserAddress;
import com.finditnow.userservice.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserAddressDao {
    private final UserAddressRepository userAddressRepository;

    public UserAddress save(UserAddress userAddress) {
        return userAddressRepository.save(userAddress);
    }

    public Optional<UserAddress> findById(UUID id) {
        return userAddressRepository.findById(id);
    }

    public List<UserAddress> findByUserId(UUID userId) {
        return userAddressRepository.findByUserId(userId);
    }

    public Page<UserAddress> findByUserId(UUID userId, Pageable pageable) {
        return userAddressRepository.findByUserId(userId, pageable);
    }

    public Optional<UserAddress> findPrimaryAddressByUserId(UUID userId) {
        return userAddressRepository.findPrimaryAddressByUserId(userId);
    }

    public void delete(UserAddress userAddress) {
        userAddressRepository.delete(userAddress);
    }

    public boolean existsById(UUID id) {
        return userAddressRepository.existsById(id);
    }
}
