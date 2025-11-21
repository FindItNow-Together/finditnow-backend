package com.finditnow.userservice.service;

import com.finditnow.userservice.entity.User;
import com.finditnow.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public User createUser(User user) {

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername(generateUsername(user.getFirstName(), user.getLastName()));
        }

        return repo.save(user);
    }

    private String generateUsername(String first, String last) {
        return first.toLowerCase() + "." + last.toLowerCase() + "." + UUID.randomUUID().toString().substring(0, 6);
    }
}