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
import frontend.chatGUI.ContactUi;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
    ContactUi contactUi;
    
    ServerSocket askServer = null;
    private volatile boolean serverRunning = true;
    
    // Store pending requests with their sockets
    private Map<String, PendingRequest> pendingRequests = new HashMap<>();
    private List<Socket> activeSockets = new ArrayList<>();
    
    private JButton acceptButton;
    private JButton rejectButton;
    
    // Inner class to hold request data
    private static class PendingRequest {
        RequestInformation requestInfo;
        Socket socket;
        BufferedReader reader;
        PrintWriter writer;
        
        PendingRequest(RequestInformation requestInfo, Socket socket, 
                      BufferedReader reader, PrintWriter writer) {
            this.requestInfo = requestInfo;
            this.socket = socket;
            this.reader = reader;
            this.writer = writer;
        }
    }
    
    public RequestContact(UserInfo user, ContactUi contactUi) {
        this.user = user;
        this.contactUi = contactUi;
        setTitle("Choose Contact");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
        
        setPreferredSize(new Dimension(450, 300));

        JLabel waitingLabel = new JLabel("Waiting for requests...");
        JLabel instruction = new JLabel("Select a request and click Accept or Reject:");

        contactListModel = new DefaultListModel<>();
        contactList = new JList<>(contactListModel);
        contactScroll = new JScrollPane(contactList);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        acceptButton = new JButton("Accept");
        rejectButton = new JButton("Reject");
        
        acceptButton.setEnabled(false);
        rejectButton.setEnabled(false);
        
        // Add action listeners
        acceptButton.addActionListener(e -> handleAccept());
        rejectButton.addActionListener(e -> handleReject());
        
        // Enable buttons when selection changes
        contactList.addListSelectionListener(e -> {
            boolean hasSelection = !contactList.isSelectionEmpty();
            acceptButton.setEnabled(hasSelection);
            rejectButton.setEnabled(hasSelection);
        });
        
        buttonPanel.add(acceptButton);
        buttonPanel.add(rejectButton);

        setLayout(new BorderLayout(10, 10));
        waitingLabel.setHorizontalAlignment(JLabel.CENTER);
        waitingLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        instruction.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        add(waitingLabel, BorderLayout.NORTH);
        add(contactScroll, BorderLayout.CENTER);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(instruction, BorderLayout.NORTH);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        pack();

        // Start server thread
        new Thread(() -> {
            listenForRequests();
        }).start();

        setVisible(true);
    }

    /**
     * Continuously listens for incoming connection requests
     */
    private void listenForRequests() {
        try {
            System.out.println("Server listening on port: " + portAsk);
            askServer = new ServerSocket(portAsk);
            
            while (serverRunning) {
                try {
                    Socket socket = askServer.accept();
                    activeSockets.add(socket);
                    System.out.println("Connected to: " + socket.getInetAddress().getHostAddress());
                    
                    // Handle each request in a separate thread
                    new Thread(() -> handleNewRequest(socket)).start();
                    
                } catch (SocketTimeoutException e) {
                    // Continue loop
                } catch (IOException e) {
                    if (serverRunning) {
                        System.err.println("Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            cleanup();
        }
    }

    /**
     * Handles a new incoming request
     */
    private void handleNewRequest(Socket socket) {
        BufferedReader reader = null;
        PrintWriter writer = null;
        
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String peerIdentity = reader.readLine();

            if (peerIdentity == null || !peerIdentity.startsWith(PREFIX_REQUEST_INFORMATION)) {
                socket.close();
                return;
            }

            RequestInformation request = new RequestInformation(peerIdentity, PREFIX_REQUEST_INFORMATION);
            System.out.println("Request received from: " + request);

            writer = new PrintWriter(socket.getOutputStream(), true);

            // Create display key
            String displayKey = CryptoRSA.md5Fingerprint(request.publicKey) + 
                              " (" + request.ip + ":" + request.port + ")";
            
            // Store the pending request
            synchronized (pendingRequests) {
                PendingRequest pendingRequest = new PendingRequest(request, socket, reader, writer);
                pendingRequests.put(displayKey, pendingRequest);
            }

            // Add to GUI list
            SwingUtilities.invokeLater(() -> {
                contactListModel.addElement(displayKey);
            });

        } catch (IOException e) {
            System.err.println("Error handling new request: " + e.getMessage());
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    /**
     * Handles accept button click
     */
    private void handleAccept() {
        String selectedKey = contactList.getSelectedValue();
        if (selectedKey == null) {
            return;
        }
        
        PendingRequest pending;
        synchronized (pendingRequests) {
            pending = pendingRequests.get(selectedKey);
        }
        
        if (pending == null) {
            JOptionPane.showMessageDialog(this, 
                "Request no longer available", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            contactListModel.removeElement(selectedKey);
            return;
        }
        
        // Process the request in a separate thread
        new Thread(() -> processAcceptedRequest(selectedKey, pending)).start();
    }

    /**
     * Handles reject button click
     */
    private void handleReject() {
        String selectedKey = contactList.getSelectedValue();
        if (selectedKey == null) {
            return;
        }
        
        PendingRequest pending;
        synchronized (pendingRequests) {
            pending = pendingRequests.remove(selectedKey);
        }
        
        if (pending != null) {
            try {
                pending.writer.println("REJECTED:");
                pending.socket.close();
                activeSockets.remove(pending.socket);
            } catch (IOException e) {
                System.err.println("Error rejecting request: " + e.getMessage());
            }
        }
        
        SwingUtilities.invokeLater(() -> {
            contactListModel.removeElement(selectedKey);
        });
    }

    /**
     * Processes an accepted request (the main handshake logic)
     */
    private void processAcceptedRequest(String displayKey, PendingRequest pending) {
        try {
            RequestInformation request = pending.requestInfo;
            PrintWriter writer = pending.writer;
            BufferedReader reader = pending.reader;
            
            String connectionKey = CryptoRSA.md5Fingerprint(request.publicKey) + 
                                  ":" + request.ip + ":" + request.port;
            System.out.println("Processing accepted request: " + connectionKey);
            
            ConnectionKey unknownClient = new ConnectionKey(connectionKey, PREFIX_CONNECT);

            // Generate and encrypt AES key
            CryptoMessage crypt = new CryptoMessage(false);
            byte[] salt = crypt.generateSalt();
            byte[] aes_user = crypt.getAESKeyBytesFromPassword(
                this.user.getPasswordHashed().toString(), salt);
            
            String encrypted_aes_user = CryptoRSA.encrypt(CryptoRSA.getPublicKeyFromString(request.publicKey), aes_user);
            String senderReply = PREFIX_REPLY_INFORMATION
                    + user.username + ":"
                    + CryptoRSA.bytePublicKeyToString(user.getPublicKey()) + ":"
                    + encrypted_aes_user;

            System.out.println("Sending reply to accepted request");
            writer.println(senderReply);

            String reply = reader.readLine();

            if ("FAILED:".equals(reply) || reply == null) {
                System.out.println("Failed by client");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, 
                        "Connection failed by remote client", 
                        "Connection Failed", 
                        JOptionPane.ERROR_MESSAGE);
                });
                return;
            }

            NewConnectionAccepted successful = new NewConnectionAccepted(reply, PREFIX_REPLY_INFORMATION);

            byte[] encrypted_aes_sender = Base64.getDecoder().decode(successful.AES);
            PrivateKey privateKey = CryptoRSA.loadPrivateKeyFromFile(this.user.getUsername());
            byte[] aes_sender = CryptoRSA.decrypt(privateKey, encrypted_aes_sender);

            if (aes_sender == null) {
                System.out.println("Decryption failed");
                writer.println("FAILED:");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, 
                        "Decryption failed", 
                        "Connection Failed", 
                        JOptionPane.ERROR_MESSAGE);
                });
                return;
            }

            writer.println(successful.toString());
            System.out.println("Connection established successfully");

            saveSenderInfo(successful.username, successful.publicKey, 
                          unknownClient, aes_user, aes_sender);

            unknownConnection.remove(unknownClient);
            contactUi.loadContacts();
            SwingUtilities.invokeLater(() -> {
                contactListModel.removeElement(displayKey);
                JOptionPane.showMessageDialog(this, 
                    "Connection established with " + successful.username, 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            });

        } catch (Exception ex) {
            Logger.getLogger(RequestContact.class.getName()).log(Level.SEVERE, null, ex);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Error processing request: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
        } finally {
            // Clean up
            synchronized (pendingRequests) {
                pendingRequests.remove(displayKey);
            }
            try {
                pending.socket.close();
                activeSockets.remove(pending.socket);
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Saves sender information to database
     */
    private String saveSenderInfo(String username, String publicKey, 
                                  ConnectionKey stringKey, byte[] aes_sender, byte[] aes_user) {
        Query query = new Query(false);
        SenderInfo sender = new SenderInfo(username, publicKey);
        query.newConversation(this.user, sender, aes_sender, aes_user);
        unknownConnection.remove(stringKey);
        query.closeConnection();
        return Base64.getEncoder().encodeToString(aes_user);
    }

    /**
     * Cleanup method to close all sockets and server
     */
    private void cleanup() {
        System.out.println("Cleaning up RequestContact resources...");
        serverRunning = false;
        
        // Close all active sockets
        synchronized (activeSockets) {
            for (Socket socket : activeSockets) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                }
            }
            activeSockets.clear();
        }
        
        // Close pending request sockets
        synchronized (pendingRequests) {
            for (PendingRequest pending : pendingRequests.values()) {
                try {
                    pending.writer.println("REJECTED:");
                    pending.socket.close();
                } catch (IOException e) {
                }
            }
            pendingRequests.clear();
        }
        
        // Close server socket
        try {
            if (askServer != null && !askServer.isClosed()) {
                askServer.close();
            }
        } catch (IOException e) {
            // Ignore`
        }
        
        dispose();
    }
}