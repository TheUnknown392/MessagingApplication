/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import static main.Main.EXIT;
import static main.Main.clients;
import static main.Main.messages;
import database.SenderInfo;

/**
 *
 * @author theunknown
 */
/**
 * handles clients incomming messages and sends connection requests to clients
 */
public class get_message implements Runnable {
    
    private final String END_MESSAGE = ":END_OF_MESSAGE:";
    private SenderInfo sender;

    private Socket socket;
    private BufferedReader reader = null;
    private PrintWriter writer = null;

    boolean debug;

    public get_message(Socket socket, SenderInfo sender, boolean debug) {
        this.debug = debug;
        if (this.debug) {
            System.out.println("ready to recieved message from: (clientHandler): " + sender.getFingerpring() + ":" + socket.getInetAddress().toString().substring(1));
        }
        this.sender = sender;
        this.socket = socket;
        try {
            reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Handler setup error: " + e.getMessage());
            cleanup();
        }
        if (debug) {
            System.out.println("Conntected client: " + sender.getUsername());
        }
        /* debug */System.out.println(sender.username);
    }

    @Override
    public void run() {
        try {
            StringBuilder fmessage = new StringBuilder();
            // TODO handel through EncryptedMessage
            String encryptedMessage;
            while ((encryptedMessage = reader.readLine()) != null) {
                if (EXIT) {
                    break;
                }
                
                if(encryptedMessage.equals(END_MESSAGE)){
                    String fullMessage = fmessage.toString();
                    messages.add(new Message(this.sender,fullMessage.trim()));
                    fmessage.setLength(0);
                    continue;
                }
                
                if(!encryptedMessage.isEmpty()){
                    fmessage.append(encryptedMessage+"\n");
                    System.out.println("appended: " + encryptedMessage);
                }
            }
        } catch (IOException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ignored) {
        }

        if (writer != null) {
            writer.close();
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

        // remove this socket from clients list map thingie
        clients.entrySet().removeIf(e -> e.getValue() == this.socket);
        if (!debug) {
            System.out.println("Connection closed for: " + sender.username);
        }
    }

}
