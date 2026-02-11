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
    private String currentHistory = null;
    public MessageDisplay() {
        showMessage.setEditable(false);
    }

    public void appendMessage(String md5, String msg) {
        messageHistories.putIfAbsent(md5, new StringBuilder());
        messageHistories.get(md5).append(msg).append("\n");
        showMessage.setCaretPosition(showMessage.getDocument().getLength());
    }

    public void showHistory(String md5) {
        currentHistory = md5;
        messageHistories.putIfAbsent(md5, new StringBuilder());
        showMessage.setText(messageHistories.get(md5).toString());
        showMessage.setCaretPosition(showMessage.getDocument().getLength());
    }
    public void updateIfSelected(String md5){
        if(currentHistory == null) return;
        if(currentHistory.equals(md5)){
            showHistory(md5);
        }
    }
    public void deleteHistory(String md5){
        messageHistories.put(md5, new StringBuilder(""));
    }
}
