package com.chatapp.server.service;

import com.chatapp.server.db.dao.FriendDao;
import com.chatapp.server.db.dao.UserDao;
import com.chatapp.server.net.SessionManager;
import com.chatapp.shared.model.UserDto;
import com.chatapp.shared.protocol.MessageType;
import com.chatapp.shared.protocol.payload.FriendPayloads;
import com.chatapp.shared.util.JsonUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FriendService {

    private final FriendDao friendDao;
    private final UserDao userDao;
    private final SessionManager sessionManager;

    public FriendService(FriendDao friendDao, UserDao userDao, SessionManager sessionManager) {
        this.friendDao = friendDao;
        this.userDao = userDao;
        this.sessionManager = sessionManager;
    }

    public static class FriendException extends RuntimeException {
        public FriendException(String message) {
            super(message);
        }
    }

    public List<UserDto> getFriends(int userId) {
        return friendDao.getFriends(userId).stream().map(Mappers::toUserDto).collect(Collectors.toList());
    }

    public List<com.chatapp.shared.model.FriendRequestDto> getPendingRequests(int userId) {
        return friendDao.getPendingRequests(userId).stream()
                .map(req -> userDao.findById(req.senderId())
                        .map(sender -> Mappers.toFriendRequestDto(req, sender))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** Sends a friend request from {@code senderId} to the user named {@code targetUsername}. */
    public void sendRequest(int senderId, String targetUsername) {
        Optional<UserDao.UserRecord> targetOpt = userDao.findByUsername(targetUsername);
        if (targetOpt.isEmpty()) {
            throw new FriendException("No user found with that username");
        }
        UserDao.UserRecord target = targetOpt.get();
        if (target.id() == senderId) {
            throw new FriendException("You can't add yourself as a friend");
        }
        if (friendDao.areFriends(senderId, target.id())) {
            throw new FriendException("You are already friends with " + target.username());
        }
        if (friendDao.hasPendingRequest(senderId, target.id())) {
            throw new FriendException("Friend request already sent");
        }

        int requestId = friendDao.createFriendRequest(senderId, target.id());

        UserDao.UserRecord sender = userDao.findById(senderId).orElseThrow();
        var dto = new com.chatapp.shared.model.FriendRequestDto(
                requestId, sender.id(), sender.username(), sender.avatarColor(), System.currentTimeMillis());
        sessionManager.sendTo(target.id(), JsonUtil.envelope(MessageType.FRIEND_REQUEST_RECEIVED, dto));
    }

    /** Accepts a pending request; notifies both parties they are now friends. */
    public void acceptRequest(int currentUserId, int requestId) {
        FriendDao.FriendRequestRecord request = friendDao.findRequest(requestId)
                .orElseThrow(() -> new FriendException("Friend request not found"));
        if (request.receiverId() != currentUserId) {
            throw new FriendException("This request is not addressed to you");
        }
        if (!"PENDING".equals(request.status())) {
            throw new FriendException("This request has already been resolved");
        }

        friendDao.updateRequestStatus(requestId, "ACCEPTED");
        friendDao.addFriendship(request.senderId(), request.receiverId());

        UserDao.UserRecord senderRecord = userDao.findById(request.senderId()).orElseThrow();
        UserDao.UserRecord receiverRecord = userDao.findById(request.receiverId()).orElseThrow();

        sessionManager.sendTo(request.senderId(),
                JsonUtil.envelope(MessageType.FRIEND_ADDED, new FriendPayloads.FriendAdded(Mappers.toUserDto(receiverRecord))));
        sessionManager.sendTo(request.receiverId(),
                JsonUtil.envelope(MessageType.FRIEND_ADDED, new FriendPayloads.FriendAdded(Mappers.toUserDto(senderRecord))));
    }

    public void declineRequest(int currentUserId, int requestId) {
        FriendDao.FriendRequestRecord request = friendDao.findRequest(requestId)
                .orElseThrow(() -> new FriendException("Friend request not found"));
        if (request.receiverId() != currentUserId) {
            throw new FriendException("This request is not addressed to you");
        }
        friendDao.updateRequestStatus(requestId, "DECLINED");
    }

    public void removeFriend(int userId, int friendId) {
        if (!friendDao.areFriends(userId, friendId)) {
            throw new FriendException("You are not friends with this user");
        }
        friendDao.removeFriendship(userId, friendId);
        sessionManager.sendTo(userId, JsonUtil.envelope(MessageType.FRIEND_REMOVED, new FriendPayloads.FriendRemoved(friendId)));
        sessionManager.sendTo(friendId, JsonUtil.envelope(MessageType.FRIEND_REMOVED, new FriendPayloads.FriendRemoved(userId)));
    }
}
