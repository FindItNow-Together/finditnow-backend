package com.finditnow.auth.dao;

import com.finditnow.auth.model.AuthCredential;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AuthCredentialDao {

    private final DataSource dataSource;

    public AuthCredentialDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<AuthCredential> findById(UUID id) {
        String sql = "SELECT * FROM auth_credentials WHERE id = ?";
        return queryOne(sql, id);
    }

    public Optional<AuthCredential> findByEmail(String email) {
        String sql = "SELECT * FROM auth_credentials WHERE email = ?";
        return queryOne(sql, email);
    }

    public Optional<AuthCredential> findByPhone(String phone) {
        String sql = "SELECT * FROM auth_credentials WHERE phone = ?";
        return queryOne(sql, phone);
    }

    public Optional<AuthCredential> findByIdentifier(String identifier) throws SQLException {
        String findQuery = "SELECT * FROM auth_credentials WHERE email = ? OR phone = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();) {
            PreparedStatement stmt = conn.prepareStatement(findQuery);
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);

            try (ResultSet dbCred = stmt.executeQuery();) {
                if (!dbCred.next())
                    return Optional.empty();

                return Optional.of(mapRow(dbCred));
            }

        }
    }

    public void insert(AuthCredential c) {
        String sql = """
                    INSERT INTO auth_credentials
                    (id, user_id, email, phone, password_hash, is_email_verified, is_phone_verified, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, c.getId());
            ps.setObject(2, c.getUserId());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getPhone());
            ps.setString(5, c.getPasswordHash());
            ps.setBoolean(6, c.isEmailVerified());
            ps.setBoolean(7, c.isPhoneVerified());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(AuthCredential c) {
        String sql = """
                    UPDATE auth_credentials
                    SET email = ?, phone = ?, password_hash = ?, 
                        is_email_verified = ?, is_phone_verified = ?
                    WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getEmail());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getPasswordHash());
            ps.setBoolean(4, c.isEmailVerified());
            ps.setBoolean(5, c.isPhoneVerified());
            ps.setObject(6, c.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCredFieldsById(UUID id, Map<String, Object> newFieldValues) {
        StringBuilder setPart = new StringBuilder();
        for (String column : newFieldValues.keySet()) {
            if (!setPart.isEmpty()) {
                setPart.append(", ");
            }
            setPart.append(column).append(" = ?");
        }

        String updateQuery = "UPDATE auth_credentials SET " + setPart + " WHERE id = ?";


        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateQuery)) {
            int fieldIndex = 1;

            for (Object value : newFieldValues.values()) {
                ps.setObject(fieldIndex++, value);
            }

            ps.setObject(fieldIndex, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private void delete(UUID id) {
        String sql = "DELETE FROM auth_credentials WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<AuthCredential> queryOne(String sql, Object param) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, param);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return Optional.empty();

            return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AuthCredential mapRow(ResultSet rs) throws SQLException {
        AuthCredential c = new AuthCredential();
        c.setId((UUID) rs.getObject("id"));
        c.setUserId((UUID) rs.getObject("user_id"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setPasswordHash(rs.getString("password_hash"));
        c.setEmailVerified(rs.getBoolean("is_email_verified"));
        c.setPhoneVerified(rs.getBoolean("is_phone_verified"));
        c.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return c;
    }
}
