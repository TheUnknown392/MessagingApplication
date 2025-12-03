/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;


/**
 *
 * @author theunknown
 */
public class ChatUi {
    UserInfo user;
    private Query query = new Query(false);

    protected JTextArea showMessage = null;
    protected JTextField messageField = null;
    
    protected JScrollPane contactScroll = null;
    protected JScrollPane messageScroll = null;
    
    protected JPanel inputPanel = null;
    
    protected JButton sendMessage = null;
    
    protected JSplitPane splitPane = null;
    
    protected DefaultListModel<SenderInfo> contactListModel = null;
    protected JList<SenderInfo> contactList = null;

    public ChatUi(UserInfo user, JFrame parentFrame){
        assert user!=null;
        
        this.user = user;
        
        messageField = new JTextField();

        sendMessage = new JButton("Send");
        sendMessage.setActionCommand("Send_Message");
        showMessage = new JTextArea();
        inputPanel = new JPanel(new BorderLayout());
        
        contactListModel = new DefaultListModel<>();
        contactList = new JList(contactListModel);
        contactScroll = new JScrollPane(contactList);
        
        showMessage.setEditable(false);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendMessage, BorderLayout.EAST);


        JPanel contactPanel = new JPanel(new BorderLayout());
        contactPanel.add(contactScroll, BorderLayout.CENTER);
        JLabel contactTitle = new JLabel("Contacts");
        contactPanel.add(contactTitle, BorderLayout.NORTH);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messageScroll = new JScrollPane(showMessage);
        messagePanel.add(messageScroll, BorderLayout.CENTER);
        messagePanel.add(new JLabel("Message"), BorderLayout.NORTH);
        messagePanel.add(inputPanel, BorderLayout.SOUTH);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(150);
        splitPane.setLeftComponent(contactPanel);
        splitPane.setRightComponent(messagePanel);
        
        contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sendMessage.addActionListener(new onClick());
        
        loadContacts();
    }
    
    private class onClick implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            String action = e.getActionCommand();
            switch(action){
                case "Send_Message":{
                    String unsafeMessage = messageField.getText().trim();
                    System.out.println(unsafeMessage);
                    if(unsafeMessage.equals("")){
                       return; 
                    }
                    if(getSelectedSender()==null){
                        System.out.println("it's null getSelectedSender()");
                        return;
                    }
                    
                }break;
                default:{
                    System.err.println("unreachable in ChatUi onClick");
                }
            }
        }
    }
    
    protected void loadContacts(){
        List<SenderInfo> contacts = new ArrayList<>(this.query.getContacts(this.user));
        contacts.forEach((e)->{
            contactListModel.addElement(e);
        });
        contactList.setSelectedIndex(0);
    }
    
    public SenderInfo getSelectedSender(){
        return contactList.getSelectedValue();
    }
}
