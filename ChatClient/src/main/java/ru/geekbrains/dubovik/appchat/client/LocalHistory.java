package ru.geekbrains.dubovik.appchat.client;

import java.io.*;
import java.util.LinkedList;

public class LocalHistory {
    private BufferedWriter writer;
    private BufferedReader reader;
    private FileWriter fileWriter;
    private FileReader fileReader;

    public LocalHistory(String file) {
        try {
            fileWriter = new FileWriter(file, true);
            writer = new BufferedWriter(fileWriter);
            fileReader = new FileReader(file);
            reader = new BufferedReader(fileReader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeHistory(String msg) {
        try {
            writer.write(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] readHistory(int numLines) {
        LinkedList<String> listHistory = new LinkedList<>();
        try {
            String line;
            int counter = 1;
            while ((line = reader.readLine()) != null) {
                if (line.equals("")) counter++;
                if (counter > numLines) {
                    listHistory.removeFirst();
                }
                listHistory.addLast(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return listHistory.toArray(new String[listHistory.size()]);
    }

    public void closeLocalHistory() {
        try {
            writer.close();
            reader.close();
            fileWriter.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
