package com.finditnow.userservice.controllers;

import com.finditnow.userservice.entity.User;
import com.finditnow.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/users/new")
    public User create(@RequestBody User user) {
        return service.createUser(user);
    }

    @GetMapping("/users/me")
    public String getUser(HttpServletRequest request) {
        String userId = (String)request.getAttribute("userId");
        return "User ID: " + userId;
    }
}
