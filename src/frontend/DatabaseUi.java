/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

import javax.swing.*;
import java.awt.*;

import database.DatabaseInfo;

/**
 *
 * @author theunknown
 */

public class DatabaseUi {

    public static DatabaseInfo showDialog() {
        JTextField hostField = new JTextField("");
        JTextField portField = new JTextField("");
        JTextField dbField = new JTextField("");
        JTextField userField = new JTextField("");
        JPasswordField passField = new JPasswordField("");

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Host:"));
        panel.add(hostField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        panel.add(new JLabel("Database:"));
        panel.add(dbField);
        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);

        int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Database Settings Required",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        return new DatabaseInfo(
                hostField.getText().trim(),
                Integer.parseInt(portField.getText().trim()),
                dbField.getText().trim(),
                userField.getText().trim(),
                new String(passField.getPassword())
        );
    }
}
