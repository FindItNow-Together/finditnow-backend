package com.finditnow.auth.dao;

import com.finditnow.auth.model.AuthCredential;
import com.finditnow.auth.types.Role;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AuthCredentialDao {
    private final DataSource dataSource;

    public AuthCredentialDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Read-only methods - can use their own connections
    public Optional<AuthCredential> findById(UUID id) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return findById(conn, id);
        }
    }

    public Optional<AuthCredential> findByEmail(String email) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return findByEmail(conn, email);
        }
    }

    public Optional<AuthCredential> findByPhone(String phone) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return findByPhone(conn, phone);
        }
    }

    public Optional<AuthCredential> findByIdentifier(String identifier) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return findByIdentifier(conn, identifier);
        }
    }

    // Transactional methods - accept Connection parameter
    public Optional<AuthCredential> findById(Connection conn, UUID id) throws SQLException {
        String sql = "SELECT * FROM auth_credentials WHERE id = ?";
        return queryOne(conn, sql, id);
    }

    public Optional<AuthCredential> findByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT * FROM auth_credentials WHERE email = ?";
        return queryOne(conn, sql, email);
    }

    public Optional<AuthCredential> findByPhone(Connection conn, String phone) throws SQLException {
        String sql = "SELECT * FROM auth_credentials WHERE phone = ?";
        return queryOne(conn, sql, phone);
    }

    public Optional<AuthCredential> findByIdentifier(Connection conn, String identifier) throws SQLException {
        String sql = "SELECT * FROM auth_credentials WHERE email = ? OR phone = ? LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public Optional<AuthCredential> findByOauthSubject(Connection conn, String oauthSubject) throws SQLException {
        String sql = "SELECT ac.* FROM auth_credentials ac JOIN auth_oauth_google aog ON ac.user_id = aog.user_id WHERE aog.google_user_id=?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, oauthSubject);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    public void insert(Connection conn, AuthCredential c) throws SQLException {
        String sql = """
                INSERT INTO auth_credentials
                (id, user_id, email, first_name, phone, password_hash, role,
                 is_email_verified, is_phone_verified, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, c.getId());
            ps.setObject(2, c.getUserId());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getFirstName());
            ps.setString(5, c.getPhone());
            ps.setString(6, c.getPasswordHash());
            ps.setObject(7, c.getRole().toDb(), Types.OTHER);
            ps.setBoolean(8, c.isEmailVerified());
            ps.setBoolean(9, c.isPhoneVerified());

            ps.executeUpdate();
        }
    }

    public void update(Connection conn, AuthCredential c) throws SQLException {
        String sql = """
                UPDATE auth_credentials
                SET email = ?, phone = ?, password_hash = ?, 
                    is_email_verified = ?, is_phone_verified = ?
                WHERE id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getEmail());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getPasswordHash());
            ps.setBoolean(4, c.isEmailVerified());
            ps.setBoolean(5, c.isPhoneVerified());
            ps.setObject(6, c.getId());

            ps.executeUpdate();
        }
    }

    public void updateCredFieldsById(Connection conn, UUID id, Map<String, Object> newFieldValues) throws SQLException {
        StringBuilder setPart = new StringBuilder();
        for (String column : newFieldValues.keySet()) {
            if (!setPart.isEmpty()) {
                setPart.append(", ");
            }
            setPart.append(column).append(" = ?");
        }

        String updateQuery = "UPDATE auth_credentials SET " + setPart + " WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
            int fieldIndex = 1;

            for (Object value : newFieldValues.values()) {
                ps.setObject(fieldIndex++, value);
            }

            ps.setObject(fieldIndex, id);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, UUID id) throws SQLException {
        String sql = "DELETE FROM auth_credentials WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    // Helper methods
    private Optional<AuthCredential> queryOne(Connection conn, String sql, Object param) throws SQLException {
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

    private AuthCredential mapRow(ResultSet rs) throws SQLException {
        AuthCredential c = new AuthCredential();
        c.setId((UUID) rs.getObject("id"));
        c.setUserId((UUID) rs.getObject("user_id"));
        c.setEmail(rs.getString("email"));
        c.setFirstName(rs.getString("first_name"));
        c.setPhone(rs.getString("phone"));
        c.setPasswordHash(rs.getString("password_hash"));
        c.setRole(Role.fromDb(rs.getString("role")));
        c.setEmailVerified(rs.getBoolean("is_email_verified"));
        c.setPhoneVerified(rs.getBoolean("is_phone_verified"));
        c.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return c;
    }
}
