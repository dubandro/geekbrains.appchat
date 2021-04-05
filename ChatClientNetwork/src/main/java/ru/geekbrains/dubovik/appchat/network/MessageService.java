package ru.geekbrains.dubovik.appchat.network;

/**
 * Сервис работы с сообщениями по сети. Обрабатывает получение и отправку сообщений
 */

public class MessageService {
    private final String host;
    private final int port;
    public NetworkService networkService;
    private final MessageProcessor processor;
    public boolean isConnected;

    public MessageService(String host, int port, MessageProcessor processor) {
        this.host = host;
        this.port = port;
        this.processor = processor;
        connectToServer();
    }

    public void connectToServer() {
        this.networkService = new NetworkService(host, port, this);
    }

    public void disconnectToServer() {
        isConnected = false;
        networkService.closeConnection();
    }

    public void sendMessage(String msg) {
        networkService.writeMessage(msg);
    }

    public void receiveMessage(String msg) {
        processor.processMessage(msg);
    }
}