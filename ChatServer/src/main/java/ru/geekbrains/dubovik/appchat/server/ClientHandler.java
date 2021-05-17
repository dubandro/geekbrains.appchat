package ru.geekbrains.dubovik.appchat.server;

import ru.geekbrains.dubovik.appchat.common.MessageDTO;
import ru.geekbrains.dubovik.appchat.common.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Обработчик клиентов. Создается сервером на каждое подключение и получает свой сокет.
 * Работает с одним сокетом/клиентом, обрабатывает отправку сообщений
 * данному конкретному клиенту и обработку сообщений поступивших от него
 */

public class ClientHandler {
    private final long AUTH_TIME = 120_000;
    private final Socket socket;
    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;
    private final ChatServer chatServer;
    private String handlerUserName;
    private Timer timer;

    public String getName() {
        return handlerUserName;
    }

    public ClientHandler(ChatServer chatServer, Socket socket) {
        try {
            this.chatServer = chatServer;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.handlerUserName = null;
            this.timer = null;
            serverMessage(MessageType.SERVICE_MESSAGE, "onLine  —  Authentication is required");
            new Thread(() -> {
                try {
                    while (!socket.isClosed()) readMessages();
                } catch (Exception e) {
                    if (!socket.isClosed()) e.printStackTrace();
                } finally {
                    if (!socket.isClosed()) {
                        System.out.println("Finally close connection");
                        CloseConnection();
                    }
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("Something wrong with ClientHandler");
        }
        timeForAuth();
    }

    /**
     * реализация задачи закрытия соединения по истечении определённого времени без аутентификации
     */
    private void timeForAuth() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (handlerUserName == null) {
                    serverMessage(MessageType.SERVICE_MESSAGE, "offLine  —  Authentication timed out");
                    CloseConnection();
                }
            }
        }, AUTH_TIME);
    }

    private void readMessages() throws IOException {
        String msg = inputStream.readUTF();
        MessageDTO dto = MessageDTO.convertFromJson(msg);
        dto.setFrom(handlerUserName);
        switch (dto.getMessageType()) {
            case PUBLIC_MESSAGE -> chatServer.broadcastMessage(dto);
            case PRIVATE_MESSAGE -> chatServer.privateMessage(dto);
            case AUTH_CHANGE_NAME -> changeUserName(dto);
            case AUTH_ON_MESSAGE -> userLogIn(dto);
            case AUTH_OFF_MESSAGE -> {
                userLogOut();
                timeForAuth(); //должен быть здесь чтоб отрабатывал только при logout и не запускался при закрытии/отключении клиента
            }
        }
    }

    private void changeUserName(MessageDTO dto) {
        if (chatServer.getAuthService().changeUserName(dto.getBody(), handlerUserName)) {
            handlerUserName = dto.getBody();
            serverMessage(MessageType.AUTH_CHANGE_NAME, handlerUserName);
            chatServer.broadcastClientsOnline();
        }
        else serverMessage(MessageType.AUTH_CHANGE_NAME, "This NICK is busy");
    }

    public void sendMessage(MessageDTO dto) {
        try {
            if (!socket.isOutputShutdown()) {
                outputStream.writeUTF(dto.convertToJson());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serverMessage(MessageType type, String msg) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(type);
        dto.setBody(msg);
        dto.setFrom("server");
        sendMessage(dto);
    }

    public void serverMessage(MessageType type, String login, String msg) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(type);
        dto.setLogin(login);
        dto.setBody(msg);
        dto.setFrom("server");
        sendMessage(dto);
    }

    private void userLogIn(MessageDTO dto) {
        String login = dto.getLogin();
        String password = dto.getPassword();
        String userName = chatServer.getAuthService().getUsernameByLoginPass(login, password);
        if (userName == null || chatServer.isNickBusy(userName)) {
            serverMessage(MessageType.ERROR_MESSAGE, "Incorrect Login or Password");
            System.out.println("Authentication error");
        } else {
            handlerUserName = userName;
            timer.cancel();
            serverMessage(MessageType.AUTH_ON_MESSAGE, login, handlerUserName);
            chatServer.subscribe(this);
        }
    }

    private void userLogOut() {
        // В обратном порядке относительно подключения
        chatServer.unsubscribe(this);
        serverMessage(MessageType.AUTH_OFF_MESSAGE, "onLine  —  Authentication is required");
        handlerUserName = null;
    }

    void CloseConnection() {
        if (handlerUserName != null) userLogOut();
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
            if (socket.isClosed()) System.out.println("Socket close, client disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}