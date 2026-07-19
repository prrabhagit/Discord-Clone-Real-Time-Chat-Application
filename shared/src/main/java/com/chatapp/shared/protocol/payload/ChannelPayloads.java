package com.chatapp.shared.protocol.payload;

import com.chatapp.shared.model.ChannelDto;

import java.util.List;

public final class ChannelPayloads {

    private ChannelPayloads() {
    }

    public static class CreateChannelRequest {
        public int serverId;
        public String name;

        public CreateChannelRequest() {
        }

        public CreateChannelRequest(int serverId, String name) {
            this.serverId = serverId;
            this.name = name;
        }
    }

    public static class GetChannelsRequest {
        public int serverId;

        public GetChannelsRequest() {
        }

        public GetChannelsRequest(int serverId) {
            this.serverId = serverId;
        }
    }

    public static class ChannelsList {
        public int serverId;
        public List<ChannelDto> channels;

        public ChannelsList() {
        }

        public ChannelsList(int serverId, List<ChannelDto> channels) {
            this.serverId = serverId;
            this.channels = channels;
        }
    }

    public static class ChannelPayload {
        public ChannelDto channel;

        public ChannelPayload() {
        }

        public ChannelPayload(ChannelDto channel) {
            this.channel = channel;
        }
    }
}
