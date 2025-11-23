package com.finditnow.userservice.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
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

    // getters & setters omitted for brevity
    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
