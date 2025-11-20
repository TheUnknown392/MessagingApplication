/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import main.*;


/**
 *
 * @author theunknown
 */
public class Ui {
    
    private final int x = 800;
    private final int y = 600;
    
    JFrame frame = null;
    JTextArea showMessage = null;
    JTextField messageField = null;
    
    JScrollPane contactScroll = null;
    JScrollPane messageScroll = null;
    JPanel inputPanel = null;
    JPanel statusPanel = null;
    JButton sendMessage = null;
    
    public Ui(){
        
        frame = new JFrame("Encrypted Messaging Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(x, y);
        
        messageField = new JTextField();
        
        sendMessage = new JButton("Send");
        showMessage = new JTextArea();
        showMessage.setEditable(false);
        
        inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendMessage,BorderLayout.EAST);
        
        statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(new JLabel("status"),BorderLayout.EAST);

        JPanel contactPanel = new JPanel(new BorderLayout());
        contactScroll = new JScrollPane();
        contactPanel.add(contactScroll, BorderLayout.CENTER);
        JLabel contactTitle = new JLabel("Contacts");
        contactPanel.add(contactTitle, BorderLayout.NORTH);
        
        JPanel messagePanel = new JPanel(new BorderLayout());
        messageScroll = new JScrollPane(showMessage);
        messagePanel.add(messageScroll, BorderLayout.CENTER);
        messagePanel.add(new JLabel("Message"), BorderLayout.NORTH); 
        messagePanel.add(inputPanel, BorderLayout.SOUTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(150);
        splitPane.setLeftComponent(contactPanel);
        splitPane.setRightComponent(messagePanel);
        
        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(statusPanel, BorderLayout.SOUTH);
        
        
        frame.setVisible(true);
        
    }
}
