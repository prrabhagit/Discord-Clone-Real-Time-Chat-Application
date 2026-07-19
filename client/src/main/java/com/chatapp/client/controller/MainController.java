package com.chatapp.client.controller;

import com.chatapp.client.model.AppState;
import com.chatapp.client.net.SocketClient;
import com.chatapp.client.ui.UiFactory;
import com.chatapp.client.util.SceneManager;
import com.chatapp.shared.model.ChannelDto;
import com.chatapp.shared.model.ChatMessageDto;
import com.chatapp.shared.model.FriendRequestDto;
import com.chatapp.shared.model.PresenceStatus;
import com.chatapp.shared.model.ServerDto;
import com.chatapp.shared.model.UserDto;
import com.chatapp.shared.protocol.MessageType;
import com.chatapp.shared.protocol.payload.ChannelPayloads;
import com.chatapp.shared.protocol.payload.CommonPayloads;
import com.chatapp.shared.protocol.payload.FriendPayloads;
import com.chatapp.shared.protocol.payload.MessagePayloads;
import com.chatapp.shared.protocol.payload.PresencePayloads;
import com.chatapp.shared.protocol.payload.ServerPayloads;
import com.chatapp.shared.util.JsonUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the whole post-login experience: the server rail, the
 * channel/DM sidebar, the chat area, and the members panel. Kept as a single
 * controller (rather than one per panel) since every panel constantly needs
 * to react to state changes in the others.
 */
