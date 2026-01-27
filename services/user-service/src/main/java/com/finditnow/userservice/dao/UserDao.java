package com.finditnow.userservice.dao;

import com.finditnow.userservice.entity.User;
import com.finditnow.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserDao {
    private final UserRepository userRepository;

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> findAllByRole(String role, Pageable pageable) {
        return userRepository.findAllByRole(role, pageable);
    }

    public Page<User> searchByNameAndRole(String name, String role, Pageable pageable) {
        return userRepository.searchByNameAndRole(name, role, pageable);
    }

    public void delete(User user) {
        userRepository.delete(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    public boolean existsById(UUID id) {
        return userRepository.existsById(id);
    }
}
