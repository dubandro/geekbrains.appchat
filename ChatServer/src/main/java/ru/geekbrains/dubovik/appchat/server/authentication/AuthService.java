package ru.geekbrains.dubovik.appchat.server.authentication;

/**
 * Абстракция сервиса авторизаций
 */

public interface AuthService {
    void start();
    void stop();
    String getUsernameByLoginPass(String login, String pass);
}
