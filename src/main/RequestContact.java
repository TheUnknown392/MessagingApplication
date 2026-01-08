/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

import crypto.CryptoMessage;
import crypto.CryptoRSA;
import database.Query;
import database.SenderInfo;
import database.UserInfo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import main.ConnectionKey;
import main.Main;
import main.RequestInformation;
import main.NewConnectionAccepted;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static main.Main.portAsk;
import static main.Main.CONNECT_COOLDOWN_MS;
import static main.Main.unknownConnection;

/**
 *
 * @author theunknown
 */
public class RequestContact extends JFrame {

    public static final String PREFIX_CONNECT = "CONNECT:";
    public static final String PREFIX_REQUEST_INFORMATION = "REQUEST:";
    public static final String PREFIX_REPLY_INFORMATION = "REPLY:";

    protected JScrollPane contactScroll = null;
    protected DefaultListModel<String> contactListModel = null;
    protected JList<String> contactList = null;

    UserInfo user;
    
    ServerSocket askServer = null;
    public RequestContact(UserInfo user) {
        // TODO: Doesn't work correctly. Remaning to update this with corrected version in Main
        this.user = user;

        setTitle("Choose Contact");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    System.out.println("Close button pressed");
                    askServer.close();   
                    dispose();
                } catch (IOException ex) {
                    System.getLogger(RequestContact.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
            }
        });
        
        setPreferredSize(new Dimension(450, 300));

        JLabel waitingLabel = new JLabel("Waiting for request...");
        JLabel instruction = new JLabel("Incoming connection requests:");

        contactListModel = new DefaultListModel<>();
        contactList = new JList<>(contactListModel);
        contactScroll = new JScrollPane(contactList);

        setLayout(new BorderLayout(10, 10));
        waitingLabel.setHorizontalAlignment(JLabel.CENTER);

        add(waitingLabel, BorderLayout.NORTH);
        add(contactScroll, BorderLayout.CENTER);
        add(instruction, BorderLayout.SOUTH);

        new Thread(() -> {
            newSenderInPermission();
        }).start();

        setVisible(true);

    }

    /**
     * Opens server for new Sender requests
     *
     * @param scan
     */
    private void newSenderInPermission() {
        // TODO: do this properly so it doesn't wait for in.readline() too long
        System.out.println("waiting for Senders request...");
        Socket socket = null;

        PrintWriter writer = null;
        BufferedReader reader = null;

        try {
            System.out.println("Server listening on: " + portAsk);
            askServer = new ServerSocket(portAsk);

            socket = askServer.accept();
            System.out.println("conntected to : " + socket.getInetAddress().getHostAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String peerIdentity = in.readLine();

            if (!peerIdentity.startsWith(PREFIX_REQUEST_INFORMATION)) {
                return;
            }

            RequestInformation request = new RequestInformation(peerIdentity, PREFIX_REQUEST_INFORMATION);
            System.out.println("Request by: " + request); // TODO: some abstraction/polymorphism?

            // show request in GUI list
            SwingUtilities.invokeLater(() -> 
                    contactListModel.addElement(request.ip + ":" + request.port)
            );

            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Accept request from:\n" + CryptoRSA.md5Fingerprint(request.publicKey) + ":" + request.ip + ":" + request.port,
                    "Incoming Request",
                    JOptionPane.YES_NO_OPTION
            );

            if (choice != JOptionPane.YES_OPTION) {
                writer.println("REJECTED:");
                return;
            }

            String key = CryptoRSA.md5Fingerprint(request.publicKey) + ":" + request.ip + ":" + request.port;
            System.out.println("key: " + key);
            ConnectionKey unknownClient = new ConnectionKey(key, PREFIX_CONNECT);

            // TODO: make this part a method of itself.
            CryptoMessage crypt = new CryptoMessage(false);
            byte[] salt = crypt.generateSalt();
            byte[] aes_user = crypt.getAESKeyBytesFromPassword(this.user.getPasswordHashed().toString(), salt);

            String encrypted_aes_user
                    = CryptoRSA.encrypt(CryptoRSA.getPublicKeyFromString(request.publicKey), aes_user);

            String senderReply
                    = PREFIX_REPLY_INFORMATION
                    + user.username + ":"
                    + CryptoRSA.bytePublicKeyToString(user.getPublicKey()) + ":"
                    + encrypted_aes_user;

            System.out.println("from newSenderInPermission: " + senderReply);
            writer.println(senderReply); // send our aes key

            String reply = reader.readLine(); // get their aes key

            if ("FAILED:".equals(reply)) {
                System.out.println("Failed by client");
                return;
            }

            NewConnectionAccepted sucessfull = new NewConnectionAccepted(reply, PREFIX_REPLY_INFORMATION);

            byte[] encrypted_aes_sender = Base64.getDecoder().decode(sucessfull.AES);

            PrivateKey privateKey = CryptoRSA.loadPrivateKeyFromFile(this.user.getUsername());

            byte[] aes_sender = CryptoRSA.decrypt(privateKey, encrypted_aes_sender);

            if (aes_sender == null) {
                System.out.println("decryption failed");
                writer.println("FAILED:");
                return;
            }

            writer.println(sucessfull.toString()); // sending random information that is not "FAILED:"

            System.out.println("Reply: " + sucessfull.toString());

            saveSenderInfo(sucessfull.username, sucessfull.publicKey, unknownClient, aes_user, aes_sender);

            unknownConnection.remove(unknownClient);

        } catch (SocketTimeoutException e) {
            System.out.println("Timeout");
        } catch (ConnectException e) {
            System.out.println("Connection refused: make /openSender");
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
                if (askServer != null) {
                    askServer.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private String saveSenderInfo(String username, String publicKey, ConnectionKey stringKey, byte[] aes_sender, byte[] aes_user) {
        Query query = new Query(false);
        SenderInfo sender = new SenderInfo(username, publicKey);
        query.newConversation(this.user, sender, aes_sender, aes_user);
        unknownConnection.remove(stringKey);
        query.closeConnection();
        return Base64.getEncoder().encodeToString(aes_user);
    }
}
