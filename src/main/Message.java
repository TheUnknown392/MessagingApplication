/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

import database.SenderInfo;
/**
 *
 * @author theunknown
 */
public class Message {
    private SenderInfo sender;
    private String encryptedMessage;
    
    public Message(SenderInfo sender, String encrypted_message){
        this.sender = sender;
        this.encryptedMessage = encrypted_message;
    }
    
    public SenderInfo getSenderInfo(){
        return sender;
    }
    
    public String getEncryptedMessage(){
        return encryptedMessage;
    }
}
