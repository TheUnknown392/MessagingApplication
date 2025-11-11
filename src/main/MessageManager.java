/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

import java.util.concurrent.ConcurrentLinkedQueue;
/**
 *
 * @author theunknown
 */
public class MessageManager implements Runnable{
    private ConcurrentLinkedQueue<Message> messages;
    
    public MessageManager(ConcurrentLinkedQueue<Message> messages){
        this.messages = messages;
    }
    
    @Override
    public void run(){
        while(true){
            if(!messages.isEmpty()){
                Message message = messages.poll();
                System.out.println(message.getSenderInfo().username + ": " + message.getEncryptedMessage());
            }
        }
    }
}
