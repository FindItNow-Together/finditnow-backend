package com.finditnow.userservice.controllers;

import com.finditnow.userservice.dto.ApiResponse;
import com.finditnow.userservice.dto.PagedResponse;
import com.finditnow.userservice.dto.UserAddressDto;
import com.finditnow.userservice.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class UserAddressController {
    private final UserAddressService userAddressService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<UserAddressDto>> createAddress(
            @RequestAttribute UUID userId,
            @Valid @RequestBody UserAddressDto addressDto) {
        UserAddressDto createdAddress = userAddressService.createAddress(userId, addressDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<UserAddressDto>builder()
                        .success(true)
                        .message("Address created successfully")
                        .data(createdAddress)
                        .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserAddressDto>> getAddressById(@PathVariable UUID id) {
        UserAddressDto address = userAddressService.getAddressById(id);
        return ResponseEntity.ok(ApiResponse.<UserAddressDto>builder()
                .success(true)
                .data(address)
                .build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<UserAddressDto>>> getAddressesByUserId(
            @PathVariable UUID userId) {
        List<UserAddressDto> addresses = userAddressService.getAddressesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.<List<UserAddressDto>>builder()
                .success(true)
                .data(addresses)
                .build());
    }

    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<ApiResponse<PagedResponse<UserAddressDto>>> getAddressesByUserIdPaginated(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<UserAddressDto> addresses = userAddressService.getAddressesByUserId(userId, page, size);
        return ResponseEntity.ok(ApiResponse.<PagedResponse<UserAddressDto>>builder()
                .success(true)
                .data(addresses)
                .build());
    }

    @GetMapping("/user/{userId}/primary")
    public ResponseEntity<ApiResponse<UserAddressDto>> getPrimaryAddress(@PathVariable UUID userId) {
        UserAddressDto address = userAddressService.getPrimaryAddress(userId);
        return ResponseEntity.ok(ApiResponse.<UserAddressDto>builder()
                .success(true)
                .data(address)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserAddressDto>> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody UserAddressDto addressDto) {

        UserAddressDto updated = userAddressService.updateAddress(id, addressDto);

        return ResponseEntity.ok(ApiResponse.<UserAddressDto>builder()
                .success(true)
                .message("Address updated successfully")
                .data(updated)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable UUID id) {
        userAddressService.deleteAddress(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Address deleted successfully")
                .build());
    }
}
