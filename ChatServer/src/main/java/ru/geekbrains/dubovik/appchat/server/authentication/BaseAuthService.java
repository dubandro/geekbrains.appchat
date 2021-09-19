package ru.geekbrains.dubovik.appchat.server.authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Временный сервис авторизации с хардкодом юзеров
 */

public class BaseAuthService implements AuthService{
    private List<ChatUser> clientList;

    public BaseAuthService() {
        clientList = new ArrayList<>(Arrays.asList(
                new ChatUser("1st@User", "1", "q"),
                new ChatUser("2nd@User", "2", "w"),
                new ChatUser("3rd@User", "3", "e"),
                new ChatUser("4th@User", "4", "r"),
                new ChatUser("5th@User", "5", "t"),
                new ChatUser("6th@User", "6", "y")
        ));
    }

    @Override
    public void start() {
        System.out.println("Auth started");
    }

    @Override
    public void stop() {
        System.out.println("Auth stopped");
    }

    @Override
    public String getUsernameByLoginPass(String login, String pass) {
        for (ChatUser client : clientList) {
            if (client.getLogin().equals(login) && client.getPassword().equals(pass)) return client.getUserName();
        }
        return null;
    }

    @Override
    public boolean changeUserName(String newName, String oldName) {
        return false;
    }
}
