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
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class ChatController implements Initializable, MessageProcessor {
    private String me;
    private boolean isAuthenticated;
    private MessageService messageService;
    private final String ALL = "PUBLIC ROOM";
    private final int LINEHISTORY = 10;
    private String historyFile;
    private LocalHistory history;

    @FXML
    public ListView onlineUsers;
    public TextArea chatArea;
    public TextArea inputText;
    public Button btnSendMessage;
    public Button btnAttachment;
    public MenuItem menuSignIn;
    public MenuItem menuSignOut;
    public MenuItem menuAnonymous;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        chatStatus("Connect to Server");
        menuSignIn.fire();
        refreshUserList();
    }

    public void signIn(ActionEvent actionEvent) {
        if (messageService == null) messageService = new MessageService("localhost", 65500, this);
        else {
            if (!messageService.isConnected) messageService.connectToServer();
        }
        if (messageService.isConnected) AuthDialog.authStart(messageService, isAuthenticated);
    }

    public void signOut(ActionEvent actionEvent) {
        AuthDialog.authStart(messageService, isAuthenticated);
    }

    public void preferences(ActionEvent actionEvent) {
    }

    public void anonymous(ActionEvent actionEvent) {
        //Сделано для проверки работы функции смены имени в базе данных TODO сделать полноценный gui
        changeName("@nonymous");
    }

    public void quit(ActionEvent actionEvent) {
        history.closeLocalHistory();
        messageService.disconnectToServer();
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
        if (messageService.isConnected) {
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
                    writeHistory(myMsg);
                }
                dto.setBody(msg);
                messageService.sendMessage(dto.convertToJson());
                inputText.clear();
            } else AlertDialog.showInform("Authentication is required!");
        }
    }

    private void changeName(String newName) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(MessageType.AUTH_CHANGE_NAME);
        dto.setBody(newName);
        messageService.sendMessage(dto.convertToJson());
    }

    private void showMessage(MessageDTO dto) {
        String msgType= String.valueOf(dto.getMessageType());
        if (dto.getMessageType().equals(MessageType.PRIVATE_MESSAGE)) msgType = msgType.toLowerCase();
        String msg = String.format("[%s]  from  [%s]:  %s\n", msgType, dto.getFrom(), dto.getBody());
        chatArea.appendText(msg);
        writeHistory(msg);
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

    private void setAuthenticated(boolean auth) {
        isAuthenticated = auth;
        menuSignIn.setDisable(auth);
        menuSignOut.setDisable(!auth);
        menuAnonymous.setDisable(!auth);
        onlineUsers.setDisable(!auth);
    }

    private void chatStatus(String status) {
        ChatMainGUI.getStage().setTitle("My Chat  —  " + status);
    }

    @Override
    public void processMessage(String msg) {
        Platform.runLater(() -> {
            MessageDTO dto = MessageDTO.convertFromJson(msg);
            switch (dto.getMessageType()) {
                case PUBLIC_MESSAGE, PRIVATE_MESSAGE -> showMessage(dto);
                case CLIENTS_LIST_MESSAGE -> refreshUserList(dto);
                case ERROR_MESSAGE -> {
                    AlertDialog.showError(dto.getBody());
                    chatStatus(dto.getBody());
                }
                case AUTH_ON_MESSAGE -> {
                    setAuthenticated(true);
                    chatArea.clear();
                    me = dto.getBody();
                    chatStatus(me);
                    localHistory(dto.getLogin());
                }
                case AUTH_OFF_MESSAGE -> {
                    setAuthenticated(false);
                    me = null;
                    chatStatus(dto.getBody());
                    history.closeLocalHistory();
                }
                case AUTH_CHANGE_NAME -> {
                    if (!dto.getBody().equals("This NICK is busy")) {
                        me = dto.getBody();
                        AlertDialog.showInform(me);
                        chatStatus(me);
                    } else {
                        AlertDialog.showError(dto.getBody());
                    }
                }
                case SERVICE_MESSAGE -> {
                    if (dto.getBody().contains("offLine")) {
                        history.closeLocalHistory();
                        if (messageService.isConnected) {
                            chatStatus(dto.getBody());
                            setAuthenticated(false);
                            me = null;
                            messageService.disconnectToServer();
                            if (dto.getFrom().equals("internal")) AlertDialog.showError(dto.getBody());
                            if (dto.getFrom().equals("server")) AlertDialog.showInform(dto.getBody());
                        }
                    } else {
                        chatStatus(dto.getBody());
                    }
                }
            }
        });
    }

    private void localHistory(String me) {
        historyFile = "ChatClient/src/main/localhistory/history_" + me + ".txt";
        history = new LocalHistory(historyFile);
        String[] arrHistory = history.readHistory(LINEHISTORY);
        for(String line : arrHistory) {
            chatArea.appendText(line + "\n");
        }
    }

    private void writeHistory(String msg) {
        history.writeHistory(msg);
    }
}