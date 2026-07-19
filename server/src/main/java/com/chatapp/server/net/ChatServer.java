package com.chatapp.server.net;

import com.chatapp.server.service.AuthService;
import com.chatapp.server.service.FriendService;
import com.chatapp.server.service.GuildService;
import com.chatapp.server.service.MessageService;
import com.chatapp.server.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Listens on the configured TCP port and hands each accepted connection off to a {@link ClientHandler}. */
public class ChatServer {

    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);

    private final int port;
    private final SessionManager sessionManager;
    private final AuthService authService;
    private final FriendService friendService;
    private final PresenceService presenceService;
    private final GuildService guildService;
    private final MessageService messageService;
    private final ExecutorService clientPool = Executors.newVirtualThreadPerTaskExecutor();

    private ServerSocket serverSocket;

    public ChatServer(int port, SessionManager sessionManager, AuthService authService, FriendService friendService,
                       PresenceService presenceService, GuildService guildService, MessageService messageService) {
        this.port = port;
        this.sessionManager = sessionManager;
        this.authService = authService;
        this.friendService = friendService;
        this.presenceService = presenceService;
        this.guildService = guildService;
        this.messageService = messageService;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        log.info("ChatApp server listening on port {}", port);

        while (!serverSocket.isClosed()) {
            Socket clientSocket = serverSocket.accept();
            log.info("New connection from {}", clientSocket.getRemoteSocketAddress());
            ClientHandler handler = new ClientHandler(
                    clientSocket, sessionManager, authService, friendService, presenceService, guildService, messageService);
            clientPool.submit(handler);
        }
    }

    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.warn("Error closing server socket", e);
        }
        clientPool.shutdown();
    }
}
