package com.finditnow.userservice.repository;

import com.finditnow.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    List<User> findAllByPhone(String phone);

    List<User> findAllByEmail(String email);

    Page<User> findAllByRole(String role, Pageable pageable);

    List<User> findAllByRole(String role);

    @Query("""
    SELECT DISTINCT u
    FROM User u
    WHERE (
        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :nameQuery, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :nameQuery, '%'))
    )
    AND u.role = :role
""")
    Page<User> searchByNameAndRole(
            @Param("nameQuery") String nameQuery,
            @Param("role") String role,
            Pageable pageable
    );
}