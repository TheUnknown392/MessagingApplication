/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend.chatGUI;

import database.Query;
import database.SenderInfo;
import database.UserInfo;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import frontend.MessageHistoryLoader;
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
    public DefaultListModel<SenderInfo> contactListModel = null;
    protected JList<SenderInfo> contactList = null;
    protected JPopupMenu popupMenu = new JPopupMenu();
    private boolean historyLoaded = false;

    private Query query = null;
    private UserInfo user = null;
    private SenderInfo sender = null;
    private ChatUi chatUi = null;
    MessageHistoryLoader historyLoader = null;
    

    private ArrayList<String> unRead = new ArrayList<>();

    Object lock = new Object();

    public ContactUi(Query query, UserInfo user, ChatUi chatUi) {
        this.chatUi = chatUi;
        this.historyLoader = new MessageHistoryLoader(chatUi, user);
        contactListModel = new DefaultListModel<>();
        contactList = new JList(contactListModel);
        contactScroll = new JScrollPane(contactList);
        this.query = query;
        this.user = user;
        installPopupMenu();

        contactList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            javax.swing.JLabel label = new javax.swing.JLabel();
            label.setOpaque(true);
            label.setText(value.nickname);

            if (unRead.contains(value.getFingerprint())) {
                label.setBackground(Color.GREEN);
            } else {
                label.setBackground(list.getBackground());
            }

            if (isSelected) {
                label.setBackground(Color.GRAY);
            }
            return label;
        });
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

    public void messageIncomming(SenderInfo sender) {
        if (sender == null) {
            return;
        }
        synchronized (lock) {
            unRead.add(sender.getFingerprint());
        }
        contactList.repaint();
    }

    public void messageRead(SenderInfo sender) {
        if (sender == null) {
            return;
        }
        synchronized (lock) {
            unRead.remove(sender.getFingerprint());
        }
        contactList.repaint();
    }

    private void installPopupMenu() {

        JMenuItem renameChat = new JMenuItem("Rename");
        JMenuItem deleteChat = new JMenuItem("Delete Messages");

        popupMenu.add(renameChat);
        popupMenu.add(deleteChat);

        renameChat.addActionListener(ev -> {
            if (sender == null) {
                return;
            }

            String nickname = JOptionPane.showInputDialog(popupMenu, "Username:", sender.username);

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
            int choice = JOptionPane.showConfirmDialog(renameChat, "Do you reall want to delete message of" + sender.nickname + " (" + sender.username + "). This will delete all messages too.", "Confirm Delete?", JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                Query temp = new Query(false);
                temp.deleteMessages(user, sender);
                temp.closeConnection();
                chatUi.messageDisplay.deleteHistory(sender.getFingerprint());

                loadContacts();
            }
        });

        contactList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            private void showMenu(MouseEvent e) {
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
