package com.chatapp.server.net;

import com.chatapp.server.db.dao.UserDao;
import com.chatapp.server.service.AuthService;
import com.chatapp.server.service.FriendService;
import com.chatapp.server.service.GuildService;
import com.chatapp.server.service.Mappers;
import com.chatapp.server.service.MessageService;
import com.chatapp.server.service.PresenceService;
import com.chatapp.shared.model.ChannelDto;
import com.chatapp.shared.model.PresenceStatus;
import com.chatapp.shared.model.ServerDto;
import com.chatapp.shared.protocol.Envelope;
import com.chatapp.shared.protocol.MessageType;
import com.chatapp.shared.protocol.payload.AuthPayloads;
import com.chatapp.shared.protocol.payload.ChannelPayloads;
import com.chatapp.shared.protocol.payload.CommonPayloads;
import com.chatapp.shared.protocol.payload.FriendPayloads;
import com.chatapp.shared.protocol.payload.MessagePayloads;
import com.chatapp.shared.protocol.payload.PresencePayloads;
import com.chatapp.shared.protocol.payload.ServerPayloads;
import com.chatapp.shared.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Owns one client's socket connection: reads newline-delimited JSON envelopes,
 * dispatches them to the appropriate service, and writes responses/pushes back.
 * Runs entirely on its own thread (one thread per connected client).
 */
