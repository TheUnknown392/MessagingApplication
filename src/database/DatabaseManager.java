package database;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.io.*;
import java.awt.Component;
import java.util.Properties;

import database.DatabaseInfo;
import frontend.DatabaseUi;
/**
 *
 * @author theunknown
 */
public class DatabaseManager {

    private static final File CONFIG_FILE =
            new File(System.getProperty("user.home"), ".messagingApplication.db");

    public static DatabaseInfo loadOrAsk() {
        if (CONFIG_FILE.exists()) {
            return loadFromFile();
        }

        DatabaseInfo config = DatabaseUi.showDialog();
        if (config != null) {
            saveToFile(config);
        }
        return config;
    }

    private static DatabaseInfo loadFromFile() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            return new DatabaseInfo(
                    props.getProperty("host"),
                    Integer.parseInt(props.getProperty("port")),
                    props.getProperty("database"),
                    props.getProperty("username"),
                    props.getProperty("password")
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load DB config", e);
        }
    }

    public static void saveToFile(DatabaseInfo config) {
        Properties props = new Properties();
        props.setProperty("host", config.getHost());
        props.setProperty("port", String.valueOf(config.getPort()));
        props.setProperty("database", config.getDatabase());
        props.setProperty("username", config.getUsername());
        props.setProperty("password", config.getPassword());

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Messaging Application Database Settings");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save DB config", e);
        }
    }
}
