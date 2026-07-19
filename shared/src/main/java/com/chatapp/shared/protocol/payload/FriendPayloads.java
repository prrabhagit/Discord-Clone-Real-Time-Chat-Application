package com.chatapp.shared.protocol.payload;

import com.chatapp.shared.model.FriendRequestDto;
import com.chatapp.shared.model.UserDto;

import java.util.List;

public final class FriendPayloads {

    private FriendPayloads() {
    }

    public static class FriendsList {
        public List<UserDto> friends;

        public FriendsList() {
        }

        public FriendsList(List<UserDto> friends) {
            this.friends = friends;
        }
    }

    public static class SendFriendRequest {
        public String targetUsername;

        public SendFriendRequest() {
        }

        public SendFriendRequest(String targetUsername) {
            this.targetUsername = targetUsername;
        }
    }

    public static class PendingRequestsList {
        public List<FriendRequestDto> requests;

        public PendingRequestsList() {
        }

        public PendingRequestsList(List<FriendRequestDto> requests) {
            this.requests = requests;
        }
    }

    public static class RespondFriendRequest {
        public int requestId;

        public RespondFriendRequest() {
        }

        public RespondFriendRequest(int requestId) {
            this.requestId = requestId;
        }
    }

    public static class FriendAdded {
        public UserDto friend;

        public FriendAdded() {
        }

        public FriendAdded(UserDto friend) {
            this.friend = friend;
        }
    }

    public static class RemoveFriend {
        public int friendId;

        public RemoveFriend() {
        }

        public RemoveFriend(int friendId) {
            this.friendId = friendId;
        }
    }

    public static class FriendRemoved {
        public int friendId;

        public FriendRemoved() {
        }

        public FriendRemoved(int friendId) {
            this.friendId = friendId;
        }
    }
}
