package com.finditnow.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, name = "first_name")
    private String firstName;

    @Column(nullable = true, name = "last_name")
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(name = "profile_url")
    private String profileUrl; // nullable

    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserAddress> addresses;
}
