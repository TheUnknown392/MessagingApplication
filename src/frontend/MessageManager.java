/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this ilcense
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

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

    public MessageManager(FrameUi frame) {
        this.frame = frame;
        System.out.println("from constructor " + messages.size());
    }

    @Override
    public void run() {
        while (true) {
            if (!messages.isEmpty()) {
                Message message = messages.poll();
                if (message == null) {
                    System.out.println("it's null");
                } else {
                    SwingUtilities.invokeLater(() -> {
                        frame.chatUi.showMessage.setText(message.getSenderInfo().username + ": " + message.getEncryptedMessage().replace("//n", "\n"));
                    });
                    // System.out.println(message.getSenderInfo().username + ": " + message.getEncryptedMessage().replace("//n","\n"));}}
                }
            }
        
        }
    }
}
