package com.finditnow.auth.dao;

import com.finditnow.auth.model.AuthOauthGoogle;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class AuthOauthGoogleDao {
    private final DataSource dataSource;

    public AuthOauthGoogleDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public Optional<AuthOauthGoogle> findById(UUID id) {
        return queryOne("SELECT * FROM auth_oauth_google WHERE id = ?", id);
    }


    public Optional<AuthOauthGoogle> findByGoogleUserId(String googleUserId) {
        return queryOne("SELECT * FROM auth_oauth_google WHERE google_user_id = ?", googleUserId);
    }


    public Optional<AuthOauthGoogle> findByUserId(UUID userId) {
        return queryOne("SELECT * FROM auth_oauth_google WHERE user_id = ?", userId);
    }


    public void insert(AuthOauthGoogle o) {
        String sql = """
                    INSERT INTO auth_oauth_google
                    (id, user_id, google_user_id, email, access_token, refresh_token, created_at, last_login)
                    VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, o.getId());
            ps.setObject(2, o.getUserId());
            ps.setString(3, o.getGoogleUserId());
            ps.setString(4, o.getEmail());
            ps.setString(5, o.getAccessToken());
            ps.setString(6, o.getRefreshToken());
            ps.setObject(7, o.getLastLogin());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void updateTokens(UUID id, String accessToken, String refreshToken) {
        String sql = """
                    UPDATE auth_oauth_google
                    SET access_token = ?, refresh_token = ?
                    WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accessToken);
            ps.setString(2, refreshToken);
            ps.setObject(3, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void updateLastLogin(UUID id) {
        String sql = "UPDATE auth_oauth_google SET last_login = NOW() WHERE id = ?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void delete(UUID id) {
        String sql = "DELETE FROM auth_oauth_google WHERE id = ?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private Optional<AuthOauthGoogle> queryOne(String sql, Object param) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, param);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return Optional.empty();
            return Optional.of(mapRow(rs));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AuthOauthGoogle mapRow(ResultSet rs) throws SQLException {
        AuthOauthGoogle o = new AuthOauthGoogle();
        o.setId((UUID) rs.getObject("id"));
        o.setUserId((UUID) rs.getObject("user_id"));
        o.setGoogleUserId(rs.getString("google_user_id"));
        o.setEmail(rs.getString("email"));
        o.setAccessToken(rs.getString("access_token"));
        o.setRefreshToken(rs.getString("refresh_token"));
        o.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        o.setLastLogin(rs.getObject("last_login", java.time.OffsetDateTime.class));
        return o;
    }
}
