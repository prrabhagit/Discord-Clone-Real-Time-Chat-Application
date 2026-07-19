package com.chatapp.client.controller;

import com.chatapp.client.model.AppState;
import com.chatapp.client.util.ClientConfig;
import com.chatapp.client.util.SceneManager;
import com.chatapp.shared.protocol.MessageType;
import com.chatapp.shared.protocol.payload.AuthPayloads;
import com.chatapp.shared.util.JsonUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;
    @FXML private Button goToLoginButton;

    private final AppState state = AppState.get();

    @FXML
    private void initialize() {
        errorLabel.setText("");
        registerButton.setOnAction(e -> handleRegister());
        confirmPasswordField.setOnAction(e -> handleRegister());
        goToLoginButton.setOnAction(e -> SceneManager.show("login", "ChatApp - Login", 1000, 650));
    }

    private void handleRegister() {
        String username = usernameField.getText().strip();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }
        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        registerButton.setDisable(true);
        errorLabel.setText("");

        try {
            if (!state.socketClient().isConnected()) {
                state.socketClient().connect(ClientConfig.serverHost(), ClientConfig.serverPort());
            }
        } catch (IOException e) {
            errorLabel.setText("Could not reach server: " + e.getMessage());
            registerButton.setDisable(false);
            return;
        }

        state.socketClient().on(MessageType.REGISTER_RESPONSE, envelope -> {
            var response = JsonUtil.payload(envelope, AuthPayloads.AuthResponse.class);
            registerButton.setDisable(false);
            if (response.success) {
                errorLabel.setStyle("-fx-text-fill: #57F287;");
                errorLabel.setText("Account created! You can now log in.");
                SceneManager.show("login", "ChatApp - Login", 1000, 650);
            } else {
                errorLabel.setStyle("-fx-text-fill: #ED4245;");
                errorLabel.setText(response.message);
            }
        });

        state.socketClient().send(MessageType.REGISTER, new AuthPayloads.RegisterRequest(username, password));
    }
}
