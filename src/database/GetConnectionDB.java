/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

/**
 *
 * @author theunknown
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class GetConnectionDB {
    
    public Connection conn;
    
    private boolean debug; 
    
    private String dbhost;
    private String dbport;
    private String dbName;
    private String dbuser;
    private String dbpassword;


    public GetConnectionDB(String host, String port, String dbName, String user, String password, boolean debug) {
        this.dbhost = host;
        this.dbport = port;
        this.dbName = dbName;
        this.dbuser = user;
        this.dbpassword = password;
        
        this.debug = debug;
    }
    /**
     * returns connection to given database information and setus up database if not found. 
     * @return 
     */
    public Connection getConnection(){
        // Connect to MySQL server
        String serverUrl = "jdbc:mariadb://" + dbhost + ":" + dbport + "/?useSSL=false&serverTimezone=UTC";
        try {
            this.conn = DriverManager.getConnection(serverUrl, dbuser, dbpassword);
        } catch (SQLException e) {
            System.out.println("cannot get connection to server:"+e.getMessage());
        }

        // Create the database if it doesn't exist
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            System.out.println("Database created or already exists: " + dbName);
        }catch(SQLException e){
            System.out.println("cannot create database"+e.getMessage());
        }

        // Connect to the database
        String urlToDb = "jdbc:mariadb://" + dbhost + ":" + dbport + "/" + dbName + "?useSSL=false&serverTimezone=UTC";
        try {
            conn = DriverManager.getConnection(urlToDb, dbuser, dbpassword);
        } catch (SQLException e) {
            System.out.println("Cannnot connect to the database:"+e.getMessage());
        }
        System.out.println("Connected to new database: " + dbName);

        // Create all tables
        createUsersTable();
        createSendersTable();
        createCommunicationParticipantsTable();
        createCypherMessagesTable();
        if(this.debug){
            System.out.println("All tables created or already exist.");
        }
        return conn;
    }
    
    private void createUsersTable(){
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "uid BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "username TEXT NOT NULL UNIQUE,"
                + "password_hashed VARBINARY(75) NOT NULL UNIQUE,"
                + "salt VARBINARY(20) NOT NULL,"
                + "public_key VARBINARY(600),"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }catch (SQLException e) {
            System.err.println("Cannot create Users table: "+e.getMessage());
        }
    }

    private void createSendersTable(){
        String sql = "CREATE TABLE IF NOT EXISTS senders ("
                + "sid BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "username TEXT NOT NULL,"
                + "public_key VARBINARY(600) NOT NULL,"
                + "encrypted_aes_key VARBINARY(600),"
                + "encounter_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }catch (SQLException e) {
            System.out.println("Cannot create Senders table: " +e .getMessage());
        }
    }

    private void createCommunicationParticipantsTable(){
        String sql = "CREATE TABLE IF NOT EXISTS communication_participants ("
                + "uid BIGINT NOT NULL,"
                + "sid BIGINT NOT NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY(uid, sid),"
                + "FOREIGN KEY(uid) REFERENCES users(uid) ON DELETE CASCADE,"
                + "FOREIGN KEY(sid) REFERENCES senders(sid) ON DELETE CASCADE"
                + ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }catch (SQLException e) {
            System.out.println("Cannot create Communication Participants table: "+e.getMessage());
        }
    }

    private void createCypherMessagesTable(){
        String sql = "CREATE TABLE IF NOT EXISTS cypher_messages ("
                + "mid BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "sender BOOLEAN NOT NULL,"
                + "uid BIGINT NOT NULL,"
                + "sid BIGINT NOT NULL,"
                + "ciphertext BLOB NOT NULL,"
                + "iv VARBINARY(20) NOT NULL,"
                + "read_state BOOLEAN DEFAULT FALSE,"
                + "sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY(uid, sid) REFERENCES communication_participants(uid, sid) ON DELETE CASCADE"
                + ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }catch (SQLException e) {
            System.out.println("Cannot create Message table: "+e.getMessage());
        }
    }
    
    public String getHost() {
        return this.dbhost;
    }

    public String getPort() {
        return this.dbport;
    }

    public String getDbName() {
        return this.dbName;
    }

    public String getUser() {
        return this.dbuser;
    }

    public String getPassword() {
        return this.dbpassword;
    }
}
