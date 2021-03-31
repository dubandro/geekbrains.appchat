package ru.geekbrains.dubovik.appchat.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import ru.geekbrains.dubovik.appchat.client.modal.AlertDialog;
import ru.geekbrains.dubovik.appchat.client.modal.AuthDialog;
import ru.geekbrains.dubovik.appchat.common.MessageDTO;
import ru.geekbrains.dubovik.appchat.common.MessageType;
import ru.geekbrains.dubovik.appchat.network.MessageService;
import ru.geekbrains.dubovik.appchat.network.MessageProcessor;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class ChatController implements Initializable, MessageProcessor {
    private String me;
    private boolean isAuthenticated;
    private MessageService messageService;
    private final String ALL = "PUBLIC ROOM";

    @FXML
    public ListView onlineUsers;
    public TextArea chatArea;
    public TextArea inputText;
    public Button btnSendMessage;
    public Button btnAttachment;
    public MenuItem menuSignIn;
    public MenuItem menuSignOut;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        menuSignIn.fire();
        refreshUserList();
    }

    public void signIn(ActionEvent actionEvent) {
        if (messageService == null) messageService = new MessageService("localhost", 65500, this);
        else {
            if (!messageService.isConnected) messageService.connectToServer();
        }
        AuthDialog.authStart(messageService, isAuthenticated);
    }

    public void signOut(ActionEvent actionEvent) {
        AuthDialog.authStart(messageService, isAuthenticated);
    }

    public void preferences(ActionEvent actionEvent) {
    }

    public void quit(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void delete(ActionEvent actionEvent) {
        inputText.clear();
    }

    public void help(ActionEvent actionEvent) throws URISyntaxException, IOException {
        Desktop desktop = Desktop.getDesktop();
        desktop.browse(new URI("https://github.com/dubandro/geekbrains.appchat"));
    }

    public void about(ActionEvent actionEvent) throws URISyntaxException {
        AlertDialog.showInform("My Chat\ndubandro for geekbrains\n\u00A9 2021");
    }

    public void btnAttach(ActionEvent actionEvent) throws IOException {
    }

    public void pressEnter(KeyEvent actionEvent) {
        if (actionEvent.getCode().equals(KeyCode.ENTER) && actionEvent.isShiftDown()) sendMessage();
    }

    public void btnSend(ActionEvent actionEvent) {
        sendMessage();
    }

    public void sendMessage() {
        if (isAuthenticated) {
            String msg = inputText.getText() + "\n";
            if (msg.isBlank()) return;
            msg = msg.replaceAll("[\\n]+","\n");
            if (msg.lines().count() > 1) msg = "\n" + msg;
            MessageDTO dto = new MessageDTO();
            String selected = (String) onlineUsers.getSelectionModel().getSelectedItem();
            if (selected.equals(ALL)) dto.setMessageType(MessageType.PUBLIC_MESSAGE);
            else {
                dto.setMessageType(MessageType.PRIVATE_MESSAGE);
                dto.setTo(selected);
                String myMsg = String.format("[me]  to  [%s]:  %s\n", selected, msg);
                chatArea.appendText(myMsg);
            }
            dto.setBody(msg);
            messageService.sendMessage(dto.convertToJson());
            inputText.clear();
        } else AlertDialog.showInform("Authentication is required!");
    }

    private void showMessage(MessageDTO dto) {
        String msgType= String.valueOf(dto.getMessageType());
        if (dto.getMessageType().equals(MessageType.PRIVATE_MESSAGE)) msgType = msgType.toLowerCase();
        String msg = String.format("[%s]  from  [%s]:  %s\n", msgType, dto.getFrom(), dto.getBody());
        chatArea.appendText(msg);
    }

    private void refreshUserList(MessageDTO dto) {
        dto.getUsersOnline().remove(me);
        dto.getUsersOnline().add(0, ALL);
        onlineUsers.setItems(FXCollections.observableArrayList(dto.getUsersOnline()));
        onlineUsers.getSelectionModel().selectFirst();
    }

    private void refreshUserList() {
        onlineUsers.setItems(FXCollections.observableArrayList(ALL));
    }

    @Override
    public void processMessage(String msg) {
        Platform.runLater(() -> {
            MessageDTO dto = MessageDTO.convertFromJson(msg);
            switch (dto.getMessageType()) {
                case PUBLIC_MESSAGE, PRIVATE_MESSAGE -> showMessage(dto);
                case CLIENTS_LIST_MESSAGE -> refreshUserList(dto);
                case ERROR_MESSAGE -> AlertDialog.showError(dto);
                case AUTH_ON_MESSAGE -> {
                    isAuthenticated = true;
                    menuSignIn.setDisable(true);
                    menuSignOut.setDisable(false);
                    onlineUsers.setDisable(false);
                    chatArea.clear();
                    me = dto.getBody();
                    ChatMainGUI.getStage().setTitle("My Chat  —  " + me);
                }
                case AUTH_OFF_MESSAGE -> {
                    isAuthenticated = false;
                    menuSignIn.setDisable(false);
                    menuSignOut.setDisable(true);
                    onlineUsers.setDisable(true);
                    me = null;
                    ChatMainGUI.getStage().setTitle("My Chat  —  " + dto.getBody());
                }
                case SERVICE_MESSAGE -> {
                    ChatMainGUI.getStage().setTitle("My Chat  —  " + dto.getBody());
                    if (dto.getBody().contains("offLine")) messageService.disconnectToServer();
                }
            }
        });
    }
}