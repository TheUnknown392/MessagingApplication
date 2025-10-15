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

    String username;
    String ip;
    String port;

    public ConnectionKey(String username, String ip, String port) {
        this.username = username;
        this.ip = ip;
        this.port = port;
    }

    public ConnectionKey(String key, String PREFIX_CONNECT) {
        if (key != null && key.startsWith(PREFIX_CONNECT)) {
            String[] parts = key.substring(PREFIX_CONNECT.length()).split(":");
            this.username = parts[0];
            this.ip = parts[1];
            this.port = parts[2];
        } else {
            this.username = null;
            this.ip = null;
            this.port = null;
        }
    }

    @Override
    public String toString() {
        String toReturn = this.username + ":" + this.ip + ":" + this.port;
        return toReturn;
    }

}
