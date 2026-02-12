/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

import database.UserInfo;
import frontend.chatGUI.ChatUi;
import crypto.CryptoMessage;
import database.Query;
import database.SenderInfo;
import database.UserInfo;
import main.Message;

import javax.swing.SwingUtilities;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/**
 *
 * @author theunknown
 */

public class MessageHistoryLoader {

    private final ChatUi chatUi;
    private final UserInfo user;

    public MessageHistoryLoader(ChatUi chatUi, UserInfo user) {
        this.chatUi = chatUi;
        this.user = user;
    }

    public void loadHistory() {
        Query query = new Query(false);
        Map<String, List<Message>> conversationMap = query.getUserMessages(user);
        query.closeConnection();

        SwingUtilities.invokeLater(() -> {

            conversationMap.forEach((fingerprint, messages) -> {
                if (messages.isEmpty()) {
                    return;
                }
                SenderInfo sender = messages.get(0).getSenderInfo();

                // Add contact if missing
                if (!chatUi.contactUi.contactListModel.contains(sender)) {
                    chatUi.contactUi.contactListModel.addElement(sender);
                }

                // Append message history
                for (Message msg : messages) {
                    boolean incoming = msg.sentByUser(); 

                    chatUi.messageDisplay.appendMessage(
                        sender.getFingerprint(),
                        (incoming ? "You: " : sender.nickname + ": ") + msg.getMessage()
                    );
                }
            });
        });
    }
}
