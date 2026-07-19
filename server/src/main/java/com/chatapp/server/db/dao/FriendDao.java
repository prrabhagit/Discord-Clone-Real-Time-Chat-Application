package com.chatapp.server.db.dao;

import com.chatapp.server.db.ConnectionPool;
import com.chatapp.shared.model.PresenceStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FriendDao {

    private final ConnectionPool pool = ConnectionPool.getInstance();

    public record FriendRequestRecord(int id, int senderId, int receiverId, String status, long createdAt) {
    }

    public boolean areFriends(int userId, int otherId) {
        String sql = "SELECT 1 FROM friends WHERE user_id = ? AND friend_id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, otherId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check friendship", e);
        } finally {
            pool.release(conn);
        }
    }

    public boolean hasPendingRequest(int senderId, int receiverId) {
        String sql = "SELECT 1 FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'PENDING'";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check pending request", e);
        } finally {
            pool.release(conn);
        }
    }

    public int createFriendRequest(int senderId, int receiverId) {
        String sql = "INSERT INTO friend_requests (sender_id, receiver_id, status) VALUES (?, ?, 'PENDING')";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create friend request", e);
        } finally {
            pool.release(conn);
        }
    }

    public Optional<FriendRequestRecord> findRequest(int requestId) {
        String sql = "SELECT id, sender_id, receiver_id, status, created_at FROM friend_requests WHERE id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRequest(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load friend request", e);
        } finally {
            pool.release(conn);
        }
    }

    public List<FriendRequestRecord> getPendingRequests(int receiverId) {
        String sql = "SELECT id, sender_id, receiver_id, status, created_at FROM friend_requests " +
                "WHERE receiver_id = ? AND status = 'PENDING' ORDER BY created_at DESC";
        Connection conn = pool.borrow();
        List<FriendRequestRecord> requests = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRequest(rs));
                }
            }
            return requests;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load pending requests", e);
        } finally {
            pool.release(conn);
        }
    }

    public void updateRequestStatus(int requestId, String status) {
        String sql = "UPDATE friend_requests SET status = ? WHERE id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update friend request status", e);
        } finally {
            pool.release(conn);
        }
    }

    /** Inserts the symmetric friendship rows (both directions) in one transaction. */
    public void addFriendship(int userIdA, int userIdB) {
        String sql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)";
        Connection conn = pool.borrow();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userIdA);
                ps.setInt(2, userIdB);
                ps.addBatch();
                ps.setInt(1, userIdB);
                ps.setInt(2, userIdA);
                ps.addBatch();
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            throw new RuntimeException("Failed to add friendship", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            pool.release(conn);
        }
    }

    public void removeFriendship(int userIdA, int userIdB) {
        String sql = "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userIdA);
            ps.setInt(2, userIdB);
            ps.setInt(3, userIdB);
            ps.setInt(4, userIdA);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove friendship", e);
        } finally {
            pool.release(conn);
        }
    }

    public List<UserDao.UserRecord> getFriends(int userId) {
        String sql = """
                SELECT u.id, u.username, u.password_hash, u.avatar_color, p.status
                FROM friends f
                JOIN users u ON u.id = f.friend_id
                LEFT JOIN presence p ON p.user_id = u.id
                WHERE f.user_id = ?
                ORDER BY u.username
                """;
        Connection conn = pool.borrow();
        List<UserDao.UserRecord> friends = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String statusStr = rs.getString("status");
                    PresenceStatus status = statusStr == null ? PresenceStatus.OFFLINE : PresenceStatus.valueOf(statusStr);
                    friends.add(new UserDao.UserRecord(
                            rs.getInt("id"), rs.getString("username"),
                            rs.getString("password_hash"), rs.getString("avatar_color"), status));
                }
            }
            return friends;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load friends list", e);
        } finally {
            pool.release(conn);
        }
    }

    private FriendRequestRecord mapRequest(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new FriendRequestRecord(
                rs.getInt("id"), rs.getInt("sender_id"), rs.getInt("receiver_id"),
                rs.getString("status"), createdAt == null ? 0L : createdAt.getTime());
    }
}
