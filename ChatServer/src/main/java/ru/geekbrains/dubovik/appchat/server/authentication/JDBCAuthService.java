package ru.geekbrains.dubovik.appchat.server.authentication;

import java.sql.*;

public class JDBCAuthService implements AuthService{
    private static Connection connection;
    private static Statement statement;

    @Override
    public void start() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:ChatServer/src/main/resources/authbase.db");
            statement = connection.createStatement();
            // Сервер напечатает зарегистрированных пользователей
            registeredUsers();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void registeredUsers() throws SQLException {
        System.out.println("NameUser Log Pass");
        ResultSet res = statement.executeQuery("select * from auth");
        while (res.next()){
            System.out.println(res.getString("name") + "  " + res.getString("log") + "  " + res.getString("pass"));
        }
    }

    @Override
    public void stop() {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Data base connection close");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getUsernameByLoginPass(String login, String pass) {
        String userName = null;
        try {
            PreparedStatement ps = connection.prepareStatement("select * from auth where log = ?;");
            ps.setString(1, login);
            ResultSet result = ps.executeQuery();
            if (result.getString("pass").equals(pass)) userName = result.getString("name");
            System.out.println(userName + " authenticated");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return userName;
    }

    @Override
    public boolean changeUserName (String newName, String oldName) {
        boolean changeSuccess = false;
        // проверить допустимость Ника - валидные символы итд
        // проверить не занят ли выбранный Ник
        // "@nonymous" могут стать все - сделано для проверки, в будущем можно убрать
        if ((isNotBusyName(newName) && isValidName(newName)) || newName.equals("@nonymous")) {
            try {
                PreparedStatement ps = connection.prepareStatement("update auth set name = ? where name = ?;");
                ps.setString(1, newName);
                ps.setString(2, oldName);
                if (ps.executeUpdate() != 0) changeSuccess = true;
                System.out.printf("User %s changed name to %s\n", oldName, newName);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return changeSuccess;
    }

    private boolean isValidName(String newName) {
        return true;
    }

    private boolean isNotBusyName(String newName) {
        try {
            ResultSet res = statement.executeQuery("select * from auth");
            while (res.next()) {
                if (res.getString("name").equals(newName)) return false;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return true;
    }
}