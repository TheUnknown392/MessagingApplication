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
    private String Message;
    private Boolean sentByUser;
    
    public Message(SenderInfo sender, String encrypted_message){
        this.sender = sender;
        this.Message = encrypted_message;
    }
    public Message(SenderInfo sender, String encrypted_message, Boolean sentByUser){
        this.sender = sender;
        this.Message = encrypted_message;
        this.sentByUser = sentByUser;
    }
    
    public SenderInfo getSenderInfo(){
        return sender;
    }
    
    public String getMessage(){
        return Message;
    }
    public Boolean sentByUser(){
        return sentByUser;
    }
}
