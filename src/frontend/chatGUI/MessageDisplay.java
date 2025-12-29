/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend.chatGUI;


import java.util.HashMap;
import javax.swing.JTextArea;


/**
 *
 * @author theunknown
 */
public class MessageDisplay {
    private HashMap<String, StringBuilder> messageHistories = new HashMap<>();
    public JTextArea showMessage = new JTextArea();

    public MessageDisplay() {
        showMessage.setEditable(false);
    }

    public void appendMessage(String md5, String msg) {
        messageHistories.putIfAbsent(md5, new StringBuilder());
        messageHistories.get(md5).append(msg).append("\n");
        showMessage.setCaretPosition(showMessage.getDocument().getLength());
    }

    public void showHistory(String md5) {
        messageHistories.putIfAbsent(md5, new StringBuilder());
        showMessage.setText(messageHistories.get(md5).toString());
        showMessage.setCaretPosition(showMessage.getDocument().getLength());
    }
}
