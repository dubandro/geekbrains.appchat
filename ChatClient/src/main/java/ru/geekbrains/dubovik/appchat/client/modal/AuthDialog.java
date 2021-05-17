package ru.geekbrains.dubovik.appchat.client.modal;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import ru.geekbrains.dubovik.appchat.client.ChatController;
import ru.geekbrains.dubovik.appchat.common.MessageDTO;
import ru.geekbrains.dubovik.appchat.common.MessageType;
import ru.geekbrains.dubovik.appchat.network.MessageService;

import java.util.Optional;

import static javafx.scene.control.ButtonType.CANCEL;

public class AuthDialog {
    private static MessageService messageService;

    public static void authStart(MessageService service, boolean isAuthenticated) {
        messageService = service;
        if (!isAuthenticated) userLogIn();
        else userLogOut();
    }

    private static void userLogIn() {
        // Create the custom dialog
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("My Chat  —  Login");
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, CANCEL);
        // Create the username and password labels and fields
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 11, 11, 11));
        // TextField
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        // Adding TF to grid
        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        // Enable/Disable login button depending on whether a username was entered
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            if (messageService.isConnected) {
                loginButton.setDisable(newValue.trim().isEmpty());
            }
        });
        // Set content
        dialog.getDialogPane().setContent(grid);
        // Request focus on the username field by default.
        Platform.runLater(() -> username.requestFocus());

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter((ButtonType dialogButton) -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if (messageService.isConnected) {
            result.ifPresent(authPair -> {
                MessageDTO dto = new MessageDTO();
                dto.setLogin(authPair.getKey());
                dto.setPassword(authPair.getValue());
                dto.setMessageType(MessageType.AUTH_ON_MESSAGE);
                messageService.sendMessage(dto.convertToJson());
            });
        }
    }

    private static void userLogOut() {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(MessageType.AUTH_OFF_MESSAGE);
        messageService.sendMessage(dto.convertToJson());
        AlertDialog.showInform("See you soon...");
    }
}