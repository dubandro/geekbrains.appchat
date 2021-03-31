package ru.geekbrains.dubovik.appchat.network;

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

    public NetworkService(String address, int port, MessageService messageService) {

        try {
            this.socket = new Socket(address, port);
        } catch (IOException e) {
//            AlertDialog.showError("Error: failed to connect, server not responding");
            e.printStackTrace();
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
                    if (messageService.isConnected) {
                        System.out.println("Finally close connection");
                        closeConnection();
                        messageService.isConnected = false;
                    }
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
}