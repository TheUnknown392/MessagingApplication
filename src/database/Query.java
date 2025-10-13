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
import java.util.Scanner;

/**
 *
 * @author theunknown
 */
public class Query {

    public PreparedStatement stmt;
    private Connection conn;
    public boolean debug;

    public Query(Connection conn, boolean debug) {
        this.conn = conn;
        this.debug = debug;
    }
    
    /**
     * get UserInfo from database. return null if can't find data
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
        }catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        }
        return user;
    }

    /**
     * creates new UserInfo and all its related property
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
                rs.next();
                
                if(rs.getInt(1) == 0){
                    taken = false;
                }else{
                    System.out.println("username already taken.");
                    Scanner scan = new Scanner(System.in);
                    username = scan.nextLine();
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
            
            if (effectedRow == 0){
                System.out.println("user not added");
                return true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Query.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
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
