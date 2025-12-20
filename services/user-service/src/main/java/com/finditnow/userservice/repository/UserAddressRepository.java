package com.finditnow.userservice.repository;

import com.finditnow.userservice.entity.UserAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findByUserId(UUID userId);

    Page<UserAddress> findByUserId(UUID userId, Pageable pageable);

    @Query(value = "SELECT DISTINCT(addr) FROM UserAddress addr WHERE addr.isPrimary=true AND addr.user.id=:userId")
    Optional<UserAddress> findPrimaryAddressByUserId(@Param("userId") UUID userId);
}