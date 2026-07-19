package com.chatapp.server.db.dao;

import com.chatapp.server.db.ConnectionPool;
import com.chatapp.shared.model.PresenceStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerDao {

    private final ConnectionPool pool = ConnectionPool.getInstance();

    public record ServerRecord(int id, String name, String iconText, String iconColor,
                                int ownerId, String inviteCode) {
    }

    public int createServer(String name, String iconText, String iconColor, int ownerId, String inviteCode) {
        String sql = "INSERT INTO servers (name, icon_text, icon_color, owner_id, invite_code) VALUES (?, ?, ?, ?, ?)";
        Connection conn = pool.borrow();
        try {
            conn.setAutoCommit(false);
            int serverId;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, iconText);
                ps.setString(3, iconColor);
                ps.setInt(4, ownerId);
                ps.setString(5, inviteCode);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    serverId = keys.getInt(1);
                }
            }
            addMemberInternal(conn, serverId, ownerId);
            insertDefaultChannel(conn, serverId);
            conn.commit();
            return serverId;
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            throw new RuntimeException("Failed to create server", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            pool.release(conn);
        }
    }

    private void insertDefaultChannel(Connection conn, int serverId) throws SQLException {
        String sql = "INSERT INTO channels (server_id, name) VALUES (?, 'general')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serverId);
            ps.executeUpdate();
        }
    }

    public Optional<ServerRecord> findByInviteCode(String inviteCode) {
        String sql = "SELECT id, name, icon_text, icon_color, owner_id, invite_code FROM servers WHERE invite_code = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inviteCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapServer(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up server by invite code", e);
        } finally {
            pool.release(conn);
        }
    }

    public Optional<ServerRecord> findById(int serverId) {
        String sql = "SELECT id, name, icon_text, icon_color, owner_id, invite_code FROM servers WHERE id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapServer(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up server by id", e);
        } finally {
            pool.release(conn);
        }
    }

    public boolean isMember(int serverId, int userId) {
        String sql = "SELECT 1 FROM server_members WHERE server_id = ? AND user_id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serverId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check server membership", e);
        } finally {
            pool.release(conn);
        }
    }

    public void addMember(int serverId, int userId) {
        Connection conn = pool.borrow();
        try {
            addMemberInternal(conn, serverId, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add server member", e);
        } finally {
            pool.release(conn);
        }
    }

    private void addMemberInternal(Connection conn, int serverId, int userId) throws SQLException {
        String sql = "INSERT INTO server_members (server_id, user_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serverId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void removeMember(int serverId, int userId) {
        String sql = "DELETE FROM server_members WHERE server_id = ? AND user_id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serverId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove server member", e);
        } finally {
            pool.release(conn);
        }
    }

    public List<ServerRecord> getServersForUser(int userId) {
        String sql = """
                SELECT s.id, s.name, s.icon_text, s.icon_color, s.owner_id, s.invite_code
                FROM server_members sm
                JOIN servers s ON s.id = sm.server_id
                WHERE sm.user_id = ?
                ORDER BY sm.joined_at
                """;
        Connection conn = pool.borrow();
        List<ServerRecord> servers = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    servers.add(mapServer(rs));
                }
            }
            return servers;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load servers for user", e);
        } finally {
            pool.release(conn);
        }
    }

    public List<UserDao.UserRecord> getMembers(int serverId) {
        String sql = """
                SELECT u.id, u.username, u.password_hash, u.avatar_color, p.status
                FROM server_members sm
                JOIN users u ON u.id = sm.user_id
                LEFT JOIN presence p ON p.user_id = u.id
                WHERE sm.server_id = ?
                ORDER BY u.username
                """;
        Connection conn = pool.borrow();
        List<UserDao.UserRecord> members = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String statusStr = rs.getString("status");
                    PresenceStatus status = statusStr == null ? PresenceStatus.OFFLINE : PresenceStatus.valueOf(statusStr);
                    members.add(new UserDao.UserRecord(
                            rs.getInt("id"), rs.getString("username"),
                            rs.getString("password_hash"), rs.getString("avatar_color"), status));
                }
            }
            return members;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load server members", e);
        } finally {
            pool.release(conn);
        }
    }

    private ServerRecord mapServer(ResultSet rs) throws SQLException {
        return new ServerRecord(
                rs.getInt("id"), rs.getString("name"), rs.getString("icon_text"),
                rs.getString("icon_color"), rs.getInt("owner_id"), rs.getString("invite_code"));
    }
}
