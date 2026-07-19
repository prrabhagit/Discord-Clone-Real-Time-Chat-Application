package com.chatapp.shared.model;

import java.io.Serializable;

public class ServerDto implements Serializable {

    private int id;
    private String name;
    private String iconText;
    private String iconColor;
    private int ownerId;
    /** 6-character invite code used to join the server. */
    private String inviteCode;

    public ServerDto() {
    }

    public ServerDto(int id, String name, String iconText, String iconColor, int ownerId, String inviteCode) {
        this.id = id;
        this.name = name;
        this.iconText = iconText;
        this.iconColor = iconColor;
        this.ownerId = ownerId;
        this.inviteCode = inviteCode;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconText() {
        return iconText;
    }

    public void setIconText(String iconText) {
        this.iconText = iconText;
    }

    public String getIconColor() {
        return iconColor;
    }

    public void setIconColor(String iconColor) {
        this.iconColor = iconColor;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}
