package main;

import crypto.CryptoMessage;
import crypto.CryptoRSA;
import database.Query;
import database.SenderInfo;
import database.UserInfo;
import frontend.chatGUI.ContactUi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

import static main.Main.CONNECT_COOLDOWN_MS;
import static main.Main.PREFIX_REPLY_INFORMATION;
import static main.Main.PREFIX_REQUEST_INFORMATION;
import static main.Main.getLocalIp;
import static main.Main.unknownConnection;
import static main.Main.port;
import static main.Main.unknownConnection;

public class ConnectSenderGUI extends JFrame {

    public static final String PREFIX_REQUEST_INFORMATION = "REQUEST:";
    public static final String PREFIX_REPLY_INFORMATION = "REPLY:";

    private UserInfo user;
    ContactUi contactUi;

    private DefaultListModel<String> availableListModel;
    private JList<String> availableList;
    private JScrollPane scrollPane;
    private JButton connectButton;
    private JButton refreshButton;
    private JLabel statusLabel;

    private List<String> availableKeys = new ArrayList<>();

    public ConnectSenderGUI(UserInfo user, ContactUi contactUi) {
        this.user = user;
        this.contactUi = contactUi;
        initializeGUI();
        refreshButton.doClick();
    }

    private void initializeGUI() {
        setTitle("Connect to Sender");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setPreferredSize(new Dimension(500, 400));
        setLayout(new BorderLayout(10, 10));

        statusLabel = new JLabel("Loading available connections...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel, BorderLayout.NORTH);

        availableListModel = new DefaultListModel<>();
        availableList = new JList<>(availableListModel);
        availableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollPane = new JScrollPane(availableList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Available connections (within cooldown)"));
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadAvailableConnections());

        connectButton = new JButton("Connect");
        connectButton.setEnabled(false);
        connectButton.addActionListener(e -> handleConnect());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        availableList.addListSelectionListener(e -> {
            connectButton.setEnabled(availableList.getSelectedIndex() >= 0);
        });

        buttonPanel.add(refreshButton);
        buttonPanel.add(connectButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadAvailableConnections() {
        SwingUtilities.invokeLater(() -> {
            availableListModel.clear();
            availableKeys.clear();
            statusLabel.setText("Loading connections...");
            connectButton.setEnabled(false);
        });

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                long now = System.currentTimeMillis();

                unknownConnection.entrySet().removeIf(entry
                        -> entry.getValue() < (now - CONNECT_COOLDOWN_MS)
                );

                unknownConnection.forEach((key, value) -> {
                    if (value >= (now - CONNECT_COOLDOWN_MS)) {
                        availableKeys.add(key.toString());
                    }
                });

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    SwingUtilities.invokeLater(() -> {
                        availableListModel.clear();

                        if (availableKeys.isEmpty()) {
                            availableListModel.addElement("No active connections in the last "
                                    + (CONNECT_COOLDOWN_MS / 1000) + " seconds");
                            statusLabel.setText("No recent connections found");
                        } else {
                            for (int i = 0; i < availableKeys.size(); i++) {
                                availableListModel.addElement(i + ": " + availableKeys.get(i));
                            }
                            statusLabel.setText(availableKeys.size() + " connection(s) available");
                        }
                        connectButton.setEnabled(!availableKeys.isEmpty());
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Error loading connections");
                        JOptionPane.showMessageDialog(ConnectSenderGUI.this,
                                "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        };
        worker.execute();
    }

    private void handleConnect() {
        int selectedIndex = availableList.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= availableKeys.size()) {
            JOptionPane.showMessageDialog(this, "Please select a connection",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String chosenKey = availableKeys.get(selectedIndex);

        statusLabel.setText("Connecting to: " + chosenKey);
        connectButton.setEnabled(false);
        refreshButton.setEnabled(false);

        // Run the connection in a background thread
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    System.out.println(chosenKey + " from connectNewSender()");
                    return getSenderInfo(chosenKey);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();

                    if (success) {
                        statusLabel.setText("Connected to: " + chosenKey);
                        JOptionPane.showMessageDialog(ConnectSenderGUI.this,
                                "Connection established successfully!",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadAvailableConnections(); // Refresh list
                    } else {
                        statusLabel.setText("Connection failed");
                        JOptionPane.showMessageDialog(ConnectSenderGUI.this,
                                "Failed to establish connection",
                                "Connection Failed", JOptionPane.ERROR_MESSAGE);
                        loadAvailableConnections();
                    }

                } catch (Exception ex) {
                    statusLabel.setText("Error occurred");
                    JOptionPane.showMessageDialog(ConnectSenderGUI.this,
                            ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    loadAvailableConnections();
                } finally {
                    refreshButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }


    private boolean getSenderInfo(String senderKey) {
        System.err.println(senderKey);
        ConnectionKey senderConnectionKey = new ConnectionKey(senderKey, null);
        System.out.println("ip: " + senderConnectionKey.ip);
        int peerServerPort = Integer.parseInt(senderConnectionKey.port) + 1;// TODO: not hardcode this // convention that the ask port will be +1 of the main server port
        System.out.println("getSenderInfo: " + senderConnectionKey);
        Socket host = null;
        PrintWriter writer = null;
        BufferedReader reader = null;
        try {
            host = new Socket();
            System.out.println("Client connecting to: " + senderConnectionKey.ip + ":" + peerServerPort);
            host.connect(new InetSocketAddress(senderConnectionKey.ip, peerServerPort), (int) CONNECT_COOLDOWN_MS);
            System.out.println(host.getInetAddress().getHostAddress());

            //TODO: be more consistent with RequestInformation. Correct having to put empty space "ignored"
            String requestKey = PREFIX_REQUEST_INFORMATION + user.username + ":" + CryptoRSA.bytePublicKeyToString(user.getPublicKey()) + ":" + "ignored" + ":" + getLocalIp() + ":" + Main.port;

            writer = new PrintWriter(host.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(host.getInputStream()));
            writer.println(requestKey);

            if (!host.isConnected()) {
                return false;
            }

            String senderReply = reader.readLine();
            if (senderReply.equals("REJECTED:")) {
                System.out.println("request rejected string senderReply < getSenderInfo");
                return false;
            }

            // get the aes key
            NewConnectionAccepted newConnection = new NewConnectionAccepted(senderReply, PREFIX_REPLY_INFORMATION);
            System.out.println("from getSenderInfo: " + senderReply);

            //decode
            byte[] encrypted_aes_sender = CryptoMessage.safeBase64Decode(newConnection.AES);
            System.out.println("Encrypted AES key from getSenderInfo: " + newConnection.AES);
            PrivateKey privateKey = CryptoRSA.loadPrivateKeyFromFile(this.user.getUsername());
            byte[] aes_sender = CryptoRSA.decrypt(privateKey, encrypted_aes_sender); // we have their aes key
            if (aes_sender == null) {
                System.out.println("decryption failed");
                writer.println("FAILED:");
                return false;
            }

            CryptoMessage crypt = new CryptoMessage(false);
            byte[] salt = crypt.generateSalt();
            byte[] aes_user = crypt.getAESKeyBytesFromPassword(this.user.getPasswordHashed().toString(), salt);

            String encrypted_aes_user = CryptoRSA.encrypt(CryptoRSA.getPublicKeyFromString(newConnection.publicKey), aes_user);

            String accepted_request_key = PREFIX_REPLY_INFORMATION + user.username + ":" + CryptoRSA.bytePublicKeyToString(user.getPublicKey()) + ":" + encrypted_aes_user;

            writer.println(accepted_request_key);
            String reply = reader.readLine();// send our aes key to them

            if (reply.equals("FAILED:")) {
                System.out.println("request rejected string reply < getSenderInfo");
                return false;
            }

            System.out.println("Reply: " + newConnection); // if sucessfull we save it
            saveSenderInfo(newConnection.username, newConnection.publicKey, senderConnectionKey, aes_user, aes_sender);
            unknownConnection.remove(senderConnectionKey);
            contactUi.loadContacts();
        } catch (SocketTimeoutException ex) {
            System.err.println("Did not respond");
        } catch (NoRouteToHostException e) {
            System.err.println("no route to host error caught");
        } catch (IOException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            System.out.println("io exception here: " + ex.getMessage());
        } finally {
            if (host != null) {
                try {
                    host.close();
                } catch (IOException ignore) {
                }
            }
        }
        return true;
    }

    
    private String saveSenderInfo(String username, String publicKey, ConnectionKey stringKey, byte[] aes_user, byte[] aes_sender) {
        Query query = new Query(false);
        SenderInfo sender = new SenderInfo(username, publicKey);
        query.newConversation(user, sender, aes_user, aes_sender);
        unknownConnection.remove(stringKey);
        query.closeConnection();
        return Base64.getEncoder().encodeToString(aes_user);
    }
}
