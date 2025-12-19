package com.finditnow.auth.dao;

import com.finditnow.auth.model.AuthSession;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AuthSessionDao {
    private final DataSource dataSource;

    public AuthSessionDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Read-only methods
    public Optional<AuthSession> findById(UUID id) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return findById(conn, id);
        }
    }

    public Optional<AuthSession> findBySessionToken(String token) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return findBySessionToken(conn, token);
        }
    }

    public List<AuthSession> findActiveSessionsByCred(UUID credId) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return findActiveSessionsByCred(conn, credId);
        }
    }

    // Transactional methods
    public Optional<AuthSession> findById(Connection conn, UUID id) throws SQLException {
        return queryOne(conn, "SELECT * FROM auth_sessions WHERE id = ?", id);
    }

    public Optional<AuthSession> findBySessionToken(Connection conn, String token) throws SQLException {
        return queryOne(conn, "SELECT * FROM auth_sessions WHERE session_token = ?", token);
    }

    public List<AuthSession> findActiveSessionsByCred(Connection conn, UUID credId) throws SQLException {
        String sql = "SELECT * FROM auth_sessions WHERE cred_id = ? AND is_valid = TRUE AND expires_at > NOW()";
        List<AuthSession> results = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, credId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }

        return results;
    }

    public void insert(Connection conn, AuthSession s) throws SQLException {
        String sql = """
                INSERT INTO auth_sessions
                (id, cred_id, session_token, session_method, ip_address, user_agent, 
                 expires_at, is_valid, created_at, revoked_at)
                VALUES (?, ?, ?, ?, ?::inet, ?, ?, ?, NOW(), ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, s.getId());
            ps.setObject(2, s.getCredId());
            ps.setString(3, s.getSessionToken());
            ps.setString(4, s.getSessionMethod());
            ps.setString(5, s.getIpAddress());
            ps.setString(6, s.getUserAgent());
            ps.setObject(7, s.getExpiresAt());
            ps.setBoolean(8, s.isValid());
            ps.setObject(9, s.getRevokedAt());

            ps.executeUpdate();
        }
    }

    public String invalidate(Connection conn, UUID id) throws SQLException {
        Optional<AuthSession> session = findById(conn, id);

        if (session.isEmpty()) {
            return null;
        }

        String sql = """
                UPDATE auth_sessions
                SET is_valid = FALSE, revoked_at = NOW()
                WHERE id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
            return session.get().getSessionToken();
        }
    }

    public void invalidateByToken(Connection conn, String token) throws SQLException {
        String sql = """
                UPDATE auth_sessions
                SET is_valid = FALSE, revoked_at = NOW()
                WHERE session_token = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    public String[] invalidateAllByCredId(Connection conn, UUID credId) throws SQLException {
        List<AuthSession> sessions = findActiveSessionsByCred(conn, credId);

        if (sessions.isEmpty()) {
            return new String[0];
        }

        String[] sessionTokens = sessions.stream()
                .map(AuthSession::getSessionToken)
                .toArray(String[]::new);

        String sql = "UPDATE auth_sessions SET is_valid = FALSE, revoked_at = NOW() WHERE cred_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, credId);
            ps.executeUpdate();
        }

        return sessionTokens;
    }

    public void delete(Connection conn, UUID id) throws SQLException {
        String sql = "DELETE FROM auth_sessions WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    // Helper methods
    private Optional<AuthSession> queryOne(Connection conn, String sql, Object param) throws SQLException {
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

    private AuthSession mapRow(ResultSet rs) throws SQLException {
        AuthSession s = new AuthSession();
        s.setId((UUID) rs.getObject("id"));
        s.setCredId((UUID) rs.getObject("cred_id"));
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
