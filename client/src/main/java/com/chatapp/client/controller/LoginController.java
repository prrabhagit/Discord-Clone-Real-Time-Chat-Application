package com.chatapp.client.controller;

import com.chatapp.client.model.AppState;
import com.chatapp.client.util.ClientConfig;
import com.chatapp.client.util.SceneManager;
import com.chatapp.shared.model.PresenceStatus;
import com.chatapp.shared.model.UserDto;
import com.chatapp.shared.protocol.MessageType;
import com.chatapp.shared.protocol.payload.AuthPayloads;
import com.chatapp.shared.util.JsonUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button goToRegisterButton;

    private final AppState state = AppState.get();

    @FXML
    private void initialize() {
        errorLabel.setText("");
        passwordField.setOnAction(e -> handleLogin());
        loginButton.setOnAction(e -> handleLogin());
        goToRegisterButton.setOnAction(e -> SceneManager.show("register", "ChatApp - Register", 1000, 650));
    }

    private void handleLogin() {
        String username = usernameField.getText().strip();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter a username and password.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setText("");

        try {
            ensureConnected();
        } catch (IOException e) {
            errorLabel.setText("Could not reach server: " + e.getMessage());
            loginButton.setDisable(false);
            return;
        }

        state.socketClient().on(MessageType.LOGIN_RESPONSE, envelope -> {
            var response = JsonUtil.payload(envelope, AuthPayloads.AuthResponse.class);
            loginButton.setDisable(false);
            if (response.success) {
                UserDto user = response.user;
                user.setStatus(PresenceStatus.ONLINE);
                state.currentUserProperty().set(user);
                MainController controller = SceneManager.show("main", "ChatApp", 1200, 750);
                controller.onLoggedIn();
            } else {
                errorLabel.setText(response.message);
            }
        });

        state.socketClient().send(MessageType.LOGIN, new AuthPayloads.LoginRequest(username, password));
    }

    private void ensureConnected() throws IOException {
        if (!state.socketClient().isConnected()) {
            state.socketClient().connect(ClientConfig.serverHost(), ClientConfig.serverPort());
        }
    }
}
