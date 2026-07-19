package com.chatapp.server.service;

import com.chatapp.server.db.dao.ChannelDao;
import com.chatapp.server.db.dao.FriendDao;
import com.chatapp.server.db.dao.ServerDao;
import com.chatapp.server.db.dao.UserDao;
import com.chatapp.shared.model.ChannelDto;
import com.chatapp.shared.model.FriendRequestDto;
import com.chatapp.shared.model.ServerDto;
import com.chatapp.shared.model.UserDto;

/** Small static helpers that turn DAO records (which may include secrets) into public DTOs. */
public final class Mappers {

    private Mappers() {
    }

    public static UserDto toUserDto(UserDao.UserRecord record) {
        return new UserDto(record.id(), record.username(), record.avatarColor(), record.status());
    }

    public static ServerDto toServerDto(ServerDao.ServerRecord record) {
        return new ServerDto(record.id(), record.name(), record.iconText(), record.iconColor(),
                record.ownerId(), record.inviteCode());
    }

    public static ChannelDto toChannelDto(ChannelDao.ChannelRecord record) {
        return new ChannelDto(record.id(), record.serverId(), record.name());
    }

    public static FriendRequestDto toFriendRequestDto(FriendDao.FriendRequestRecord record, UserDao.UserRecord sender) {
        return new FriendRequestDto(record.id(), sender.id(), sender.username(), sender.avatarColor(), record.createdAt());
    }
}
