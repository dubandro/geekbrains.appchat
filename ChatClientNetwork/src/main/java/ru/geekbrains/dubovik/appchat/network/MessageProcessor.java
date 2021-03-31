package ru.geekbrains.dubovik.appchat.network;

/**
 * Абстракция для обозначения класса, который обрабатывает полученные из сети сообщения
 */

public interface MessageProcessor {

    void processMessage(String msg);
}
