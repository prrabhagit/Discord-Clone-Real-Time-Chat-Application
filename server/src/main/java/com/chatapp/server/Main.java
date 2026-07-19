package com.chatapp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chatapp.server.config.ServerConfig;
import com.chatapp.server.db.dao.ChannelDao;
import com.chatapp.server.db.dao.FriendDao;
import com.chatapp.server.db.dao.MessageDao;
import com.chatapp.server.db.dao.ServerDao;
import com.chatapp.server.db.dao.UserDao;
import com.chatapp.server.net.ChatServer;
import com.chatapp.server.net.SessionManager;
import com.chatapp.server.service.AuthService;
import com.chatapp.server.service.FriendService;
import com.chatapp.server.service.GuildService;
import com.chatapp.server.service.MessageService;
import com.chatapp.server.service.PresenceService;

/** Composition root: wires DAOs -> services -> the socket server, then starts listening. */
public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        UserDao userDao = new UserDao();
        FriendDao friendDao = new FriendDao();
        ServerDao serverDao = new ServerDao();
        ChannelDao channelDao = new ChannelDao();
        MessageDao messageDao = new MessageDao();

        SessionManager sessionManager = new SessionManager();

        AuthService authService = new AuthService(userDao);
        FriendService friendService = new FriendService(friendDao, userDao, sessionManager);
        PresenceService presenceService = new PresenceService(userDao, friendDao, sessionManager);
        GuildService guildService = new GuildService(serverDao, channelDao);
        MessageService messageService = new MessageService(messageDao, userDao, friendDao, serverDao, channelDao, sessionManager);

        ChatServer server = new ChatServer(
                ServerConfig.serverPort(), sessionManager, authService, friendService, presenceService, guildService, messageService);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        try {
            server.start();
        } catch (Exception e) {
            log.error("Server failed to start", e);
            System.exit(1);
        }
    }
}
