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
    private final long AUTH_TIME = 30_000;
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

    private void timeForAuth() {
        timer = new Timer();
        timer.schedule(new AuthTimeIsUp(this), AUTH_TIME);
    }

    private void readMessages() throws IOException {
        String msg = inputStream.readUTF();
        MessageDTO dto = MessageDTO.convertFromJson(msg);
        dto.setFrom(handlerUserName);
        switch (dto.getMessageType()) {
            case PUBLIC_MESSAGE -> chatServer.broadcastMessage(dto);
            case PRIVATE_MESSAGE -> chatServer.privateMessage(dto);
            case AUTH_ON_MESSAGE -> userLogIn(dto);
            case AUTH_OFF_MESSAGE -> {
                userLogOut();
                timeForAuth(); //должен быть здесь чтоб отрабатывал только при logout и не запускался при закрытии/отключении клиента
            }
        }
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

    private void userLogIn(MessageDTO dto) {
        String userName = chatServer.getAuthService().getUsernameByLoginPass(dto.getLogin(), dto.getPassword());
        if (userName == null || chatServer.isNickBusy(userName)) {
            serverMessage(MessageType.ERROR_MESSAGE, "Incorrect Login or Password");
            System.out.println("Authentication error");
        } else {
            handlerUserName = userName;
            timer.cancel();
            serverMessage(MessageType.AUTH_ON_MESSAGE, handlerUserName);
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

/**
 * реализация задачи закрытия соединения по истечении определённого времени
 * метод timeForAuth создаёт объект TimerTask, который отрабатывает закрытие соединения
 */
class AuthTimeIsUp extends TimerTask {
    ClientHandler client;

    public AuthTimeIsUp(ClientHandler client) {
        this.client = client;
    }

    @Override
    public void run() {
        if (client.getName() == null) {
            client.serverMessage(MessageType.SERVICE_MESSAGE, "offLine  —  Authentication timed out");
            client.CloseConnection();
        }
    }
}