/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

/**
 *
 * @author theunknown
 */
public class ConnectionKey {
    public String md5;
    String ip;
    String port;
    
    public ConnectionKey(String md5){
        this.md5 = md5;
        this.ip = "";
        this.port = "";
    }

    public ConnectionKey(String md5, String ip, String port) {
        this.md5 = md5;
        this.ip = ip;
        this.port = port;
    }

    public ConnectionKey(String key, String PREFIX_CONNECT) {
        if(PREFIX_CONNECT == null){
            String[] parts = key.split(":");
            this.md5 = parts[0];
            this.ip = parts[1];
            this.port = parts[2];
            return;
        }
        
        if (key != null && key.startsWith(PREFIX_CONNECT)) {
            String[] parts = key.substring(PREFIX_CONNECT.length()).split(":");
            this.md5 = parts[0];
            this.ip = parts[1];
            this.port = parts[2];
        } else {
            this.md5 = null;
            this.ip = null;
            this.port = null;
        }
    }

    @Override
    public String toString() {
        String toReturn = this.md5 + ":" + this.ip + ":" + this.port;
        return toReturn;
    }

}
