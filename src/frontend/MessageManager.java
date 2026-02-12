/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this ilcense
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

import crypto.CryptoMessage;
import database.Query;
import database.UserInfo;
import javax.swing.SwingUtilities;
import main.Message;
import static main.Main.messages;
/**
 *
 * @author theunknown
 */
public class MessageManager implements Runnable {
//    private ConcurrentLinkedQueue<Message> messages;

    FrameUi frame = null;
    UserInfo user = null;
    private Query query = new Query(false);

    public MessageManager(FrameUi frame, UserInfo user) {
        this.user = user;
        this.frame = frame;
        System.out.println("from constructor " + messages.size());
    }
    
    protected boolean saveRecievedMessage(Message message){
        if(query.saveIncomingEncryptedMessage(message, user)){
            System.err.println("couldn't save. saveRecievedMessage, MessageManager");
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        while (true) {
            if (!messages.isEmpty()) {
                Message message = messages.poll();
                saveRecievedMessage(message);
                if (message == null) {
                    System.out.println("it's null");
                } else {
                    byte[] aesKeyBytes = query.relatedUserAES(user.getId(), message.getSenderInfo().getId());
                    System.out.println("aesKeyBytes(MessageManager): " + aesKeyBytes);
                    String decrypted= CryptoMessage.decryptMessage(message.getMessage(), aesKeyBytes);
                    SwingUtilities.invokeLater(() -> {
                        System.out.println("The selected sender in MessageManager: " + message.getSenderInfo().getFingerprint());
                        if(!frame.chatUi.selectedSender.getFingerprint().equals(message.getSenderInfo().getFingerprint())){
                            frame.chatUi.contactUi.messageIncomming(message.getSenderInfo());
                        }
                        frame.chatUi.messageDisplay.appendMessage(message.getSenderInfo().getFingerprint(),message.getSenderInfo().username +": "+decrypted.replace("//n", "\n"));
                        frame.chatUi.messageDisplay.updateIfSelected(message.getSenderInfo().getFingerprint());
//                        statusUi.chatUi.messageDisplay.showHistory(message.getSenderInfo().getFingerprint());
                    });
                    // System.out.println(message.getSenderInfo().username + ": " + message.getMessage().replace("//n","\n"));}}
                }
            }
        
        }
    }
}
