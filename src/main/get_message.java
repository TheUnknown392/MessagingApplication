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

/**
 *
 * @author theunknown
 */
    /**
     * handles clients incomming messages and sends connection requests to
     * clients
     */
public class get_message implements Runnable {

//        String message;
        String username;

        Socket socket;
        BufferedReader reader = null;
        PrintWriter writer = null;
        
        boolean debug;

        public get_message(Socket socket, String peerUser, boolean debug) {
            this.debug = debug;
            if (this.debug) {
                System.out.println("ready to recieved message from: (clientHandler): " + peerUser + ":" + socket.getInetAddress().toString().substring(1));
            }

            this.socket = socket;
            this.username = peerUser;
            try {
                reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("Handler setup error: " + e.getMessage());
                cleanup();
            }
            if (debug) {
                System.out.println("Conntected client: " + username);
            }
        }

        @Override
        public void run() {
            try {
                // TODO handel through EncryptedMessage
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println(username + ": " + message);
                    if (EXIT) {
                        break;
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
                System.out.println("Connection closed for: " + username);
            }
        }

}

