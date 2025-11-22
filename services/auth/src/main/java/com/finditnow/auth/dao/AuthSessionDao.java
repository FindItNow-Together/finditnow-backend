package com.finditnow.auth.dao;


import com.finditnow.auth.model.AuthSession;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class AuthSessionDao {

    private final DataSource dataSource;

    public AuthSessionDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public Optional<AuthSession> findById(UUID id) {
        return queryOne("SELECT * FROM auth_sessions WHERE id = ?", id);
    }


    public Optional<AuthSession> findBySessionToken(String token) {
        return queryOne("SELECT * FROM auth_sessions WHERE session_token = ?", token);
    }


    public List<AuthSession> findActiveSessionsByUser(UUID userId) {
        String sql = "SELECT * FROM auth_sessions WHERE user_id = ? AND is_valid = TRUE AND expires_at > NOW()";
        List<AuthSession> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                results.add(mapRow(rs));
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void insert(AuthSession s) {
        String sql = """
                    INSERT INTO auth_sessions
                    (id, user_id, session_token, session_method, ip_address, user_agent, expires_at,
                     is_valid, created_at, revoked_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, s.getId());
            ps.setObject(2, s.getUserId());
            ps.setString(3, s.getSessionToken());
            ps.setString(4, s.getSessionMethod());
            ps.setString(5, s.getIpAddress());   // stored as text -> PostgreSQL converts
            ps.setString(6, s.getUserAgent());
            ps.setObject(7, s.getExpiresAt());
            ps.setBoolean(8, s.isValid());
            ps.setObject(9, s.getRevokedAt());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void invalidate(UUID id) {
        String sql = """
                    UPDATE auth_sessions
                    SET is_valid = FALSE, revoked_at = NOW()
                    WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void invalidateByToken(String token) {
        String sql = """
                    UPDATE auth_sessions
                    SET is_valid = FALSE, revoked_at = NOW()
                    WHERE session_token = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void delete(UUID id) {
        String sql = "DELETE FROM auth_sessions WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private Optional<AuthSession> queryOne(String sql, Object param) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, param);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return Optional.empty();
            return Optional.of(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AuthSession mapRow(ResultSet rs) throws SQLException {
        AuthSession s = new AuthSession();
        s.setId((UUID) rs.getObject("id"));
        s.setUserId((UUID) rs.getObject("user_id"));
        s.setSessionToken(rs.getString("session_token"));
        s.setSessionMethod(rs.getString("session_method"));
        s.setIpAddress(rs.getString("ip_address"));
        s.setUserAgent(rs.getString("user_agent"));
        s.setExpiresAt(rs.getObject("expires_at", java.time.OffsetDateTime.class));
        s.setValid(rs.getBoolean("is_valid"));
        s.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        s.setRevokedAt(rs.getObject("revoked_at", java.time.OffsetDateTime.class));
        return s;
    }
}
