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
import database.UserInfo;
import java.util.ArrayList;
import java.util.List;

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
    protected JPanel statusPanel = null;
    
    protected JButton sendMessage = null;
    
    protected JSplitPane splitPane = null;
    
    protected DefaultListModel<String> contactListModel = null;
    protected JList<String> contactList = null;

    public ChatUi(UserInfo user) {
        assert user!=null;
        
        this.user = user;
        
        messageField = new JTextField();

        sendMessage = new JButton("Send");
        showMessage = new JTextArea();
        inputPanel = new JPanel(new BorderLayout());
        statusPanel = new JPanel(new BorderLayout());
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
        
        statusPanel.add(new JLabel("status"), BorderLayout.EAST);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(150);
        splitPane.setLeftComponent(contactPanel);
        splitPane.setRightComponent(messagePanel);
        loadContacts();
    }
    
    protected void loadContacts(){
        List<String> contacts = new ArrayList<>(this.query.getContacts(this.user));
        contacts.forEach((e)->{
            contactListModel.addElement(e);
        });
    }
}
