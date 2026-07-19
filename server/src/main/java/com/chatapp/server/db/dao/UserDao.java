package com.chatapp.server.db.dao;

import com.chatapp.server.db.ConnectionPool;
import com.chatapp.shared.model.PresenceStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UserDao {

    private final ConnectionPool pool = ConnectionPool.getInstance();

    /** Plain data holder for a full user row, including the password hash (server-side only). */
    public record UserRecord(int id, String username, String passwordHash,
                              String avatarColor, PresenceStatus status) {
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check username existence", e);
        } finally {
            pool.release(conn);
        }
    }

    public int createUser(String username, String passwordHash, String avatarColor) {
        String sql = "INSERT INTO users (username, password_hash, avatar_color) VALUES (?, ?, ?)";
        String presenceSql = "INSERT INTO presence (user_id, status) VALUES (?, 'OFFLINE')";
        Connection conn = pool.borrow();
        try {
            conn.setAutoCommit(false);
            int userId;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, passwordHash);
                ps.setString(3, avatarColor);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    userId = keys.getInt(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(presenceSql)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            conn.commit();
            return userId;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Failed to create user", e);
        } finally {
            restoreAutoCommit(conn);
            pool.release(conn);
        }
    }

    public Optional<UserRecord> findByUsername(String username) {
        String sql = """
                SELECT u.id, u.username, u.password_hash, u.avatar_color, p.status
                FROM users u
                LEFT JOIN presence p ON p.user_id = u.id
                WHERE u.username = ?
                """;
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up user by username", e);
        } finally {
            pool.release(conn);
        }
    }

    public Optional<UserRecord> findById(int id) {
        String sql = """
                SELECT u.id, u.username, u.password_hash, u.avatar_color, p.status
                FROM users u
                LEFT JOIN presence p ON p.user_id = u.id
                WHERE u.id = ?
                """;
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up user by id", e);
        } finally {
            pool.release(conn);
        }
    }

    public void updatePresence(int userId, PresenceStatus status) {
        String sql = "UPDATE presence SET status = ?, last_seen = CURRENT_TIMESTAMP WHERE user_id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update presence", e);
        } finally {
            pool.release(conn);
        }
    }

    private UserRecord mapRow(ResultSet rs) throws SQLException {
        String statusStr = rs.getString("status");
        PresenceStatus status = statusStr == null ? PresenceStatus.OFFLINE : PresenceStatus.valueOf(statusStr);
        return new UserRecord(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("avatar_color"),
                status);
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // best effort
        }
    }

    private void restoreAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignored) {
            // best effort
        }
    }
}