public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final SessionManager sessionManager;
    private final AuthService authService;
    private final FriendService friendService;
    private final PresenceService presenceService;
    private final GuildService guildService;
    private final MessageService messageService;

    private PrintWriter out;
    private volatile UserDao.UserRecord currentUser;

    public ClientHandler(Socket socket, SessionManager sessionManager, AuthService authService,
                          FriendService friendService, PresenceService presenceService,
                          GuildService guildService, MessageService messageService) {
        this.socket = socket;
        this.sessionManager = sessionManager;
        this.authService = authService;
        this.friendService = friendService;
        this.presenceService = presenceService;
        this.guildService = guildService;
        this.messageService = messageService;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), false, StandardCharsets.UTF_8)) {
            this.out = writer;
            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            log.info("Connection closed for {}: {}", clientLabel(), e.getMessage());
        } finally {
            onDisconnect();
        }
    }

    private void handleLine(String line) {
        Envelope envelope;
        try {
            envelope = JsonUtil.fromLine(line);
        } catch (Exception e) {
            sendError("Malformed message");
            return;
        }
        if (envelope == null || envelope.getType() == null) {
            sendError("Malformed message");
            return;
        }

        try {
            dispatch(envelope);
        } catch (AuthService.AuthException | FriendService.FriendException
                 | GuildService.GuildException | MessageService.MessageException e) {
            sendError(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error handling {} from {}", envelope.getType(), clientLabel(), e);
            sendError("Internal server error");
        }
    }

    private void dispatch(Envelope envelope) {
        MessageType type = envelope.getType();

        // Messages allowed before authentication
        switch (type) {
            case REGISTER -> {
                handleRegister(envelope);
                return;
            }
            case LOGIN -> {
                handleLogin(envelope);
                return;
            }
            default -> {
                // fall through to authenticated handling below
            }
        }

        if (currentUser == null) {
            sendError("You must be logged in first");
            return;
        }

        switch (type) {
            case LOGOUT -> onDisconnect();
            case SET_PRESENCE -> handleSetPresence(envelope);
            case GET_FRIENDS -> handleGetFriends();
            case SEND_FRIEND_REQUEST -> handleSendFriendRequest(envelope);
            case GET_PENDING_REQUESTS -> handleGetPendingRequests();
            case ACCEPT_FRIEND_REQUEST -> handleAcceptFriendRequest(envelope);
            case DECLINE_FRIEND_REQUEST -> handleDeclineFriendRequest(envelope);
            case REMOVE_FRIEND -> handleRemoveFriend(envelope);
            case SEND_DIRECT_MESSAGE -> handleSendDirectMessage(envelope);
            case GET_DM_HISTORY -> handleGetDmHistory(envelope);
            case TYPING_START -> handleTyping(envelope, true);
            case TYPING_STOP -> handleTyping(envelope, false);
            case CREATE_SERVER -> handleCreateServer(envelope);
            case JOIN_SERVER -> handleJoinServer(envelope);
            case LEAVE_SERVER -> handleLeaveServer(envelope);
            case GET_SERVERS -> handleGetServers();
            case GET_SERVER_MEMBERS -> handleGetServerMembers(envelope);
            case CREATE_CHANNEL -> handleCreateChannel(envelope);
            case GET_CHANNELS -> handleGetChannels(envelope);
            case SEND_CHANNEL_MESSAGE -> handleSendChannelMessage(envelope);
            case GET_CHANNEL_HISTORY -> handleGetChannelHistory(envelope);
            default -> sendError("Unsupported message type: " + type);
        }
    }

    // ---- Auth ----

    private void handleRegister(Envelope envelope) {
        var req = JsonUtil.payload(envelope, AuthPayloads.RegisterRequest.class);
        try {
            UserDao.UserRecord user = authService.register(req.username, req.password);
            send(JsonUtil.envelope(MessageType.REGISTER_RESPONSE,
                    new AuthPayloads.AuthResponse(true, "Registration successful", Mappers.toUserDto(user))));
        } catch (AuthService.AuthException e) {
            send(JsonUtil.envelope(MessageType.REGISTER_RESPONSE, new AuthPayloads.AuthResponse(false, e.getMessage(), null)));
        }
    }

    private void handleLogin(Envelope envelope) {
        var req = JsonUtil.payload(envelope, AuthPayloads.LoginRequest.class);
        try {
            UserDao.UserRecord user = authService.login(req.username, req.password);
            currentUser = user;
            sessionManager.register(user.id(), this);
            presenceService.setPresence(user.id(), PresenceStatus.ONLINE);
            send(JsonUtil.envelope(MessageType.LOGIN_RESPONSE,
                    new AuthPayloads.AuthResponse(true, "Login successful", Mappers.toUserDto(user))));
            log.info("{} logged in", user.username());
        } catch (AuthService.AuthException e) {
            send(JsonUtil.envelope(MessageType.LOGIN_RESPONSE, new AuthPayloads.AuthResponse(false, e.getMessage(), null)));
        }
    }

    // ---- Presence ----

    private void handleSetPresence(Envelope envelope) {
        var req = JsonUtil.payload(envelope, PresencePayloads.SetPresenceRequest.class);
        presenceService.setPresence(currentUser.id(), req.status);
    }

    // ---- Friends ----

    private void handleGetFriends() {
        send(JsonUtil.envelope(MessageType.FRIENDS_LIST, new FriendPayloads.FriendsList(friendService.getFriends(currentUser.id()))));
    }

    private void handleSendFriendRequest(Envelope envelope) {
        var req = JsonUtil.payload(envelope, FriendPayloads.SendFriendRequest.class);
        friendService.sendRequest(currentUser.id(), req.targetUsername);
        send(JsonUtil.envelope(MessageType.ACK, new CommonPayloads.AckPayload("Friend request sent")));
    }

    private void handleGetPendingRequests() {
        send(JsonUtil.envelope(MessageType.PENDING_REQUESTS_LIST,
                new FriendPayloads.PendingRequestsList(friendService.getPendingRequests(currentUser.id()))));
    }

    private void handleAcceptFriendRequest(Envelope envelope) {
        var req = JsonUtil.payload(envelope, FriendPayloads.RespondFriendRequest.class);
        friendService.acceptRequest(currentUser.id(), req.requestId);
    }

    private void handleDeclineFriendRequest(Envelope envelope) {
        var req = JsonUtil.payload(envelope, FriendPayloads.RespondFriendRequest.class);
        friendService.declineRequest(currentUser.id(), req.requestId);
    }

    private void handleRemoveFriend(Envelope envelope) {
        var req = JsonUtil.payload(envelope, FriendPayloads.RemoveFriend.class);
        friendService.removeFriend(currentUser.id(), req.friendId);
    }

    // ---- Direct messages ----

    private void handleSendDirectMessage(Envelope envelope) {
        var req = JsonUtil.payload(envelope, MessagePayloads.SendDirectMessage.class);
        messageService.sendDirectMessage(currentUser.id(), req.receiverId, req.content);
    }

    private void handleGetDmHistory(Envelope envelope) {
        var req = JsonUtil.payload(envelope, MessagePayloads.GetDmHistory.class);
        var history = messageService.getDirectHistory(currentUser.id(), req.friendId);
        send(JsonUtil.envelope(MessageType.DM_HISTORY, new MessagePayloads.DmHistory(req.friendId, history)));
    }

    private void handleTyping(Envelope envelope, boolean typing) {
        var req = JsonUtil.payload(envelope, MessagePayloads.TypingEvent.class);
        req.typing = typing;
        messageService.relayTyping(currentUser.id(), currentUser.username(), req);
    }

    // ---- Servers ----

    private void handleCreateServer(Envelope envelope) {
        var req = JsonUtil.payload(envelope, ServerPayloads.CreateServerRequest.class);
        ServerDto server = guildService.createServer(currentUser.id(), req.name);
        send(JsonUtil.envelope(MessageType.SERVER_CREATED, new ServerPayloads.ServerPayload(server)));
    }

    private void handleJoinServer(Envelope envelope) {
        var req = JsonUtil.payload(envelope, ServerPayloads.JoinServerRequest.class);
        ServerDto server = guildService.joinServer(currentUser.id(), req.inviteCode);
        send(JsonUtil.envelope(MessageType.SERVER_JOINED, new ServerPayloads.ServerPayload(server)));
    }

    private void handleLeaveServer(Envelope envelope) {
        var req = JsonUtil.payload(envelope, ServerPayloads.LeaveServerRequest.class);
        guildService.leaveServer(currentUser.id(), req.serverId);
        send(JsonUtil.envelope(MessageType.SERVER_LEFT, new ServerPayloads.ServerLeft(req.serverId)));
    }

    private void handleGetServers() {
        List<ServerDto> servers = guildService.getServersForUser(currentUser.id());
        send(JsonUtil.envelope(MessageType.SERVERS_LIST, new ServerPayloads.ServersList(servers)));
    }

    private void handleGetServerMembers(Envelope envelope) {
        var req = JsonUtil.payload(envelope, ServerPayloads.GetServerMembersRequest.class);
        var members = guildService.getMembers(currentUser.id(), req.serverId);
        send(JsonUtil.envelope(MessageType.SERVER_MEMBERS_LIST, new ServerPayloads.ServerMembersList(req.serverId, members)));
    }

    // ---- Channels ----

    private void handleCreateChannel(Envelope envelope) {
        var req = JsonUtil.payload(envelope, ChannelPayloads.CreateChannelRequest.class);
        ChannelDto channel = guildService.createChannel(currentUser.id(), req.serverId, req.name);
        var pushEnvelope = JsonUtil.envelope(MessageType.CHANNEL_CREATED, new ChannelPayloads.ChannelPayload(channel));
        for (var member : guildService.getMembers(currentUser.id(), req.serverId)) {
            sessionManager.sendTo(member.getId(), pushEnvelope);
        }
    }

    private void handleGetChannels(Envelope envelope) {
        var req = JsonUtil.payload(envelope, ChannelPayloads.GetChannelsRequest.class);
        var channels = guildService.getChannels(currentUser.id(), req.serverId);
        send(JsonUtil.envelope(MessageType.CHANNELS_LIST, new ChannelPayloads.ChannelsList(req.serverId, channels)));
    }

    private void handleSendChannelMessage(Envelope envelope) {
        var req = JsonUtil.payload(envelope, MessagePayloads.SendChannelMessage.class);
        messageService.sendChannelMessage(currentUser.id(), req.channelId, req.content);
    }

    private void handleGetChannelHistory(Envelope envelope) {
        var req = JsonUtil.payload(envelope, MessagePayloads.GetChannelHistory.class);
        var history = messageService.getChannelHistory(currentUser.id(), req.channelId);
        send(JsonUtil.envelope(MessageType.CHANNEL_HISTORY, new MessagePayloads.ChannelHistory(req.channelId, history)));
    }

    // ---- Plumbing ----

    /** Thread-safe: may be called both from this handler's own thread and from others relaying messages. */
    public synchronized void send(Envelope envelope) {
        if (out == null) {
            return;
        }
        out.println(JsonUtil.toLine(envelope));
        out.flush();
    }

    private void sendError(String message) {
        send(JsonUtil.envelope(MessageType.ERROR, new CommonPayloads.ErrorPayload(message)));
    }

    private void onDisconnect() {
        if (currentUser != null) {
            sessionManager.unregister(currentUser.id());
            try {
                presenceService.setPresence(currentUser.id(), PresenceStatus.OFFLINE);
            } catch (Exception ignored) {
                // best effort - connection is going away regardless
            }
            log.info("{} disconnected", currentUser.username());
            currentUser = null;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private String clientLabel() {
        return currentUser != null ? currentUser.username() : socket.getRemoteSocketAddress().toString();
    }
}
