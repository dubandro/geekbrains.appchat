package ru.geekbrains.dubovik.appchat.server;

import ru.geekbrains.dubovik.appchat.common.MessageDTO;
import ru.geekbrains.dubovik.appchat.common.MessageType;
import ru.geekbrains.dubovik.appchat.server.authentication.AuthService;
import ru.geekbrains.dubovik.appchat.server.authentication.BaseAuthService;
import ru.geekbrains.dubovik.appchat.server.authentication.JDBCAuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Сервер - создает ServerSocket и слушает указанный порт,
 * при подключении кого-либо, создает ClientHandler и отдает работу ему
 */

public class ChatServer {
    private static final int PORT = 65500;
    private List<ClientHandler> clientHandlerList;
    private AuthService authService;
    private ExecutorService executorService;

    public AuthService getAuthService() {
        return authService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ChatServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server started");
            /**
             * Java. Уровень 3. Урок 2. Задание 1.
             * Добавить в сетевой чат авторизацию через базу данных SQLite
             *
             * Прежний сервис авторизации -
             * authService = new BaseAuthService();
             */
            authService = new JDBCAuthService();
            authService.start();
            clientHandlerList = new LinkedList<>();
            /**
             * Java. Уровень 3. Урок 4. Задание 2.
             * На серверной стороне сетевого чата реализовать управление потоками через ExecutorService.
             */
            executorService = Executors.newCachedThreadPool();
            while (true) {
                System.out.println("Waiting new connection...");
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            System.out.println("Server error");
            e.printStackTrace();
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public synchronized boolean isNickBusy(String nick) {
        for (ClientHandler client : clientHandlerList) {
            if (client.getName().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastClientsOnline() {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(MessageType.CLIENTS_LIST_MESSAGE);
        List<String> usersOnLine = new LinkedList<>();
        for (ClientHandler client : clientHandlerList) {
            usersOnLine.add(client.getName());
        }
        dto.setUsersOnline(usersOnLine);
        broadcastMessage(dto);
    }

    public synchronized void broadcastMessage(MessageDTO dto) {
        for (ClientHandler client : clientHandlerList) {
            client.sendMessage(dto);
        }
    }

    public synchronized void privateMessage(MessageDTO dto) {
        for (ClientHandler client : clientHandlerList) {
            if (client.getName().equals(dto.getTo())) {
                client.sendMessage(dto);
                break;
            }
        }
    }

    public synchronized void subscribe(ClientHandler client) {
        clientHandlerList.add(client);
        broadcastClientsOnline();
        System.out.println(client.getName() + " subscribed");
    }

    public synchronized void unsubscribe(ClientHandler client) {
        clientHandlerList.remove(client);
        broadcastClientsOnline();
        System.out.println(client.getName() + " unsubscribed");
    }
}