/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import crypto.CryptoPassword;
import database.UserInfo;
import database.Query;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author theunknown
 */
public class LandingUi extends JDialog {

    private UserInfo result = null;

    private JTextField username;
    private JPasswordField password;
    private JButton submitButton;
    private JButton databaseButton;

    public LandingUi(JFrame parent) {
        super(parent, "Encrypted Messaging Application", true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        int x = 450;
        int y = 350;
        setSize(x, y);
        setLayout(new BorderLayout(15, 15));
        setLocationRelativeTo(parent);

        GridLayout formLayout = new GridLayout(2, 2, 15, 15);
        GridLayout buttonLayout = new GridLayout(2, 1, 15, 15);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setHorizontalAlignment(SwingConstants.CENTER);

        username = new JTextField();
        password = new JPasswordField();

        submitButton = new JButton("Submit");
        submitButton.setActionCommand("Submit");
        databaseButton = new JButton("Database Settings");
        databaseButton.setActionCommand("Database");

        submitButton.addActionListener(new onClick());
        databaseButton.addActionListener(new onClick());

        JPanel form = new JPanel(formLayout);
        form.add(usernameLabel);
        form.add(username);
        form.add(passwordLabel);
        form.add(password);

        JPanel buttons = new JPanel(buttonLayout);
        buttons.add(submitButton);
        buttons.add(databaseButton);

        add(new JPanel(), BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        
        this.getRootPane().setDefaultButton(submitButton);
    }

    private class onClick implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String message = e.getActionCommand();
            switch (message) {
                case "Submit":
                    boolean verified = submitHandle();
                    if (!verified) {
                        JOptionPane.showMessageDialog(rootPane, "Could not log in", "Try Again!", JOptionPane.ERROR_MESSAGE);
                    } else {
                        dispose();
                    }
                    break;
                case "Database":
                    // TODO: make database information inputted by user, somehow and save it for future use in some file;
                    break;
                default:
                    System.err.println("Unexpected onClick action case: " + message);
            }
        }

    }

    public boolean submitHandle() {
        // TODO: remove necessary debug parameter in constructor
        String username = this.username.getText();
        System.out.println(username);
        String password = new String(this.password.getPassword());
        System.out.println(password);
        
        if(!(cleanUsername(username) && cleanPassword(password))){
            JOptionPane.showConfirmDialog(this, "username cannot have ':', ' ' character(s) and it cannot be empty", "Dirty input.", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        Query query = new Query(false);
        UserInfo user = query.getUser(username);

        if (user == null) {
            int choice = JOptionPane.showConfirmDialog(this, "Do you want to create new user ? Don't forget the password.", "Unknown User", JOptionPane.WARNING_MESSAGE);
            
            if (choice != JOptionPane.YES_OPTION) {
                this.username.setText("");
                this.password.setText("");
                return false;
            }
            
            user = query.createUser(username, password);

            if ((user = query.saveNewUser(user)) == null) {
                System.exit(1);
            }
            query.closeConnection();
        }
        query.closeConnection();
        // System.out.println(user.username);
        boolean verification = new CryptoPassword(false).verifyPassword(user, password);
        System.out.println(verification);
        assert user != null;
        this.result = user;
        return verification;
    }

    public UserInfo showDialog() {
        setVisible(true);
        return result;
    }
    
    private boolean cleanUsername(String username){
        return !(username.contains(":") || username.contains(" ") || username.equals(""));
    }
    
    private boolean cleanPassword(String password){
        return !username.equals("");
    }
    
}
// TODO: message notification
// TODO: get conversation list of selected user only and not other users
