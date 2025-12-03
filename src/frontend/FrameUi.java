/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

import database.UserInfo;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import javax.swing.SwingUtilities;

import main.*;


/**
 *
 * @author theunknown
 */
public class FrameUi {
    
    protected static final int x = 800;
    protected static final int y = 600;
    
    JFrame frame = null;
    protected ChatUi chatUi;
    protected StatusUi statusUi;

    
    public FrameUi(UserInfo user){
        
        frame = new JFrame("Encrypted Messaging Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(x, y);
        
        chatUi = new ChatUi(user, frame);
        statusUi = new StatusUi();
        
        
        frame.add(chatUi.splitPane, BorderLayout.CENTER);
        frame.add(statusUi.statusPanel, BorderLayout.SOUTH);
        
        new Thread(new MessageManager(this)).start();
        
        frame.setVisible(true);
        
    }
}
