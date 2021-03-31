package ru.geekbrains.dubovik.appchat.client;

/**
 * Этот метод запускает приложение на JavaFX и нужен только для этого.
 * Из класса Application работает только в java 8 или
 * придётся добавить vm-options - https://www.jetbrains.com/help/idea/javafx.html#vm-options
 */

public class ClientApp {
    public static void main(String[] args) {
        ChatMainGUI.main(args);
    }
}