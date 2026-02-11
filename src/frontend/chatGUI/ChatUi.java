/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend.chatGUI;

import static frontend.FrameUi.x;
import static frontend.FrameUi.y;
import java.awt.BorderLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import database.Query;
import database.SenderInfo;
import database.UserInfo;
import frontend.SendMessage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 *
 * @author theunknown
 */
public class ChatUi {

    JFrame frame;
    public UserInfo user;
    private Query query = new Query(false);

    public ContactUi contactUi = null;
    public MessageDisplay messageDisplay = null;
    SendMessage sendmessage = new SendMessage();

    public JTextField messageField = null;

    public JScrollPane messageScroll = null;

    public JPanel inputPanel = null;

    public JButton sendMessage = null;
    
    public JButton addContacts = null;

    public JSplitPane splitPane = null;

    private SenderInfo selectedSender = null;
    
    private OnClick onClickListener = null;
    
    private void setProperties(UserInfo user, JFrame parentFrame){
        this.frame = parentFrame;
        this.user = user;
        this.contactUi       = new ContactUi(this.query, this.user, this);
        this.sendMessage     = new JButton("Send");
        this.addContacts     = new JButton("Add Contacts");
        this.messageDisplay  = new MessageDisplay();
        this.messageField    = new JTextField();
        this.inputPanel      = new JPanel(new BorderLayout());
        this.messageScroll   = new JScrollPane(messageDisplay.showMessage);
        this.onClickListener = new OnClick();
        this.splitPane       = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    }

    public ChatUi(UserInfo user, JFrame parentFrame) {
        assert user != null;
        
        
        setProperties(user, parentFrame);
        
        // listeners
        sendMessage.setActionCommand("Send_Message");
        addContacts.setActionCommand("Add_Contacts");
        sendMessage.addActionListener(onClickListener);
        addContacts.addActionListener(onClickListener);
        contactUi.contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                if (true) {
                    if(contactUi.getSelectedSender()!= null){
                        System.out.println("selected: " + contactUi.getSelectedSender().username);
                    }
                }
                this.selectedSender = contactUi.getSelectedSender();
                SwingUtilities.invokeLater(() -> {
                    messageDisplay.showHistory(selectedSender.getFingerprint());
                });
            }
        });

        // inputPane
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendMessage, BorderLayout.EAST);
        // contactPane
        JPanel contactPanel = new JPanel(new BorderLayout());
        JLabel contactTitle = new JLabel("Contacts");
        contactPanel.add(contactUi.contactScroll, BorderLayout.CENTER);
        contactPanel.add(contactTitle, BorderLayout.NORTH);
        contactPanel.add(addContacts,BorderLayout.SOUTH);
        // messagePane
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(new JLabel("Message"), BorderLayout.NORTH);
        messagePanel.add(messageScroll, BorderLayout.CENTER);
        messagePanel.add(inputPanel, BorderLayout.SOUTH);
        // splipPane
        splitPane.setDividerLocation(150);
        splitPane.setLeftComponent(contactPanel);
        splitPane.setRightComponent(messagePanel);
       
        contactUi.contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        

        contactUi.loadContacts();
    }
 
    private class OnClick implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String action = e.getActionCommand();
            switch (action) {
                case "Send_Message": {
                    String unsafeMessage = messageField.getText().trim();
                    System.out.println(unsafeMessage);
                    if (unsafeMessage.equals("")) {
                        return;
                    }
                    if (contactUi.getSelectedSender() == null) {
                        System.out.println("it's null getSelectedSender()");
                        return;
                    }

                    sendmessage.sendMessage(user.id, selectedSender, unsafeMessage);
                    System.out.println("The selected sender in actionPerformed: " + selectedSender.getFingerprint());
                    messageDisplay.appendMessage(selectedSender.getFingerprint(),"You: " + unsafeMessage);
                    messageDisplay.showHistory(selectedSender.getFingerprint());
                    messageField.setText("");
                    contactUi.messageRead(selectedSender);
                }
                break;
                case "Add_Contacts": {
                    System.out.println("TODO: Adding new contacts through GUI. Incomplete");
                    new NewContactUi(user,contactUi);
                    contactUi.loadContacts();
                }
                break;
                default: {
                    System.err.println("unreachable in ChatUi onClick");
                }
            }
        }
    }
}
