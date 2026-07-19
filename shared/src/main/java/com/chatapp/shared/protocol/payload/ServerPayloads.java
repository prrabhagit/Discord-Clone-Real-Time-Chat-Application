package com.chatapp.shared.protocol.payload;

import com.chatapp.shared.model.ServerDto;
import com.chatapp.shared.model.UserDto;

import java.util.List;

public final class ServerPayloads {

    private ServerPayloads() {
    }

    public static class CreateServerRequest {
        public String name;

        public CreateServerRequest() {
        }

        public CreateServerRequest(String name) {
            this.name = name;
        }
    }

    public static class JoinServerRequest {
        public String inviteCode;

        public JoinServerRequest() {
        }

        public JoinServerRequest(String inviteCode) {
            this.inviteCode = inviteCode;
        }
    }

    public static class LeaveServerRequest {
        public int serverId;

        public LeaveServerRequest() {
        }

        public LeaveServerRequest(int serverId) {
            this.serverId = serverId;
        }
    }

    public static class ServerPayload {
        public ServerDto server;

        public ServerPayload() {
        }

        public ServerPayload(ServerDto server) {
            this.server = server;
        }
    }

    public static class ServersList {
        public List<ServerDto> servers;

        public ServersList() {
        }

        public ServersList(List<ServerDto> servers) {
            this.servers = servers;
        }
    }

    public static class ServerLeft {
        public int serverId;

        public ServerLeft() {
        }

        public ServerLeft(int serverId) {
            this.serverId = serverId;
        }
    }

    public static class GetServerMembersRequest {
        public int serverId;

        public GetServerMembersRequest() {
        }

        public GetServerMembersRequest(int serverId) {
            this.serverId = serverId;
        }
    }

    public static class ServerMembersList {
        public int serverId;
        public List<UserDto> members;

        public ServerMembersList() {
        }

        public ServerMembersList(int serverId, List<UserDto> members) {
            this.serverId = serverId;
            this.members = members;
        }
    }
}
