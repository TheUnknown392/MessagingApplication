/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

import crypto.CryptoMessage;
import database.Query;
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
//CryptoMessage crypto = new CryptoMessage();
    public boolean sendMessage(int uid,SenderInfo sender, String message) {
        // TODO: properly handel the END_MESSAGE
        final String END_MESSAGE = ":END_OF_MESSAGE:";

        message.replace("\n", "//n");

        System.out.println("send_message: " + sender);

        if (!clients.containsKey(sender.getFingerprint())) {
            System.err.println("User does not exist");
            return true;
        }

        Socket activeSocket = clients.get(sender.getFingerprint());
        
        Query query = new Query(false);
        byte[] aes_sender = query.relatedSenderAES(uid, sender.getId());
        query.closeConnection();
        String encryptedMessage = CryptoMessage.encryptMessage(message, aes_sender);

        try {
            PrintWriter writer = new PrintWriter(activeSocket.getOutputStream(), true);
//            EncryptedMessage crypto.encryptMessage(message, );
            writer.println(encryptedMessage);
            writer.println(END_MESSAGE);
        } catch (IOException e) {
            System.err.println("Could not create a new reader and writer to send message: " + e.getMessage());
        }
        return false;
    }
    

}
