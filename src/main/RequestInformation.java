/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

/**
 *
 * @author theunknown
 */
public class RequestInformation {
//    PREFIX_REQUEST_INFORMATION + user.username + ":" + user.publicKey + ":" + getLocalIp() + ":" + this.port;

    public String username;
    public String publicKey;
    public String ip;
    public String port;

    public RequestInformation(String key, String PREFIX) {
        if (key != null && key.startsWith(PREFIX)) {
            String[] parts = key.substring(PREFIX.length()).split(":");
            this.username = parts[0];
            this.publicKey = parts[1];
            this.ip = parts[2];
            this.port = parts[3];
        }
    }
    @Override
    public String toString(){
        return this.username + ":" + this.publicKey + ":" + this.ip + ":" + this.port;
    }
}
