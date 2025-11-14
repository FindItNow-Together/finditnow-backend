package com.finditnow.auth.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import com.finditnow.auth.model.User;
import com.finditnow.database.Database;

public class UserDao {
    private final DataSource dataSource;

    public UserDao(Database db) {
        this.dataSource = db.get();
    }

    public void save(User user) throws SQLException {
        String saveQuery = "INSERT INTO users (id, username, email, phone_no, password) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();) {
            PreparedStatement stmt = conn.prepareStatement(saveQuery);
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUserName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPhoneNo());
            stmt.setString(5, user.getPasswordHash());

            stmt.executeUpdate();
        }
    }

    public void updateUserById(String userId, Map<String, Object> newFieldValues) throws SQLException{

        StringBuilder setPart = new StringBuilder();
            for (String column : newFieldValues.keySet()) {
                if (setPart.length() > 0) {
                    setPart.append(", ");
                }
                setPart.append(column).append(" = ?");
            }

            String updateQuery = "UPDATE users SET " + setPart.toString();

        try (Connection conn = dataSource.getConnection();) {
            PreparedStatement stmt = conn.prepareStatement(updateQuery);

            int fieldIndex = 1;
            for(Object value: newFieldValues.values()){
                stmt.setObject(fieldIndex++, value);
            }

            stmt.executeUpdate();
        }
    }

    public User findByEmail(String email) throws SQLException {
        String findQuery = "SELECT id, username, email, phone_no, password FROM users WHERE email = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();) {
            PreparedStatement stmt = conn.prepareStatement(findQuery);
            stmt.setString(1, email);

            try (ResultSet dbUser = stmt.executeQuery();) {
                if (!dbUser.next())
                    return null;

                User user = new User(dbUser.getString("id"), dbUser.getString("username"), dbUser.getString("email"),
                        dbUser.getString("phone_no"), dbUser.getString("password"));

                return user;
            }

        }
    }

    public User findByIdentifier(String identifier) throws SQLException {
        String findQuery = "SELECT id, username, email, phone_no, password FROM users WHERE username = ? OR email = ? OR phone_no = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();) {
            PreparedStatement stmt = conn.prepareStatement(findQuery);
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);
            stmt.setString(3, identifier);

            try (ResultSet dbUser = stmt.executeQuery();) {
                if (!dbUser.next())
                    return null;

                User user = new User(dbUser.getString("id"), dbUser.getString("username"), dbUser.getString("email"),
                        dbUser.getString("phone_no"), dbUser.getString("password"));

                return user;
            }

        }
    }

    public void insertSession(String id, String userId, String token, String authMethod, long ttlMillis,
            boolean isValid, String profile) {
        String insertSessionQuery = "INSERT INTO user_sessions (id, user_id, session_id, auth_method, session_validity, isValid, auth_profile, created_at) VALUES (?,?,?,?,DATE_ADD(NOW(), INTERVAL ? SECOND),?,?,NOW())";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(insertSessionQuery)) {
            stmt.setString(1, id);
            stmt.setString(2, userId);
            stmt.setString(3, token);
            stmt.setString(4, authMethod);
            stmt.setLong(5, ttlMillis / 1000L);
            stmt.setBoolean(6, isValid);
            stmt.setString(7, profile);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // error logging to console, redis token will be used for verification
            e.printStackTrace();
        }
    }

    public String logoutSession(String userId, String userProfile) throws SQLException {
        String sessionSelectQuery = "SELECT session_id FROM user_sessions WHERE user_id = ? AND auth_profile = ? AND isValid = TRUE ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = dataSource.getConnection();) {
            String deletedSessionToken;

            try (PreparedStatement sessionSelect = conn.prepareStatement(sessionSelectQuery)) {
                sessionSelect.setString(1, userId);
                sessionSelect.setString(2, userProfile);

                try (ResultSet user = sessionSelect.executeQuery()) {
                    if (!user.next())
                        return null;

                    deletedSessionToken = user.getString("session_id");
                }
            }

            String sessionUpdateQuery = "UPDATE user_sessions SET isValid = FALSE WHERE session_id = ?";
            try (PreparedStatement updateStatement = conn.prepareStatement(sessionUpdateQuery);) {
                updateStatement.setString(1, deletedSessionToken);
                updateStatement.executeUpdate();

                return deletedSessionToken;
            }
        }
    }
}
