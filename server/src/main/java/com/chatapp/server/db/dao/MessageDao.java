package com.chatapp.server.db.dao;

import com.chatapp.server.db.ConnectionPool;
import com.chatapp.shared.model.ChatMessageDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {

    private final ConnectionPool pool = ConnectionPool.getInstance();

    /** Persists a direct message and returns it (with generated id/timestamp) as a DTO. */
    public ChatMessageDto saveDirectMessage(int senderId, String senderUsername, String senderAvatarColor,
                                             int receiverId, String content) {
        return save(senderId, senderUsername, senderAvatarColor, receiverId, null, content);
    }

    /** Persists a channel message and returns it (with generated id/timestamp) as a DTO. */
    public ChatMessageDto saveChannelMessage(int senderId, String senderUsername, String senderAvatarColor,
                                              int channelId, String content) {
        return save(senderId, senderUsername, senderAvatarColor, null, channelId, content);
    }

    private ChatMessageDto save(int senderId, String senderUsername, String senderAvatarColor,
                                 Integer receiverId, Integer channelId, String content) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, channel_id, content) VALUES (?, ?, ?, ?)";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderId);
            if (receiverId != null) {
                ps.setInt(2, receiverId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            if (channelId != null) {
                ps.setInt(3, channelId);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, content);
            ps.executeUpdate();

            long id;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                id = keys.getLong(1);
            }
            long timestamp = System.currentTimeMillis();
            return new ChatMessageDto(id, senderId, senderUsername, senderAvatarColor,
                    receiverId, channelId, content, timestamp);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        } finally {
            pool.release(conn);
        }
    }

    /** Direct-message history between two users, oldest first, capped at {@code limit}. */
    public List<ChatMessageDto> getDirectHistory(int userA, int userB, int limit) {
        String sql = """
                SELECT m.id, m.sender_id, u.username, u.avatar_color, m.receiver_id, m.channel_id, m.content, m.created_at
                FROM messages m
                JOIN users u ON u.id = m.sender_id
                WHERE m.channel_id IS NULL
                  AND ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?))
                ORDER BY m.created_at DESC, m.id DESC
                LIMIT ?
                """;
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);
            ps.setInt(5, limit);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAndReverse(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load direct message history", e);
        } finally {
            pool.release(conn);
        }
    }

    /** Channel message history, oldest first, capped at {@code limit}. */
    public List<ChatMessageDto> getChannelHistory(int channelId, int limit) {
        String sql = """
                SELECT m.id, m.sender_id, u.username, u.avatar_color, m.receiver_id, m.channel_id, m.content, m.created_at
                FROM messages m
                JOIN users u ON u.id = m.sender_id
                WHERE m.channel_id = ?
                ORDER BY m.created_at DESC, m.id DESC
                LIMIT ?
                """;
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, channelId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAndReverse(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load channel message history", e);
        } finally {
            pool.release(conn);
        }
    }

    private List<ChatMessageDto> mapAndReverse(ResultSet rs) throws SQLException {
        List<ChatMessageDto> messages = new ArrayList<>();
        while (rs.next()) {
            Timestamp createdAt = rs.getTimestamp("created_at");
            int receiverIdVal = rs.getInt("receiver_id");
            Integer receiverId = rs.wasNull() ? null : receiverIdVal;
            int channelIdVal = rs.getInt("channel_id");
            Integer channelId = rs.wasNull() ? null : channelIdVal;
            messages.add(new ChatMessageDto(
                    rs.getLong("id"),
                    rs.getInt("sender_id"),
                    rs.getString("username"),
                    rs.getString("avatar_color"),
                    receiverId,
                    channelId,
                    rs.getString("content"),
                    createdAt == null ? 0L : createdAt.getTime()));
        }
        // query orders newest-first for the LIMIT to work; reverse to chronological order
        java.util.Collections.reverse(messages);
        return messages;
    }
}
