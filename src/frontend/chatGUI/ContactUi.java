/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend.chatGUI;

import database.Query;
import database.SenderInfo;
import database.UserInfo;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author theunknown
 */
public class ContactUi {

    protected JScrollPane contactScroll = null;
    protected DefaultListModel<SenderInfo> contactListModel = null;
    protected JList<SenderInfo> contactList = null;
    protected JPopupMenu popupMenu = new JPopupMenu();

    private Query query = null;
    private UserInfo user = null;
    private SenderInfo sender = null;

    public ContactUi(Query query, UserInfo user) {
        contactListModel = new DefaultListModel<>();
        contactList = new JList(contactListModel);
        contactScroll = new JScrollPane(contactList);
        this.query = query;
        this.user = user;
        installPopupMenu();
    }

    public void loadContacts() {
        Runnable uiUpdate = () -> {
            contactListModel.clear();

            List<SenderInfo> contacts = query.getContacts(user);
            for (SenderInfo s : contacts) {
                contactListModel.addElement(s);
            }

            if (!contacts.isEmpty()) {
                contactList.setSelectedIndex(0);
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            uiUpdate.run();
        } else {
            SwingUtilities.invokeLater(uiUpdate);
        }
    }

    public SenderInfo getSelectedSender() {
        SenderInfo senderInfo = contactList.getSelectedValue();
        if (senderInfo == null) {
            System.out.println("getSelectedSender is null");
        }
        return senderInfo;
    }

    private void installPopupMenu() {

        JMenuItem renameChat = new JMenuItem("Rename");
        JMenuItem deleteChat = new JMenuItem("Delete conversation");

        popupMenu.add(renameChat);
        popupMenu.add(deleteChat);

        // Action listener for Rename
        renameChat.addActionListener(ev -> {
            if (sender == null) {
                return;
            }

            String nickname = JOptionPane.showInputDialog(popupMenu,"Username:",sender.username);

            if (nickname != null && !nickname.trim().isEmpty()) {
                System.out.println("Trying to change username of " + sender.username + " to " + nickname);

                Query temp = new Query(false);
                temp.changeUsername(user, sender, nickname);
                temp.closeConnection();

                loadContacts();
            }
        });
        deleteChat.addActionListener(ev -> {
            if (sender == null) {
                return;
            }
            // TODO: System.out.println("Delete chat with: " + sender.getUsername());
        });
        
        
        
        
        contactList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            private void showMenu(java.awt.event.MouseEvent e) {
                int index = contactList.locationToIndex(e.getPoint());
                if (index < 0) {
                    return;
                }

                contactList.setSelectedIndex(index);
                sender = contactList.getModel().getElementAt(index);

                popupMenu.show(contactList, e.getX(), e.getY());
            }
        });
    }
}
