/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import java.sql.Timestamp;

/**
 *
 * @author theunknown
 */
public class UserInfo{
        public int id;
        public String username;
        public Timestamp created_at;
        
        private byte[] password_hashed;
        private byte[]  Salt;
        private byte[] PublicKey;
        
        public int getId(){
            return this.id;
        }
        
        public String getUsername(){
            return this.username;
        }
        
        public Timestamp getCreatedDate(){
            return this.created_at;
        }
        
        public byte[] getPasswordHashed(){
            return this.password_hashed;
        }
        
        public byte[] getSalt(){
            return this.Salt;
        }
        
        public byte[] getPublicKey(){
            return this.PublicKey;
        }
        
        public void setSalt(byte[] salt){
            this.Salt = salt;
        }
        
        public void setPasswordHashed(byte[] passwordHashed){
            this.password_hashed = passwordHashed;
        }
        
        public void setPublicKey(byte[] publicKeyByte){
            this.PublicKey = publicKeyByte;
        }
    }
