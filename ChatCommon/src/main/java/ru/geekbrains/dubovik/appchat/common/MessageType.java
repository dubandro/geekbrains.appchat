package ru.geekbrains.dubovik.appchat.common;

/**
 * Перечисление типов сообщений чтоб было их удобно категоризировать и обрабатывать
 */

public enum MessageType {
    PUBLIC_MESSAGE,
    PRIVATE_MESSAGE,
    ERROR_MESSAGE,
    SERVICE_MESSAGE,
    AUTH_ON_MESSAGE,
    AUTH_OFF_MESSAGE,
    CLIENTS_LIST_MESSAGE
}