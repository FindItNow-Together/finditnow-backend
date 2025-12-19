package com.finditnow.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_addresses")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAddress {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    private String line1;
    private String line2;
    private String city;
    private String state;
    private String country;
    private String postalCode;

    // this is the concatenated human-readable string
    @Column(columnDefinition = "text")
    private String fullAddress;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
