
package database;

import java.sql.Timestamp;
import crypto.CryptoRSA;
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
        
        public String getMd5(){
            CryptoRSA crypt = new CryptoRSA();
            return crypt.md5Fingerprint(this.PublicKey);
        }
    }
