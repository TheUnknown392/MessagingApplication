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

    public JSplitPane splitPane = null;

    private SenderInfo selectedSender = null;

    public ChatUi(UserInfo user, JFrame parentFrame) {
        assert user != null;
        this.frame = parentFrame;

        this.user = user;

        contactUi = new ContactUi(this.query, this.user);
        messageDisplay = new MessageDisplay();

        sendMessage = new JButton("Send");
        sendMessage.setActionCommand("Send_Message");
        contactUi.contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                if (true) {
                    System.out.println("selected: " + contactUi.getSelectedSender().username);
                }
                this.selectedSender = contactUi.getSelectedSender();
                SwingUtilities.invokeLater(() -> {
                    messageDisplay.showHistory(selectedSender.getFingerprint());
                });
            }
        });

        messageField = new JTextField();
        inputPanel = new JPanel(new BorderLayout());

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendMessage, BorderLayout.EAST);

        JPanel contactPanel = new JPanel(new BorderLayout());
        contactPanel.add(contactUi.contactScroll, BorderLayout.CENTER);
        JLabel contactTitle = new JLabel("Contacts");
        contactPanel.add(contactTitle, BorderLayout.NORTH);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messageScroll = new JScrollPane(messageDisplay.showMessage);
        messagePanel.add(messageScroll, BorderLayout.CENTER);
        messagePanel.add(new JLabel("Message"), BorderLayout.NORTH);
        messagePanel.add(inputPanel, BorderLayout.SOUTH);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(150);
        splitPane.setLeftComponent(contactPanel);
        splitPane.setRightComponent(messagePanel);

        contactUi.contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sendMessage.addActionListener(new onClick());

        contactUi.loadContacts();
    }

    private class onClick implements ActionListener {

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
                }
                break;
                default: {
                    System.err.println("unreachable in ChatUi onClick");
                }
            }
        }
    }
}
