/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

/**
 *
 * @author theunknown
 */
public class DatabaseInfo {
    // "localhost", "3306", "messagedb", "user", "1234"
    private String host;
    private String port;
    private String database;
    private String user;
    private String password;
    
    public void setHost(String host){
        this.host = host;
    }
    public void setPort(String Port){
        this.port= Port;
    }
    public void setDatabase(String database){
        this.database = database;
    }
    public void setUser(String User){
        this.user = User;
    }
    public void setPassword(String password){
        this.password = password;
    }
    
    public String getHost(){
        return this.host;
    }
    public String getPort(){
        return this.port;
    }
    public String getDatabase(){
        return this.database;
    }
    public String getUser(){
        return this.user;
    }
    public String getPassword(){
        return this.password;
    }
    
    
}
