/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

/**
 *
 * @author theunknown
 */
public class NewConnectionAccepted {
    // TODO: rename this class with better name
    String username;
    String publicKey;
    String AES;
    
    // username:publicKey
    public NewConnectionAccepted(String key, String PREFIX){
        if (key != null && key.startsWith(PREFIX)) {
            String[] parts = key.substring(PREFIX.length()).split(":");
            this.username = parts[0];
            this.publicKey= parts[1];
            this.AES = parts[2];
        }
    }
    
    @Override
    public String toString(){
        return this.username + ":" + this.publicKey + ":" + this.AES;
    }
}
