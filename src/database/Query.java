/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import crypto.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import frontend.DatabaseUi;
import database.DatabaseManager;
import java.util.HashMap;
import java.util.Map;
import main.Message;

/**
 *
 * @author theunknown
 */
public class Query {

    // TODO: update COUNT operation to boolean returning query
    public PreparedStatement stmt;
    private Connection conn = null;
    public boolean debug;

    public Query(boolean debug) {
        //TODO: input databse information by userinput
        while (this.conn == null) {
            this.conn = new GetConnectionDB(DatabaseManager.loadOrAsk(), this.debug).getConnection();

            if (this.conn == null) {
                DatabaseInfo databaseInfo = DatabaseUi.showDialog();
                this.conn = new GetConnectionDB(databaseInfo, debug).getConnection();

                if (this.conn != null) {
                    DatabaseManager.saveToFile(databaseInfo);
                }
            }
        }

        this.debug = debug;
    }

    public Query(DatabaseInfo databaseInfo, boolean debug) {
        this.conn = new GetConnectionDB(databaseInfo, debug).getConnection();
        this.debug = debug;
    }

    /**
     * closes the created connection
     */
    public void closeConnection() {
        try {
            this.conn.close();
        } catch (SQLException ex) {
            // Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * get UserInfo from database. return null if can't find data
     *
     * @param username
     * @return
     */
    public UserInfo getUser(String username) {
        UserInfo user = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM users where username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                user = new UserInfo();
                user.id = rs.getInt("uid");
                user.username = rs.getString("username");
                user.setPasswordHashed(rs.getBytes("password_hashed"));
                user.setSalt(rs.getBytes("salt"));
                user.setPublicKey(rs.getBytes("public_key"));
                user.created_at = rs.getTimestamp("created_at");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        }
        return user;
    }

    /**
     * creates new UserInfo and all its related property
     *
     * @param username
     * @param password
     * @return
     */
    public UserInfo createUser(String username, String password) {
        UserInfo user = null;
        ResultSet rs = null;
        try {
            user = new UserInfo();

            boolean taken = true;
            do {
                stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
                stmt.setString(1, username);

                rs = stmt.executeQuery();
                if (rs.next()) {
                    if (rs.getInt(1) == 0) {
                        taken = false;
                    } else {
                        System.out.println("username already taken.");
                        Scanner scan = new Scanner(System.in);
                        username = scan.nextLine();
                    }
                }

            } while (taken);

            user.username = username; // set username 

            CryptoRSA crypt = new CryptoRSA();
            CryptoPassword pass = new CryptoPassword(this.debug);
            pass.setPasswordHashedSalt(user, password); // set hashed password and salt

            try {
                crypt.saveNewKeys(user, username); // set pub key
            } catch (IOException ex) {
                System.err.println("could not create file for private key");
            }

        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                    stmt = null;
                }
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return user;
    }

    /**
     * saves UserInfo to the database and returns true if fails
     *
     * @param user
     * @return
     */
    public UserInfo saveNewUser(UserInfo user) {
        try {
            stmt = conn.prepareStatement("INSERT INTO users(username, password_hashed, salt, public_key) values(?,?,?,?)");
            stmt.setString(1, user.username);
            stmt.setBytes(2, user.getPasswordHashed());
            stmt.setBytes(3, user.getSalt());
            stmt.setBytes(4, user.getPublicKey());

            int effectedRow = stmt.executeUpdate();

            if (effectedRow == 0) {
                System.out.println("user not added");
                return null;
            }

            stmt = conn.prepareStatement("SELECT uid from users where username = ? AND public_key = ?");
            stmt.setString(1, user.username);
            stmt.setBytes(2, user.getPublicKey());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user.id = rs.getInt("uid");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return user;
    }

    /**
     * saves sender into the database
     *
     * @param sender
     * @return
     */
    public boolean saveNewSender(SenderInfo sender) {
        try {
            stmt = conn.prepareStatement("INSERT INTO senders(username, public_key, fingerprint, encrypted_aes_key) values(?,?,?,?)");
            stmt.setString(1, sender.username);
            // TODO: aes keys
            SecureRandom random = new SecureRandom();
            byte[] randomBytes = new byte[20];
            random.nextBytes(randomBytes);

            sender.setEncryptedAES(randomBytes);
            //
            stmt.setBytes(2, sender.getPublicKey());
            if (sender.setFingerprint()) {
                return true;
            }
            stmt.setString(3, sender.getFingerprint());
            stmt.setBytes(4, sender.getEncryptedAES());

            int effectedRow = stmt.executeUpdate();

            if (effectedRow == 0) {
                System.out.println("user not added");
                return true;
            }

            stmt = conn.prepareStatement("SELECT sid FROM senders where public_key = ?");
            stmt.setBytes(1, sender.getPublicKey());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                sender.setId(rs.getInt("sid"));
            } else {
                System.err.println("unable to get sid of user " + sender.username); // TODO: remove the invalid user from database
            }
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    public boolean changeUsername(UserInfo userInfo, SenderInfo senderInfo, String username) {
        try {
            stmt = conn.prepareStatement("UPDATE communication_participants SET username = ? where uid = ? and sid = ?");
            stmt.setString(1, username);
            stmt.setInt(2, userInfo.id);
            stmt.setInt(3, senderInfo.getId());

            int result = stmt.executeUpdate();
            if (result == 0) {
                System.err.println("unable to change sender username (changeUsername)");
                return false;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    /**
     * gets sender from the fingerprint
     *
     * @param md5
     * @return
     */
    public SenderInfo getSender(String md5) {
        SenderInfo sender = null;
        try {
            stmt = conn.prepareStatement("SELECT * from senders where fingerprint = ?");
            stmt.setString(1, md5);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("sid");
                String username = rs.getString("username");
                String public_key = new CryptoRSA().bytePublicKeyToString(rs.getBytes("public_key"));
                sender = new SenderInfo(username, public_key);
                sender.setId(id);
            } else {
                System.err.println("unable to get info of sender (getSender)"); // TODO: remove the invalid user from database
            }
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return sender;
    }

    public byte[] relatedSenderAES(int uid, int sid) {
        SenderInfo sender = null;
        try {
            stmt = conn.prepareStatement("SELECT * from communication_participants where uid = ? and sid = ?");
            stmt.setInt(1, uid);
            stmt.setInt(2, sid);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("aes_sender");
            } else {
                System.err.println("unable to get info (relatedSenderAES)"); // TODO: remove the invalid user from database
            }
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public byte[] relatedUserAES(int uid, int sid) {
        SenderInfo sender = null;
        try {
            stmt = conn.prepareStatement("SELECT * from communication_participants where uid = ? and sid = ?");
            stmt.setInt(1, uid);
            stmt.setInt(2, sid);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("aes_user");
            } else {
                System.err.println("unable to get info (relatedUserAES)"); // TODO: remove the invalid user from database
            }
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    /**
     * gets sender from sid
     *
     * @param md5
     * @return
     */
    public SenderInfo getSender(int sid) {
        SenderInfo sender = null;
        try {
            stmt = conn.prepareStatement("SELECT * from senders where sid = ?");
            stmt.setInt(1, sid);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String username = rs.getString("username");
                String public_key = new CryptoRSA().bytePublicKeyToString(rs.getBytes("public_key"));
                sender = new SenderInfo(username, public_key);
                sender.setId(sid);
                sender.setFingerprint(rs.getString("fingerprint").trim());
            } else {
                System.err.println("unable to get info of sender (getSender)"); // TODO: remove the invalid user from database
            }
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return sender;
    }

    // TODO: forgot we have over riding. Make over rided newConversation? or split this method to two. remove state stuff
    public void newConversation(UserInfo user, SenderInfo sender, byte[] aes_user, byte[] aes_sender) {
        if (!hasSender(sender.getFingerprint())) {
            if (saveNewSender(sender)) {
                System.err.println("failed to save new sender (newConversation)");
            }
        }

        sender = getSender(sender.getFingerprint());

        try {
            stmt = conn.prepareStatement("INSERT INTO communication_participants(uid, sid, username, aes_user ,aes_sender) values(?,?,?,?,?);");
            stmt.setInt(1, user.id);
            stmt.setInt(2, sender.getId());
            stmt.setString(3, sender.username);
            stmt.setBytes(4, aes_user);
            stmt.setBytes(5, aes_sender);
            if (!debug) {
                System.out.println("INSERT INTO communication_participants(uid, sid, username) values(" + user.id + "," + sender.getId() + "," + sender.username + ");");
            }

            int effectedRow = stmt.executeUpdate();

            if (effectedRow == 0) {
                System.out.println("conversation not added");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean hasSender(String md5) {
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) AS count\n"
                    + "FROM senders where fingerprint = ?\n");
//            System.out.println("from hasSender");
            stmt.setString(1, md5);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                if (rs.getInt("count") == 1) {
                    return true;
                } else if (rs.getInt("count") > 1) {
                    System.err.println("dublicate senders (hasSender)");
                    return false; // TODO: properly handel this.
                }
            }
            return false;
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    /**
     * returns true if sender's md5 is present in communication_participants
     * database.
     *
     * @param md5
     * @return
     */
    public boolean hasCommunication(int uid, String senderMd5) {
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) AS count\n"
                    + "FROM communication_participants cp\n"
                    + "JOIN senders s ON cp.sid = s.sid\n"
                    + "WHERE cp.uid = ? AND s.fingerprint = ?;");
//            System.out.println("from hasCommunication");
            stmt.setInt(1, uid);
            stmt.setString(2, senderMd5);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                if (rs.getInt("count") == 1) {
                    return true;
                } else if (rs.getInt("count") > 1) {
                    System.err.println("dublicate senders (hasSender)");
                    return false; // TODO: properly handel this.
                }
            }
            return false;
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    /**
     * returns a list of contacts database.
     *
     * @param md5
     * @return
     */
    public List<SenderInfo> getContacts(UserInfo user) {
        List<SenderInfo> contacts = null;
        try {
            contacts = new ArrayList<>();
            stmt = conn.prepareStatement("SELECT sid,username FROM communication_participants where uid = ?;");
            stmt.setInt(1, user.id);
            ResultSet rs = stmt.executeQuery();
            Query temp = new Query(false);
            while (rs.next()) {
                SenderInfo sender = temp.getSender(rs.getInt("sid"));
                sender.nickname = rs.getString("username");
                contacts.add(sender);
            }
            temp.closeConnection();
            return contacts;
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException ex) {
                Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return contacts;
    }

    public boolean saveIncomingEncryptedMessage(Message messageInfo, UserInfo user) {
        PreparedStatement stmt = null;
        try {
            byte[] combined = Base64.getDecoder().decode(messageInfo.getMessage());
            int ivLength = 16;

            if (combined.length < ivLength) {
                System.err.println("Invalid encrypted message length");
                return true;
            }

            byte[] iv = new byte[ivLength];
            byte[] ciphertext = new byte[combined.length - ivLength];

            System.arraycopy(combined, 0, ciphertext, 0, ciphertext.length);
            System.arraycopy(combined, ciphertext.length, iv, 0, ivLength);

            stmt = conn.prepareStatement(
                    "INSERT INTO cypher_messages(uid, sid, ciphertext, iv, read_state, sender) "
                    + "VALUES (?, ?, ?, ?, 0, 0)"
            );
            stmt.setInt(1, user.getId());
            stmt.setInt(2, messageInfo.getSenderInfo().getId());
            stmt.setBytes(3, ciphertext);
            stmt.setBytes(4, iv);

            int affectedRows = stmt.executeUpdate();
            return affectedRows != 1;

        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            return true;
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.WARNING, "Invalid encrypted message", ex);
            return true;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public boolean saveOutgoingEncryptedMessage(SenderInfo sender, String encryptedMessage, int uid) {
        PreparedStatement stmt = null;
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedMessage);
            int ivLength = 16;

            if (combined.length < ivLength) {
                return true;
            }

            byte[] iv = new byte[ivLength];
            byte[] ciphertext = new byte[combined.length - ivLength];

            System.arraycopy(combined, 0, ciphertext, 0, ciphertext.length);
            System.arraycopy(combined, ciphertext.length, iv, 0, ivLength);

            stmt = conn.prepareStatement(
                    "INSERT INTO cypher_messages(uid, sid, ciphertext, iv, read_state, sender) "
                    + "VALUES (?, ?, ?, ?, 0, 1)"
            );
            stmt.setInt(1, uid);
            stmt.setInt(2, sender.getId());
            stmt.setBytes(3, ciphertext);
            stmt.setBytes(4, iv);

            return stmt.executeUpdate() != 1;
        } catch (Exception ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            return true;
        } finally {
            if (stmt != null) try {
                stmt.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public boolean deleteMessages(UserInfo user, SenderInfo sender) {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM cypher_messages where uid = ? AND sid = ?");
            stmt.setInt(1, user.getId());
            stmt.setInt(2, sender.getId());

            int affectedRows = stmt.executeUpdate();
            return affectedRows != 1;

        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            return true;
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.WARNING, "Invalid Request", ex);
            return true;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public Map<String, List<Message>> getUserMessages(UserInfo user) {
        PreparedStatement stmt = null;
        Map<String, List<Message>> conversationMap = new HashMap<>();
        try {
            stmt = conn.prepareStatement("select  cypher_messages.*, senders.sid, senders.fingerprint, communication_participants.aes_user, communication_participants.aes_sender, communication_participants.username from \n"
                    + "	cypher_messages join communication_participants on cypher_messages.uid = communication_participants.uid and cypher_messages.sid = communication_participants.sid join\n"
                    + "	senders on communication_participants.sid = senders.sid where\n"
                    + "	cypher_messages.uid = ? order by\n"
                    + "	cypher_messages.sent_at ASC;");
            stmt.setInt(1, user.id);
            ResultSet rs = stmt.executeQuery();
            Query queryAES = new Query(false);
            Query querySender = new Query(false);
            while (rs.next()) {
                int senderId = rs.getInt("senders.sid");
                String fingerprint = rs.getString("senders.fingerprint");
                byte[] encrypted = rs.getBytes("cypher_messages.ciphertext");
                byte[] ivBytes = rs.getBytes("cypher_messages.iv");
                Boolean sentByUser = rs.getBoolean("cypher_messages.sender");

                byte[] aesUser = queryAES.relatedUserAES(user.getId(), senderId);
                byte[] aesSender = queryAES.relatedSenderAES(user.getId(), senderId);
                String decrypted;
                try {
                    if (sentByUser) {
                        decrypted = CryptoMessage.decryptMessage(ivBytes,encrypted, aesSender).replace("//n", "\n");
                    } else {
                        decrypted = CryptoMessage.decryptMessage(ivBytes,encrypted, aesUser).replace("//n", "\n");
                    }
                } catch (Exception e) {
                    decrypted = "[unreadable message]";
                }

                SenderInfo senderInfo = querySender.getSender(fingerprint);
                Message msg = new Message(senderInfo, decrypted, sentByUser);
                conversationMap.computeIfAbsent(fingerprint, (k) -> new ArrayList<>()).add(msg);
            }
            queryAES.closeConnection();
            querySender.closeConnection();
            return conversationMap;

        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.WARNING, "Invalid Request", ex);
            return null;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
