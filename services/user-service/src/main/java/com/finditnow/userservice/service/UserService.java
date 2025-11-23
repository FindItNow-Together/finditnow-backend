package com.finditnow.userservice.service;

import com.finditnow.userservice.entity.User;
import com.finditnow.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public User createUser(User user) {
        return repo.save(user);
    }
}