package ru.geekbrains.dubovik.appchat.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ChatMainGUI extends Application {
    private static Stage ourStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        ourStage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("/chatclient.fxml"));
        primaryStage.getIcons().add(new Image(ChatMainGUI.class.getResourceAsStream("/mails.png")));
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static Stage getStage() {
        return ourStage;
    }
}