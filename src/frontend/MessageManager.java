/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this ilcense
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

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
        if(query.saveIncommingEncryptedMessage(message, user)){
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
                    SwingUtilities.invokeLater(() -> {
                        frame.chatUi.showMessage.append(message.getSenderInfo().username + ": " + message.getEncryptedMessage().replace("//n", "\n") + "\n");
                        frame.chatUi.showMessage.setCaretPosition(frame.chatUi.showMessage.getDocument().getLength());
                    });
                    // System.out.println(message.getSenderInfo().username + ": " + message.getEncryptedMessage().replace("//n","\n"));}}
                }
            }
        
        }
    }
}
