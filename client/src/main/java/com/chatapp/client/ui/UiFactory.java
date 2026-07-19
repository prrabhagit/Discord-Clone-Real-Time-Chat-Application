package com.chatapp.client.ui;

import com.chatapp.shared.model.PresenceStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** Builds small reusable pieces of UI (avatars, presence dots) shared across the app. */
public final class UiFactory {

    private UiFactory() {
    }

    /** A colored circle with the first letter of the username, sized in pixels. */
    public static StackPane avatar(String username, String colorHex, double size) {
        Circle circle = new Circle(size / 2.0);
        try {
            circle.setFill(Color.web(colorHex == null ? "#5865F2" : colorHex));
        } catch (Exception e) {
            circle.setFill(Color.web("#5865F2"));
        }
        Label label = new Label(initial(username));
        label.getStyleClass().add(size <= 34 ? "avatar-label-small" : "avatar-label-medium");
        StackPane pane = new StackPane(circle, label);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setMinSize(size, size);
        return pane;
    }

    /** Avatar with a small presence-colored dot in the bottom-right corner. */
    public static StackPane avatarWithPresence(String username, String colorHex, double size, PresenceStatus status) {
        StackPane avatar = avatar(username, colorHex, size);
        Circle dot = new Circle(size / 6.0);
        dot.getStyleClass().add("presence-dot");
        dot.getStyleClass().add(presenceStyleClass(status));
        StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);
        StackPane wrapper = new StackPane(avatar, dot);
        wrapper.setPrefSize(size, size);
        return wrapper;
    }

    public static String presenceStyleClass(PresenceStatus status) {
        if (status == null) {
            return "presence-offline";
        }
        return switch (status) {
            case ONLINE -> "presence-online";
            case IDLE -> "presence-idle";
            case DO_NOT_DISTURB -> "presence-dnd";
            case OFFLINE -> "presence-offline";
        };
    }

    public static String presenceLabel(PresenceStatus status) {
        if (status == null) {
            return "Offline";
        }
        return switch (status) {
            case ONLINE -> "Online";
            case IDLE -> "Idle";
            case DO_NOT_DISTURB -> "Do Not Disturb";
            case OFFLINE -> "Offline";
        };
    }

    private static String initial(String username) {
        if (username == null || username.isEmpty()) {
            return "?";
        }
        return username.substring(0, 1).toUpperCase();
    }
}
