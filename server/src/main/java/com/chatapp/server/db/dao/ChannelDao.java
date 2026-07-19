package com.chatapp.server.db.dao;

import com.chatapp.server.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChannelDao {

    private final ConnectionPool pool = ConnectionPool.getInstance();

    public record ChannelRecord(int id, int serverId, String name) {
    }

    public int createChannel(int serverId, String name) {
        String sql = "INSERT INTO channels (server_id, name) VALUES (?, ?)";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, serverId);
            ps.setString(2, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create channel", e);
        } finally {
            pool.release(conn);
        }
    }

    public List<ChannelRecord> getChannels(int serverId) {
        String sql = "SELECT id, server_id, name FROM channels WHERE server_id = ? ORDER BY id";
        Connection conn = pool.borrow();
        List<ChannelRecord> channels = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    channels.add(new ChannelRecord(rs.getInt("id"), rs.getInt("server_id"), rs.getString("name")));
                }
            }
            return channels;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load channels", e);
        } finally {
            pool.release(conn);
        }
    }

    public Optional<ChannelRecord> findById(int channelId) {
        String sql = "SELECT id, server_id, name FROM channels WHERE id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, channelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ChannelRecord(rs.getInt("id"), rs.getInt("server_id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up channel", e);
        } finally {
            pool.release(conn);
        }
    }
}
