package com.finditnow.auth.dao;

import com.finditnow.auth.model.AuthOauthGoogle;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class AuthOauthGoogleDao {
    private final DataSource dataSource;

    public AuthOauthGoogleDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Read-only methods
    public Optional<AuthOauthGoogle> findById(UUID id) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return findById(conn, id);
        }
    }

    public Optional<AuthOauthGoogle> findByGoogleUserId(String googleUserId) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return findByGoogleUserId(conn, googleUserId);
        }
    }

    public Optional<AuthOauthGoogle> findByUserId(UUID userId) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return findByUserId(conn, userId);
        }
    }

    // Transactional methods
    public Optional<AuthOauthGoogle> findById(Connection conn, UUID id) throws SQLException {
        return queryOne(conn, "SELECT * FROM auth_oauth_google WHERE id = ?", id);
    }

    public Optional<AuthOauthGoogle> findByGoogleUserId(Connection conn, String googleUserId) throws SQLException {
        return queryOne(conn, "SELECT * FROM auth_oauth_google WHERE google_user_id = ?", googleUserId);
    }

    public Optional<AuthOauthGoogle> findByUserId(Connection conn, UUID userId) throws SQLException {
        return queryOne(conn, "SELECT * FROM auth_oauth_google WHERE user_id = ?", userId);
    }

    public void insert(Connection conn, AuthOauthGoogle o) throws SQLException {
        String sql = """
                INSERT INTO auth_oauth_google
                (id, user_id, google_user_id, email, created_at)
                VALUES (?, ?, ?, ?, NOW())
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, o.getId());
            ps.setObject(2, o.getUserId());
            ps.setString(3, o.getGoogleUserId());
            ps.setString(4, o.getEmail());

            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, UUID id) throws SQLException {
        String sql = "DELETE FROM auth_oauth_google WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    // Helper methods
    private Optional<AuthOauthGoogle> queryOne(Connection conn, String sql, Object param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    private AuthOauthGoogle mapRow(ResultSet rs) throws SQLException {
        AuthOauthGoogle o = new AuthOauthGoogle();
        o.setId((UUID) rs.getObject("id"));
        o.setUserId((UUID) rs.getObject("user_id"));
        o.setGoogleUserId(rs.getString("google_user_id"));
        o.setEmail(rs.getString("email"));
        o.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return o;
    }
}
