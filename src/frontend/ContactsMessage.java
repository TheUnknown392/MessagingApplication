/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

import database.SenderInfo;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author theunknown
 */
public class ContactsMessage {
    // TODO: 
    ConcurrentHashMap<SenderInfo, LinkedList<String>> contactMessages = new ConcurrentHashMap<>();
}
