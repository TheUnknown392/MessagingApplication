/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend.chatGUI;

import database.Query;
import database.SenderInfo;
import database.UserInfo;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JList;
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
    
    private Query query = null;
    private UserInfo user = null;
    public ContactUi(Query query, UserInfo user){
        contactListModel = new DefaultListModel<>();
        contactList = new JList(contactListModel);
        contactScroll = new JScrollPane(contactList);
        this.query = query;
        this.user = user;
    }
    
    public void loadContacts(){
        contactListModel.clear();
        List<SenderInfo> contacts = new ArrayList<>(query.getContacts(user));
        contacts.forEach((e)->{
            try {
                SwingUtilities.invokeAndWait(()->{
                    contactListModel.addElement(e);
                });
            } catch (InterruptedException ex) {
                Logger.getLogger(ChatUi.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(ChatUi.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        contactList.setSelectedIndex(0);
    }
    
    public SenderInfo getSelectedSender(){
        return contactList.getSelectedValue();
    }
}
