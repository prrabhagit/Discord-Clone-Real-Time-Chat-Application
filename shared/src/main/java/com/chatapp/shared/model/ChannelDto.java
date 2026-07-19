package com.chatapp.shared.model;

import java.io.Serializable;

public class ChannelDto implements Serializable {

    private int id;
    private int serverId;
    private String name;

    public ChannelDto() {
    }

    public ChannelDto(int id, int serverId, String name) {
        this.id = id;
        this.serverId = serverId;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
