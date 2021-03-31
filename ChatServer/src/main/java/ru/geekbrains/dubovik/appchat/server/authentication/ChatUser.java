package ru.geekbrains.dubovik.appchat.server.authentication;

/**
 * Сущность клиента для идентификации и авторизации
 */

public class ChatUser {
    private String username;
    private String login;
    private String password;

    public ChatUser(String username, String login, String password) {
        this.username = username;
        this.login = login;
        this.password = password;
    }

    public String getUserName() {
        return username;
    }

    public void setUserName(String username) {
        this.username = username;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
