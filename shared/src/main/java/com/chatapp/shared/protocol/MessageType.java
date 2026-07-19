package com.chatapp.shared.protocol;

/**
 * Every kind of message that can travel between client and server over the
 * socket connection. Client -&gt; server requests and server -&gt; client
 * responses/pushes share the same enum for simplicity; the direction is
 * implied by context.
 */
public enum MessageType {

    // ---- Connection / auth ----
    REGISTER,
    REGISTER_RESPONSE,
    LOGIN,
    LOGIN_RESPONSE,
    LOGOUT,

    // ---- Generic ----
    ERROR,
    ACK,

    // ---- Presence ----
    SET_PRESENCE,
    PRESENCE_UPDATE,

    // ---- Friends ----
    GET_FRIENDS,
    FRIENDS_LIST,
    SEND_FRIEND_REQUEST,
    FRIEND_REQUEST_RECEIVED,
    GET_PENDING_REQUESTS,
    PENDING_REQUESTS_LIST,
    ACCEPT_FRIEND_REQUEST,
    DECLINE_FRIEND_REQUEST,
    FRIEND_ADDED,
    REMOVE_FRIEND,
    FRIEND_REMOVED,

    // ---- Direct messages ----
    SEND_DIRECT_MESSAGE,
    DIRECT_MESSAGE,
    GET_DM_HISTORY,
    DM_HISTORY,
    TYPING_START,
    TYPING_STOP,
    TYPING_INDICATOR,

    // ---- Servers (guilds) ----
    CREATE_SERVER,
    SERVER_CREATED,
    GET_SERVERS,
    SERVERS_LIST,
    JOIN_SERVER,
    SERVER_JOINED,
    LEAVE_SERVER,
    SERVER_LEFT,
    GET_SERVER_MEMBERS,
    SERVER_MEMBERS_LIST,

    // ---- Channels ----
    CREATE_CHANNEL,
    CHANNEL_CREATED,
    GET_CHANNELS,
    CHANNELS_LIST,

    // ---- Channel messages ----
    SEND_CHANNEL_MESSAGE,
    CHANNEL_MESSAGE,
    GET_CHANNEL_HISTORY,
    CHANNEL_HISTORY
}
