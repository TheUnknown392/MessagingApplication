/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend.chatGUI;

import database.UserInfo;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import javax.swing.JDialog;
import java.awt.Dimension;
import java.awt.*;
import javax.swing.*;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import main.RequestContact;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

/**
 *
 * @author theunknown
 */
public class NewContactUi extends JDialog {
    // TODO:
    private UserInfo user = null;
    private onClick onClickActionListener;
    private JButton acceptButton;
    private JButton requestButton;

    public NewContactUi(UserInfo user) {
        this.user = user;
        this.onClickActionListener = new onClick();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Contact Options");
        

        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
        

        JLabel titleLabel = new JLabel("Contact Options");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        requestButton = new JButton("Send Request");
        requestButton.setActionCommand("Request");
        requestButton.addActionListener(onClickActionListener);

        requestButton.setPreferredSize(new Dimension(180, 40));
        requestButton.setMaximumSize(new Dimension(180, 40));
        requestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        buttonPanel.add(requestButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        

        acceptButton = new JButton("Accept Request");
        acceptButton.setActionCommand("Accept");
        acceptButton.addActionListener(onClickActionListener);
        acceptButton.setPreferredSize(new Dimension(180, 40));
        acceptButton.setMaximumSize(new Dimension(180, 40));
        acceptButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        buttonPanel.add(acceptButton);
        
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        
        setContentPane(mainPanel);
        pack();
        setMinimumSize(new Dimension(300, 200));
        setLocationRelativeTo(null);
        setVisible(true);

    }

    private class onClick implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String message = e.getActionCommand();
            switch (message) {
                case "Accept": {
                    System.out.println("TODO: Accept Request");
                    setVisible(false);
                    dispose();
                    SwingUtilities.invokeLater(() ->{
                        new RequestContact(user);
                     });
                }
                break;
                case "Request":
                    System.out.println("TODO: Send Request");
                break;
                default:
                    System.err.println("Unexpected onClick action case: " + message);
            }
        }

    }

}
