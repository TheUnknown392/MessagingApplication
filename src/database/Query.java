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
import java.util.Scanner;

/**
 *
 * @author theunknown
 */

public class Query {
    // TODO: update COUNT operation to boolean returning query
    public PreparedStatement stmt;
    private Connection conn;
    public boolean debug;

    public Query(boolean debug) {
        //TODO: input databse information by userinput
        this.conn = new GetConnectionDB("localhost", "3306", "messagedb", "user", "1234", debug).getConnection();
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
    public boolean saveNewUser(UserInfo user) {
        try {
            stmt = conn.prepareStatement("INSERT INTO users(username, password_hashed, salt, public_key) values(?,?,?,?)");
            stmt.setString(1, user.username);
            stmt.setBytes(2, user.getPasswordHashed());
            stmt.setBytes(3, user.getSalt());
            stmt.setBytes(4, user.getPublicKey());

            int effectedRow = stmt.executeUpdate();

            if (effectedRow == 0) {
                System.out.println("user not added");
                return true;
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
            stmt.setString(3, sender.getFingerpring());
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
    
    public SenderInfo getSender(String md5){
        SenderInfo sender = null;
        try {
            stmt = conn.prepareStatement("SELECT * from senders where fingerprint = ?");
            stmt.setString(1, md5);
            
            ResultSet rs = stmt.executeQuery();
            sender = new SenderInfo(rs.getString("username"),rs.getString("md5"));
            if (rs.next()) {
                rs.getInt("sid");
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

    public boolean newConversation(UserInfo user, SenderInfo sender) {
        // TODO: have the aes key in conversation_participants

        if (!hasSender(sender.getFingerpring())) {
            if (saveNewSender(sender)) {
                System.err.println("failed to save new sender (newConversation)");
            }
        }else{
            sender = getSender(sender.getFingerpring());
        }

        try {
            stmt = conn.prepareStatement("INSERT INTO communication_participants(uid, sid) values(?,?);");
            stmt.setInt(1, user.id);
            stmt.setInt(2, sender.getId());
            if (!debug) {
                System.out.println("INSERT INTO communication_participants(uid, sid) values(" + user.id + "," + sender.getId() + ");");
            }

            int effectedRow = stmt.executeUpdate();

            if (effectedRow == 0) {
                System.out.println("conversation not added");
                return true;
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
    
    public boolean hasSender(String md5){
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) AS count\n"
                    + "FROM senders where fingerprint = ?\n");
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
}
