package com.finditnow.userservice.service;

import com.finditnow.userservice.dao.UserDao;
import com.finditnow.userservice.dto.PagedResponse;
import com.finditnow.userservice.dto.UserDto;
import com.finditnow.userservice.entity.User;
import com.finditnow.userservice.exception.DuplicateResourceException;
import com.finditnow.userservice.exception.ResourceNotFoundException;
import com.finditnow.userservice.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(UserDto userDto) {
        // Validate unique constraints
        if (userDao.existsByEmail(userDto.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + userDto.getEmail());
        }
        if (userDto.getPhone() != null && userDao.existsByPhone(userDto.getPhone())) {
            throw new DuplicateResourceException("Phone number already exists: " + userDto.getPhone());
        }

        User user = userMapper.toEntity(userDto);

        if (userDto.getId() == null) {
            user.setId(UUID.randomUUID());
        }

        user.setCreatedAt(Instant.now());

        User savedUser = userDao.save(user);
        return userMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        User user = userDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getAllUsers(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> userPage = userDao.findAll(pageable);

        return mapToPagedResponse(userPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getUsersByRole(String role, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> userPage = userDao.findAllByRole(role, pageable);

        return mapToPagedResponse(userPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> searchUsersByNameAndRole(String name, String role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userDao.searchByNameAndRole(name, role, pageable);

        return mapToPagedResponse(userPage);
    }

    @Transactional
    public UserDto updateUser(UUID id, UserDto userDto) {
        User existingUser = userDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check email uniqueness if changed
        if (!existingUser.getEmail().equals(userDto.getEmail()) && userDao.existsByEmail(userDto.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + userDto.getEmail());
        }

        // Check phone uniqueness if changed
        if (userDto.getPhone() != null && !userDto.getPhone().equals(existingUser.getPhone()) && userDao.existsByPhone(userDto.getPhone())) {
            throw new DuplicateResourceException("Phone number already exists: " + userDto.getPhone());
        }

        userMapper.updateEntityFromDto(userDto, existingUser);
        User updatedUser = userDao.save(existingUser);

        return userMapper.toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userDao.delete(user);
    }

    private PagedResponse<UserDto> mapToPagedResponse(Page<User> userPage) {
        return new PagedResponse<>(userMapper.toDtoList(userPage.getContent()), userPage.getNumber(), userPage.getSize(), userPage.getTotalElements(), userPage.getTotalPages(), userPage.isFirst(), userPage.isLast());
    }
}
