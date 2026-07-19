package com.chatapp.client.model;

import com.chatapp.client.net.SocketClient;
import com.chatapp.shared.model.ChannelDto;
import com.chatapp.shared.model.ChatMessageDto;
import com.chatapp.shared.model.FriendRequestDto;
import com.chatapp.shared.model.ServerDto;
import com.chatapp.shared.model.UserDto;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Holds all client-side session state: the logged-in user, friends, servers,
 * and whatever conversation is currently open. A single instance is created
 * at startup and shared by every controller, which is the simplest way to
 * keep the UI in sync with the socket connection without a full DI framework.
 */
public final class AppState {

    private static final AppState INSTANCE = new AppState();

    public static AppState get() {
        return INSTANCE;
    }

    private final SocketClient socketClient = new SocketClient();

    private final ObjectProperty<UserDto> currentUser = new SimpleObjectProperty<>();
    private final ObservableList<UserDto> friends = FXCollections.observableArrayList();
    private final ObservableList<FriendRequestDto> pendingRequests = FXCollections.observableArrayList();
    private final ObservableList<ServerDto> servers = FXCollections.observableArrayList();
    private final ObservableList<ChannelDto> currentChannels = FXCollections.observableArrayList();
    private final ObservableList<UserDto> currentServerMembers = FXCollections.observableArrayList();
    private final ObservableList<ChatMessageDto> currentMessages = FXCollections.observableArrayList();

    /** Which conversation is open: either a friend (DM) or a channel, never both. */
    private final ObjectProperty<UserDto> activeDmFriend = new SimpleObjectProperty<>();
    private final ObjectProperty<ChannelDto> activeChannel = new SimpleObjectProperty<>();
    private final ObjectProperty<ServerDto> activeServer = new SimpleObjectProperty<>();

    private AppState() {
    }

    public void reset() {
        currentUser.set(null);
        friends.clear();
        pendingRequests.clear();
        servers.clear();
        currentChannels.clear();
        currentServerMembers.clear();
        currentMessages.clear();
        activeDmFriend.set(null);
        activeChannel.set(null);
        activeServer.set(null);
    }

    public SocketClient socketClient() {
        return socketClient;
    }

    public ObjectProperty<UserDto> currentUserProperty() {
        return currentUser;
    }

    public UserDto getCurrentUser() {
        return currentUser.get();
    }

    public ObservableList<UserDto> friends() {
        return friends;
    }

    public ObservableList<FriendRequestDto> pendingRequests() {
        return pendingRequests;
    }

    public ObservableList<ServerDto> servers() {
        return servers;
    }

    public ObservableList<ChannelDto> currentChannels() {
        return currentChannels;
    }

    public ObservableList<UserDto> currentServerMembers() {
        return currentServerMembers;
    }

    public ObservableList<ChatMessageDto> currentMessages() {
        return currentMessages;
    }

    public ObjectProperty<UserDto> activeDmFriendProperty() {
        return activeDmFriend;
    }

    public ObjectProperty<ChannelDto> activeChannelProperty() {
        return activeChannel;
    }

    public ObjectProperty<ServerDto> activeServerProperty() {
        return activeServer;
    }
}