public class MainController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a")
            .withZone(ZoneId.systemDefault());

    @FXML private StackPane homeButton;
    @FXML private ListView<ServerDto> serverListView;
    @FXML private StackPane addServerButton;

    @FXML private Label sidebarTitleLabel;
    @FXML private Button sidebarActionButton;
    @FXML private ScrollPane sidebarScrollPane;
    @FXML private VBox sidebarListContainer;

    @FXML private Circle myAvatarCircle;
    @FXML private Label myAvatarLabel;
    @FXML private Label myUsernameLabel;
    @FXML private Label myStatusLabel;
    @FXML private Button presenceButton;

    @FXML private Label chatHeaderLabel;
    @FXML private Button membersToggleButton;
    @FXML private ScrollPane messageScrollPane;
    @FXML private VBox messageListContainer;
    @FXML private Label typingIndicatorLabel;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;

    @FXML private VBox membersPanel;
    @FXML private VBox membersListContainer;

    private final AppState state = AppState.get();
    private SocketClient client;

    private enum SidebarMode { HOME, SERVER }
    private SidebarMode mode = SidebarMode.HOME;

    private boolean isTyping = false;
    private final PauseTransition typingStopTimer = new PauseTransition(Duration.seconds(3));
    private final PauseTransition remoteTypingClearTimer = new PauseTransition(Duration.seconds(5));

    @FXML
    private void initialize() {
        client = state.socketClient();

        homeButton.setOnMouseClicked(e -> selectHome());
        addServerButton.setOnMouseClicked(e -> showAddServerMenu());
        sendButton.setOnAction(e -> sendCurrentMessage());
        messageInput.setOnAction(e -> sendCurrentMessage());
        presenceButton.setOnAction(e -> showPresenceMenu());
        membersToggleButton.setOnAction(e -> toggleMembersPanel());

        serverListView.setCellFactory(list -> new ServerCell());
        serverListView.getItems().setAll(state.servers());
        state.servers().addListener((javafx.collections.ListChangeListener<ServerDto>) c -> serverListView.getItems().setAll(state.servers()));
        serverListView.setOnMouseClicked(e -> {
            ServerDto selected = serverListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectServer(selected);
            }
        });

        messageInput.textProperty().addListener((obs, old, text) -> handleTypingChanged(text));
        typingStopTimer.setOnFinished(e -> stopTyping());
        remoteTypingClearTimer.setOnFinished(e -> typingIndicatorLabel.setText(""));

        client.onDisconnected(this::handleDisconnected);
        registerListeners();

        membersPanel.managedProperty().bind(membersPanel.visibleProperty());
    }

    /** Called once by LoginController right after the scene is shown, to kick off initial data loading. */
    public void onLoggedIn() {
        UserDto me = state.getCurrentUser();
        myUsernameLabel.setText(me.getUsername());
        myAvatarCircle.setFill(safeColor(me.getAvatarColor()));
        myAvatarLabel.setText(me.getUsername().substring(0, 1).toUpperCase());
        myStatusLabel.setText(UiFactory.presenceLabel(me.getStatus()));

        client.send(MessageType.GET_FRIENDS, null);
        client.send(MessageType.GET_PENDING_REQUESTS, null);
        client.send(MessageType.GET_SERVERS, null);

        selectHome();
    }

    // =====================================================================
    // Socket listeners
    // =====================================================================

    private void registerListeners() {
        client.on(MessageType.ERROR, env -> {
            var payload = JsonUtil.payload(env, CommonPayloads.ErrorPayload.class);
            showAlert(Alert.AlertType.ERROR, payload.message);
        });

        client.on(MessageType.FRIENDS_LIST, env -> {
            var payload = JsonUtil.payload(env, FriendPayloads.FriendsList.class);
            state.friends().setAll(payload.friends);
            if (mode == SidebarMode.HOME) renderSidebar();
        });

        client.on(MessageType.PENDING_REQUESTS_LIST, env -> {
            var payload = JsonUtil.payload(env, FriendPayloads.PendingRequestsList.class);
            state.pendingRequests().setAll(payload.requests);
            if (mode == SidebarMode.HOME) renderSidebar();
        });

        client.on(MessageType.FRIEND_REQUEST_RECEIVED, env -> {
            var request = JsonUtil.payload(env, FriendRequestDto.class);
            state.pendingRequests().add(request);
            if (mode == SidebarMode.HOME) renderSidebar();
        });

        client.on(MessageType.FRIEND_ADDED, env -> {
            var payload = JsonUtil.payload(env, FriendPayloads.FriendAdded.class);
            state.friends().removeIf(f -> f.getId() == payload.friend.getId());
            state.friends().add(payload.friend);
            if (mode == SidebarMode.HOME) renderSidebar();
        });

        client.on(MessageType.FRIEND_REMOVED, env -> {
            var payload = JsonUtil.payload(env, FriendPayloads.FriendRemoved.class);
            state.friends().removeIf(f -> f.getId() == payload.friendId);
            UserDto activeDm = state.activeDmFriendProperty().get();
            if (activeDm != null && activeDm.getId() == payload.friendId) {
                state.activeDmFriendProperty().set(null);
                clearChat("Select a conversation");
            }
            if (mode == SidebarMode.HOME) renderSidebar();
        });

        client.on(MessageType.PRESENCE_UPDATE, env -> {
            var payload = JsonUtil.payload(env, PresencePayloads.PresenceUpdate.class);
            updatePresenceEverywhere(payload.userId, payload.status);
        });

        client.on(MessageType.DIRECT_MESSAGE, env -> {
            var message = JsonUtil.payload(env, ChatMessageDto.class);
            UserDto activeDm = state.activeDmFriendProperty().get();
            int me = state.getCurrentUser().getId();
            boolean forActiveConversation = activeDm != null &&
                    ((message.getSenderId() == activeDm.getId() && message.getReceiverId() == me) ||
                     (message.getSenderId() == me && message.getReceiverId() == activeDm.getId()));
            if (forActiveConversation) {
                state.currentMessages().add(message);
                appendMessageRow(message);
                scrollMessagesToBottom();
                if (message.getSenderId() == activeDm.getId()) {
                    typingIndicatorLabel.setText("");
                }
            }
        });

        client.on(MessageType.DM_HISTORY, env -> {
            var payload = JsonUtil.payload(env, MessagePayloads.DmHistory.class);
            UserDto activeDm = state.activeDmFriendProperty().get();
            if (activeDm != null && activeDm.getId() == payload.friendId) {
                state.currentMessages().setAll(payload.messages);
                renderMessages();
            }
        });

        client.on(MessageType.TYPING_INDICATOR, env -> {
            var event = JsonUtil.payload(env, MessagePayloads.TypingEvent.class);
            handleRemoteTyping(event);
        });

        client.on(MessageType.SERVERS_LIST, env -> {
            var payload = JsonUtil.payload(env, ServerPayloads.ServersList.class);
            state.servers().setAll(payload.servers);
        });

        client.on(MessageType.SERVER_CREATED, env -> {
            var payload = JsonUtil.payload(env, ServerPayloads.ServerPayload.class);
            state.servers().add(payload.server);
            selectServer(payload.server);
            showAlert(Alert.AlertType.INFORMATION, "Server created! Invite code: " + payload.server.getInviteCode());
        });

        client.on(MessageType.SERVER_JOINED, env -> {
            var payload = JsonUtil.payload(env, ServerPayloads.ServerPayload.class);
            state.servers().add(payload.server);
            selectServer(payload.server);
        });

        client.on(MessageType.SERVER_LEFT, env -> {
            var payload = JsonUtil.payload(env, ServerPayloads.ServerLeft.class);
            state.servers().removeIf(s -> s.getId() == payload.serverId);
            ServerDto activeServer = state.activeServerProperty().get();
            if (activeServer != null && activeServer.getId() == payload.serverId) {
                selectHome();
            }
        });

        client.on(MessageType.SERVER_MEMBERS_LIST, env -> {
            var payload = JsonUtil.payload(env, ServerPayloads.ServerMembersList.class);
            ServerDto activeServer = state.activeServerProperty().get();
            if (activeServer != null && activeServer.getId() == payload.serverId) {
                state.currentServerMembers().setAll(payload.members);
                renderMembers();
            }
        });

        client.on(MessageType.CHANNELS_LIST, env -> {
            var payload = JsonUtil.payload(env, ChannelPayloads.ChannelsList.class);
            ServerDto activeServer = state.activeServerProperty().get();
            if (activeServer != null && activeServer.getId() == payload.serverId) {
                state.currentChannels().setAll(payload.channels);
                if (mode == SidebarMode.SERVER) renderSidebar();
                if (state.activeChannelProperty().get() == null && !payload.channels.isEmpty()) {
                    openChannel(payload.channels.get(0));
                }
            }
        });

        client.on(MessageType.CHANNEL_CREATED, env -> {
            var payload = JsonUtil.payload(env, ChannelPayloads.ChannelPayload.class);
            ServerDto activeServer = state.activeServerProperty().get();
            if (activeServer != null && activeServer.getId() == payload.channel.getServerId()) {
                state.currentChannels().add(payload.channel);
                if (mode == SidebarMode.SERVER) renderSidebar();
            }
        });

        client.on(MessageType.CHANNEL_MESSAGE, env -> {
            var message = JsonUtil.payload(env, ChatMessageDto.class);
            ChannelDto activeChannel = state.activeChannelProperty().get();
            if (activeChannel != null && message.getChannelId() != null && activeChannel.getId() == message.getChannelId()) {
                state.currentMessages().add(message);
                appendMessageRow(message);
                scrollMessagesToBottom();
                typingIndicatorLabel.setText("");
            }
        });

        client.on(MessageType.CHANNEL_HISTORY, env -> {
            var payload = JsonUtil.payload(env, MessagePayloads.ChannelHistory.class);
            ChannelDto activeChannel = state.activeChannelProperty().get();
            if (activeChannel != null && activeChannel.getId() == payload.channelId) {
                state.currentMessages().setAll(payload.messages);
                renderMessages();
            }
        });
    }

    private void handleDisconnected() {
        showAlert(Alert.AlertType.ERROR, "Lost connection to the server.");
        state.reset();
        SceneManager.show("login", "ChatApp - Login", 1000, 650);
    }

    // =====================================================================
    // Navigation: home / servers / DMs / channels
    // =====================================================================

    private void selectHome() {
        mode = SidebarMode.HOME;
        state.activeServerProperty().set(null);
        sidebarTitleLabel.setText("Direct Messages");
        sidebarActionButton.setText("＋");
        sidebarActionButton.setOnAction(e -> showAddFriendDialog());
        membersPanel.setVisible(false);
        serverListView.getSelectionModel().clearSelection();
        renderSidebar();
        if (state.activeDmFriendProperty().get() == null) {
            clearChat("Select a friend to start chatting");
        }
    }

    private void selectServer(ServerDto server) {
        mode = SidebarMode.SERVER;
        state.activeServerProperty().set(server);
        state.activeChannelProperty().set(null);
        sidebarTitleLabel.setText(server.getName());
        sidebarActionButton.setText("＋");
        sidebarActionButton.setOnAction(e -> showCreateChannelDialog(server));
        state.currentChannels().clear();
        clearChat("Select a channel");
        renderSidebar();

        client.send(MessageType.GET_CHANNELS, new ChannelPayloads.GetChannelsRequest(server.getId()));
        client.send(MessageType.GET_SERVER_MEMBERS, new ServerPayloads.GetServerMembersRequest(server.getId()));
    }

    private void openDm(UserDto friend) {
        stopTyping();
        state.activeDmFriendProperty().set(friend);
        state.currentMessages().clear();
        chatHeaderLabel.setText("@ " + friend.getUsername());
        membersPanel.setVisible(false);
        typingIndicatorLabel.setText("");
        client.send(MessageType.GET_DM_HISTORY, new MessagePayloads.GetDmHistory(friend.getId()));
        renderSidebar();
    }

    private void openChannel(ChannelDto channel) {
        stopTyping();
        state.activeChannelProperty().set(channel);
        state.currentMessages().clear();
        chatHeaderLabel.setText("# " + channel.getName());
        typingIndicatorLabel.setText("");
        client.send(MessageType.GET_CHANNEL_HISTORY, new MessagePayloads.GetChannelHistory(channel.getId()));
        renderSidebar();
    }

    private void clearChat(String headerText) {
        chatHeaderLabel.setText(headerText);
        state.currentMessages().clear();
        messageListContainer.getChildren().clear();
        typingIndicatorLabel.setText("");
    }

    private void toggleMembersPanel() {
        if (state.activeServerProperty().get() != null) {
            membersPanel.setVisible(!membersPanel.isVisible());
        }
    }

    // =====================================================================
    // Sidebar rendering
    // =====================================================================

    private void renderSidebar() {
        sidebarListContainer.getChildren().clear();
        if (mode == SidebarMode.HOME) {
            renderHomeSidebar();
        } else {
            renderServerSidebar();
        }
    }

    private void renderHomeSidebar() {
        List<FriendRequestDto> requests = state.pendingRequests();
        if (!requests.isEmpty()) {
            sidebarListContainer.getChildren().add(sectionLabel("PENDING REQUESTS — " + requests.size()));
            for (FriendRequestDto request : requests) {
                sidebarListContainer.getChildren().add(buildRequestRow(request));
            }
        }

        sidebarListContainer.getChildren().add(sectionLabel("FRIENDS — " + state.friends().size()));
        UserDto activeDm = state.activeDmFriendProperty().get();
        for (UserDto friend : state.friends()) {
            boolean selected = activeDm != null && activeDm.getId() == friend.getId();
            sidebarListContainer.getChildren().add(buildFriendRow(friend, selected));
        }
        if (state.friends().isEmpty()) {
            Label empty = new Label("No friends yet — add one with the + button above.");
            empty.setWrapText(true);
            empty.getStyleClass().add("list-row-subtitle");
            empty.setPadding(new Insets(8));
            sidebarListContainer.getChildren().add(empty);
        }
    }

    private void renderServerSidebar() {
        sidebarListContainer.getChildren().add(sectionLabel("TEXT CHANNELS"));
        ChannelDto activeChannel = state.activeChannelProperty().get();
        for (ChannelDto channel : state.currentChannels()) {
            boolean selected = activeChannel != null && activeChannel.getId() == channel.getId();
            sidebarListContainer.getChildren().add(buildChannelRow(channel, selected));
        }
    }

    private HBox buildFriendRow(UserDto friend, boolean selected) {
        HBox row = new HBox(10);
        row.getStyleClass().add("list-row");
        if (selected) row.getStyleClass().add("list-row-selected");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(UiFactory.avatarWithPresence(friend.getUsername(), friend.getAvatarColor(), 32, friend.getStatus()));
        VBox labels = new VBox();
        Label name = new Label(friend.getUsername());
        name.getStyleClass().add("list-row-title");
        Label status = new Label(UiFactory.presenceLabel(friend.getStatus()));
        status.getStyleClass().add("list-row-subtitle");
        labels.getChildren().addAll(name, status);
        row.getChildren().add(labels);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        row.getChildren().add(spacer);

        Button removeButton = new Button("✕");
        removeButton.getStyleClass().add("icon-button");
        removeButton.setOnAction(e -> {
            e.consume();
            confirmAndRemoveFriend(friend);
        });
        row.getChildren().add(removeButton);

        row.setOnMouseClicked(e -> openDm(friend));
        return row;
    }

    private VBox buildRequestRow(FriendRequestDto request) {
        VBox row = new VBox(6);
        row.getStyleClass().add("list-row");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().add(UiFactory.avatar(request.getSenderUsername(), request.getSenderAvatarColor(), 28));
        Label name = new Label(request.getSenderUsername());
        name.getStyleClass().add("list-row-title");
        top.getChildren().add(name);
        row.getChildren().add(top);

        HBox actions = new HBox(8);
        Button accept = new Button("Accept");
        accept.getStyleClass().add("primary-button");
        accept.setOnAction(e -> {
            client.send(MessageType.ACCEPT_FRIEND_REQUEST, new FriendPayloads.RespondFriendRequest(request.getRequestId()));
            state.pendingRequests().removeIf(r -> r.getRequestId() == request.getRequestId());
            renderSidebar();
        });
        Button decline = new Button("Decline");
        decline.getStyleClass().add("icon-button");
        decline.setOnAction(e -> {
            client.send(MessageType.DECLINE_FRIEND_REQUEST, new FriendPayloads.RespondFriendRequest(request.getRequestId()));
            state.pendingRequests().removeIf(r -> r.getRequestId() == request.getRequestId());
            renderSidebar();
        });
        actions.getChildren().addAll(accept, decline);
        row.getChildren().add(actions);

        return row;
    }

    private HBox buildChannelRow(ChannelDto channel, boolean selected) {
        HBox row = new HBox(8);
        row.getStyleClass().add("list-row");
        if (selected) row.getStyleClass().add("list-row-selected");
        row.setAlignment(Pos.CENTER_LEFT);
        Label hash = new Label("#");
        hash.getStyleClass().add("list-row-subtitle");
        Label name = new Label(channel.getName());
        name.getStyleClass().add("list-row-title");
        row.getChildren().addAll(hash, name);
        row.setOnMouseClicked(e -> openChannel(channel));
        return row;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-label");
        return label;
    }

    // =====================================================================
    // Members panel rendering
    // =====================================================================

    private void renderMembers() {
        membersListContainer.getChildren().clear();
        for (UserDto member : state.currentServerMembers()) {
            HBox row = new HBox(10);
            row.getStyleClass().add("list-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().add(UiFactory.avatarWithPresence(member.getUsername(), member.getAvatarColor(), 28, member.getStatus()));
            Label name = new Label(member.getUsername());
            name.getStyleClass().add("list-row-title");
            row.getChildren().add(name);
            membersListContainer.getChildren().add(row);
        }
        if (state.activeServerProperty().get() != null) {
            membersPanel.setVisible(true);
        }
    }

    // =====================================================================
    // Message rendering
    // =====================================================================

    private void renderMessages() {
        messageListContainer.getChildren().clear();
        for (ChatMessageDto message : state.currentMessages()) {
            appendMessageRow(message);
        }
        scrollMessagesToBottom();
    }

    private void appendMessageRow(ChatMessageDto message) {
        boolean own = message.getSenderId() == state.getCurrentUser().getId();

        HBox row = new HBox(12);
        row.getStyleClass().add("message-row");
        row.setAlignment(Pos.TOP_LEFT);

        row.getChildren().add(UiFactory.avatar(message.getSenderUsername(), message.getSenderAvatarColor(), 36));

        VBox content = new VBox(2);
        HBox header = new HBox(8);
        header.setAlignment(Pos.BASELINE_LEFT);
        Label author = new Label(message.getSenderUsername());
        author.getStyleClass().add("message-author");
        Label timestamp = new Label(formatTimestamp(message.getTimestamp()));
        timestamp.getStyleClass().add("message-timestamp");
        header.getChildren().addAll(author, timestamp);
        if (own) row.getStyleClass().add("message-own");

        Label body = new Label(message.getContent());
        body.getStyleClass().add("message-content");
        body.setWrapText(true);
        body.maxWidthProperty().bind(messageScrollPane.widthProperty().subtract(120));

        content.getChildren().addAll(header, body);
        row.getChildren().add(content);

        messageListContainer.getChildren().add(row);
    }

    private void scrollMessagesToBottom() {
        Platform.runLater(() -> messageScrollPane.setVvalue(1.0));
    }

    private String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0) return "";
        return TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis));
    }

    // =====================================================================
    // Sending messages & typing indicator
    // =====================================================================

    private void sendCurrentMessage() {
        String text = messageInput.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        UserDto activeDm = state.activeDmFriendProperty().get();
        ChannelDto activeChannel = state.activeChannelProperty().get();

        if (activeDm != null) {
            client.send(MessageType.SEND_DIRECT_MESSAGE, new MessagePayloads.SendDirectMessage(activeDm.getId(), text.strip()));
        } else if (activeChannel != null) {
            client.send(MessageType.SEND_CHANNEL_MESSAGE, new MessagePayloads.SendChannelMessage(activeChannel.getId(), text.strip()));
        } else {
            return;
        }
        messageInput.clear();
        stopTyping();
    }

    private void handleTypingChanged(String text) {
        UserDto activeDm = state.activeDmFriendProperty().get();
        ChannelDto activeChannel = state.activeChannelProperty().get();
        if (activeDm == null && activeChannel == null) {
            return;
        }
        if (text != null && !text.isEmpty()) {
            if (!isTyping) {
                isTyping = true;
                sendTypingEvent(true);
            }
            typingStopTimer.playFromStart();
        } else {
            stopTyping();
        }
    }

    private void stopTyping() {
        if (isTyping) {
            isTyping = false;
            sendTypingEvent(false);
        }
        typingStopTimer.stop();
    }

    private void sendTypingEvent(boolean typing) {
        UserDto activeDm = state.activeDmFriendProperty().get();
        ChannelDto activeChannel = state.activeChannelProperty().get();
        Integer receiverId = activeDm != null ? activeDm.getId() : null;
        Integer channelId = activeChannel != null ? activeChannel.getId() : null;
        var event = new MessagePayloads.TypingEvent(receiverId, channelId, 0, null, typing);
        client.send(typing ? MessageType.TYPING_START : MessageType.TYPING_STOP, event);
    }

    private void handleRemoteTyping(MessagePayloads.TypingEvent event) {
        UserDto activeDm = state.activeDmFriendProperty().get();
        ChannelDto activeChannel = state.activeChannelProperty().get();

        boolean matches = (activeDm != null && event.channelId == null && activeDm.getId() == event.userId)
                || (activeChannel != null && event.channelId != null && activeChannel.getId() == event.channelId);

        if (!matches) {
            return;
        }
        if (event.typing) {
            typingIndicatorLabel.setText(event.username + " is typing...");
            remoteTypingClearTimer.playFromStart();
        } else {
            typingIndicatorLabel.setText("");
            remoteTypingClearTimer.stop();
        }
    }

    // =====================================================================
    // Presence
    // =====================================================================

    private void updatePresenceEverywhere(int userId, PresenceStatus status) {
        state.friends().stream().filter(f -> f.getId() == userId).findFirst().ifPresent(f -> f.setStatus(status));
        state.currentServerMembers().stream().filter(m -> m.getId() == userId).findFirst().ifPresent(m -> m.setStatus(status));
        if (mode == SidebarMode.HOME) renderSidebar();
        if (membersPanel.isVisible()) renderMembers();
    }

    private void showPresenceMenu() {
        ChoiceDialog<PresenceStatus> dialog = new ChoiceDialog<>(PresenceStatus.ONLINE,
                PresenceStatus.ONLINE, PresenceStatus.IDLE, PresenceStatus.DO_NOT_DISTURB);
        dialog.setTitle("Set status");
        dialog.setHeaderText(null);
        dialog.setContentText("Choose your status:");
        dialog.showAndWait().ifPresent(status -> {
            client.send(MessageType.SET_PRESENCE, new PresencePayloads.SetPresenceRequest(status));
            state.getCurrentUser().setStatus(status);
            myStatusLabel.setText(UiFactory.presenceLabel(status));
        });
    }

    // =====================================================================
    // Dialogs: add friend / create server / join server / create channel
    // =====================================================================

    private void showAddFriendDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Friend");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter a username:");
        dialog.showAndWait().ifPresent(username -> {
            if (!username.isBlank()) {
                client.send(MessageType.SEND_FRIEND_REQUEST, new FriendPayloads.SendFriendRequest(username.strip()));
            }
        });
    }

    private void confirmAndRemoveFriend(UserDto friend) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + friend.getUsername() + " from your friends?");
        confirm.setHeaderText(null);
        confirm.showAndWait().filter(bt -> bt.getButtonData().isDefaultButton()).ifPresent(bt ->
                client.send(MessageType.REMOVE_FRIEND, new FriendPayloads.RemoveFriend(friend.getId())));
    }

    private void showAddServerMenu() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Create a server", "Create a server", "Join a server");
        dialog.setTitle("Servers");
        dialog.setHeaderText(null);
        dialog.setContentText("What would you like to do?");
        Optional<String> choice = dialog.showAndWait();
        if (choice.isEmpty()) return;
        if (choice.get().startsWith("Create")) {
            showCreateServerDialog();
        } else {
            showJoinServerDialog();
        }
    }

    private void showCreateServerDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create a server");
        dialog.setHeaderText(null);
        dialog.setContentText("Server name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                client.send(MessageType.CREATE_SERVER, new ServerPayloads.CreateServerRequest(name.strip()));
            }
        });
    }

    private void showJoinServerDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join a server");
        dialog.setHeaderText(null);
        dialog.setContentText("Invite code:");
        dialog.showAndWait().ifPresent(code -> {
            if (!code.isBlank()) {
                client.send(MessageType.JOIN_SERVER, new ServerPayloads.JoinServerRequest(code.strip()));
            }
        });
    }

    private void showCreateChannelDialog(ServerDto server) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create a channel");
        dialog.setHeaderText(null);
        dialog.setContentText("Channel name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                client.send(MessageType.CREATE_CHANNEL, new ChannelPayloads.CreateChannelRequest(server.getId(), name.strip()));
            }
        });
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(null);
        alert.show();
    }

    private Color safeColor(String hex) {
        try {
            return Color.web(hex == null ? "#5865F2" : hex);
        } catch (Exception e) {
            return Color.web("#5865F2");
        }
    }

    // =====================================================================
    // Server rail list cell
    // =====================================================================

    private class ServerCell extends ListCell<ServerDto> {
        @Override
        protected void updateItem(ServerDto server, boolean empty) {
            super.updateItem(server, empty);
            if (empty || server == null) {
                setGraphic(null);
                return;
            }
            StackPane icon = new StackPane();
            icon.getStyleClass().add("server-icon");
            ServerDto activeServer = state.activeServerProperty().get();
            if (activeServer != null && activeServer.getId() == server.getId()) {
                icon.getStyleClass().add("server-icon-selected");
            }
            Circle circle = new Circle(24);
            circle.setFill(safeColor(server.getIconColor()));
            Label label = new Label(server.getIconText());
            label.getStyleClass().add("server-icon-label");
            icon.getChildren().addAll(circle, label);
            icon.setPrefSize(48, 48);
            setGraphic(icon);
            setText(null);
        }
    }
}
