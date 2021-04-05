package ru.geekbrains.dubovik.appchat.network;

import ru.geekbrains.dubovik.appchat.common.MessageDTO;
import ru.geekbrains.dubovik.appchat.common.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Сетевой сервис. Подключается к серверу, пишет сообщения, в потоке читает входящие передавая в MessageService
 */

public class NetworkService {
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Thread clientThread;
    private final MessageService messageService;

    public NetworkService(String address, int port, MessageService messageService) {
        this.messageService = messageService;
        try {
            this.socket = new Socket(address, port);
        } catch (IOException e) {
            internalMessage(MessageType.ERROR_MESSAGE, "Failed to connect. Server not responding");
        }

        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            messageService.isConnected = true;
            System.out.println("Client connected");
            try {
                this.inputStream = new DataInputStream(socket.getInputStream());
                this.outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            clientThread = new Thread(() -> {
                try {
                    while (messageService.isConnected && !clientThread.isInterrupted() && !socket.isClosed()) {
                        String msg = inputStream.readUTF();
                        messageService.receiveMessage(msg);
                    }
                } catch (IOException e) {
                    if (messageService.isConnected) {
                        e.printStackTrace();
                    }
                } finally {
                    internalMessage(MessageType.SERVICE_MESSAGE, "offLine  —  Server not responding\n\n" +
                            "Try connecting again later...");
                }
            });
            clientThread.setDaemon(true);
            clientThread.start();
        }
    }

    public void closeConnection() {
        if (!socket.isClosed()) {
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
            clientThread.interrupt();
        }
    }

    public void writeMessage(String msg) {
        try {
            outputStream.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void internalMessage(MessageType type, String msg) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(type);
        dto.setBody(msg);
        dto.setFrom("internal");
        messageService.receiveMessage(dto.convertToJson());
    }
}