package com.chatapp.server.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.chatapp.server.db.dao.ChannelDao;
import com.chatapp.server.db.dao.ServerDao;
import com.chatapp.shared.model.ChannelDto;
import com.chatapp.shared.model.ServerDto;
import com.chatapp.shared.model.UserDto;

/**
 * Handles servers ("guilds" in Discord terms) and their text channels.
 * Named GuildService to avoid clashing with java.rmi/network "Server" naming.
 */
public class GuildService {

    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final List<String> ICON_COLORS = List.of(
            "#5865F2", "#EB459E", "#57F287", "#FEE75C", "#ED4245", "#9B59B6", "#1ABC9C", "#E67E22"
    );

    private final ServerDao serverDao;
    private final ChannelDao channelDao;
    private final SecureRandom random = new SecureRandom();

    public GuildService(ServerDao serverDao, ChannelDao channelDao) {
        this.serverDao = serverDao;
        this.channelDao = channelDao;
    }

    public static class GuildException extends RuntimeException {
        public GuildException(String message) {
            super(message);
        }
    }

    public ServerDto createServer(int ownerId, String name) {
        String trimmed = validateName(name);
        String iconText = initials(trimmed);
        String iconColor = ICON_COLORS.get(random.nextInt(ICON_COLORS.size()));
        String inviteCode = generateInviteCode();

        int serverId = serverDao.createServer(trimmed, iconText, iconColor, ownerId, inviteCode);
        ServerDao.ServerRecord record = serverDao.findById(serverId).orElseThrow();
        return Mappers.toServerDto(record);
    }

    public ServerDto joinServer(int userId, String inviteCode) {
        Optional<ServerDao.ServerRecord> serverOpt = serverDao.findByInviteCode(inviteCode.strip().toUpperCase());
        if (serverOpt.isEmpty()) {
            throw new GuildException("Invalid invite code");
        }
        ServerDao.ServerRecord server = serverOpt.get();
        if (serverDao.isMember(server.id(), userId)) {
            throw new GuildException("You are already a member of this server");
        }
        serverDao.addMember(server.id(), userId);
        return Mappers.toServerDto(server);
    }

    public void leaveServer(int userId, int serverId) {
        ServerDao.ServerRecord server = serverDao.findById(serverId)
                .orElseThrow(() -> new GuildException("Server not found"));
        if (server.ownerId() == userId) {
            throw new GuildException("The server owner can't leave; delete or transfer ownership instead");
        }
        if (!serverDao.isMember(serverId, userId)) {
            throw new GuildException("You are not a member of this server");
        }
        serverDao.removeMember(serverId, userId);
    }

    public List<ServerDto> getServersForUser(int userId) {
        return serverDao.getServersForUser(userId).stream().map(Mappers::toServerDto).collect(Collectors.toList());
    }

    public List<UserDto> getMembers(int userId, int serverId) {
        if (!serverDao.isMember(serverId, userId)) {
            throw new GuildException("You are not a member of this server");
        }
        return serverDao.getMembers(serverId).stream().map(Mappers::toUserDto).collect(Collectors.toList());
    }

    public ChannelDto createChannel(int userId, int serverId, String name) {
        ServerDao.ServerRecord server = serverDao.findById(serverId)
                .orElseThrow(() -> new GuildException("Server not found"));
        if (server.ownerId() != userId) {
            throw new GuildException("Only the server owner can create channels");
        }
        String trimmed = validateChannelName(name);
        int channelId = channelDao.createChannel(serverId, trimmed);
        return Mappers.toChannelDto(channelDao.findById(channelId).orElseThrow());
    }

    public List<ChannelDto> getChannels(int userId, int serverId) {
        if (!serverDao.isMember(serverId, userId)) {
            throw new GuildException("You are not a member of this server");
        }
        return channelDao.getChannels(serverId).stream().map(Mappers::toChannelDto).collect(Collectors.toList());
    }

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(INVITE_CHARS.charAt(random.nextInt(INVITE_CHARS.length())));
        }
        return sb.toString();
    }

    private String initials(String name) {
        String[] words = name.strip().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return sb.isEmpty() ? "?" : sb.toString();
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new GuildException("Server name cannot be empty");
        }
        String trimmed = name.strip();
        if (trimmed.length() > 50) {
            throw new GuildException("Server name is too long (max 50 characters)");
        }
        return trimmed;
    }

    private String validateChannelName(String name) {
        if (name == null || name.isBlank()) {
            throw new GuildException("Channel name cannot be empty");
        }
        String trimmed = name.strip().toLowerCase().replace(' ', '-');
        if (trimmed.length() > 30) {
            throw new GuildException("Channel name is too long (max 30 characters)");
        }
        return trimmed;
    }
}
