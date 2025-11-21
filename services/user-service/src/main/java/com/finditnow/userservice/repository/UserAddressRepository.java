package com.finditnow.userservice.repository;

import com.finditnow.userservice.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> { }