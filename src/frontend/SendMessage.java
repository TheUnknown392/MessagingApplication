/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

import database.SenderInfo;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import main.ConnectionKey;
import static main.Main.clients;

/**
 *
 * @author theunknown
 */
public class SendMessage {

    public static boolean sendMessage(String userkey, String message) {
        ConnectionKey connectionKey = new ConnectionKey(userkey, null);
        // TODO: properly handel the END_MESSAGE
        final String END_MESSAGE = ":END_OF_MESSAGE:";

        message.replace("\n", "//n");

        System.out.println("send_message: " + connectionKey);

        if (!clients.containsKey(connectionKey.toString())) {
            System.err.println("User does not exist");
            return true;
        }

        Socket activeSocket = clients.get(connectionKey.toString());

        try {
            PrintWriter writer = new PrintWriter(activeSocket.getOutputStream(), true);
            writer.println(message);
            writer.println(END_MESSAGE);
        } catch (IOException e) {
            System.err.println("Could not create a new reader and writer to send message: " + e.getMessage());
        }
        return false;
    }
}
