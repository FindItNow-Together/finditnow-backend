package com.finditnow.userservice.controllers;

import com.finditnow.userservice.dto.ApiResponse;
import com.finditnow.userservice.dto.PagedResponse;
import com.finditnow.userservice.dto.UserDto;
import com.finditnow.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserDto userDto) {
        UserDto createdUser = userService.createUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UserDto>builder().success(true).message("User created successfully").data(createdUser).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.<UserDto>builder().success(true).data(user).build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(@RequestAttribute("userId") UUID userId) {
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.<UserDto>builder().success(true).data(user).build());
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getAllUsers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<UserDto> users = userService.getAllUsers(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.<PagedResponse<UserDto>>builder().success(true).data(users).build());
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getUsersByRole(@PathVariable String role, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<UserDto> users = userService.getUsersByRole(role, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.<PagedResponse<UserDto>>builder().success(true).data(users).build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> searchUsers(@RequestParam String name, @RequestParam String role, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        PagedResponse<UserDto> users = userService.searchUsersByNameAndRole(name, role, page, size);
        return ResponseEntity.ok(ApiResponse.<PagedResponse<UserDto>>builder().success(true).data(users).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable UUID id, @Valid @RequestBody UserDto userDto) {
        UserDto updatedUser = userService.updateUser(id, userDto);
        return ResponseEntity.ok(ApiResponse.<UserDto>builder().success(true).message("User updated successfully").data(updatedUser).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("User deleted successfully").build());
    }
}
