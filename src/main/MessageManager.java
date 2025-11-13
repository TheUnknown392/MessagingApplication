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
        System.out.println("from constructor " + messages.size());
    }
    
    @Override
    public void run(){
        while(true){
            if(!messages.isEmpty()){
                Message message = messages.poll();
                if(message == null){
                    System.out.println("it's null");
                }else{
                System.out.println(message.getSenderInfo().username + ": " + message.getEncryptedMessage().replace("//n","\n"));}
            }
        }
    }
}
