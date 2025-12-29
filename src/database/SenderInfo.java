package database;

import crypto.CryptoRSA;
import java.sql.Timestamp;

/**
 *
 * @author theunknown
 */
public class SenderInfo {
    public SenderInfo(String username, String publicKey){
        CryptoRSA crypt = new CryptoRSA();
        this.username = username;
        this.PublicKey = crypt.getPublicKeyFromString(publicKey).getEncoded();
    }
    private int id;

    public String username;
    private Timestamp encounter_date;

    private byte[] PublicKey;
    private String md5;
    private byte[] aes_key;
    
    public int getId() {
        return this.id;
    }

    public boolean setFingerprint() {
        CryptoRSA crypt = new CryptoRSA();
        this.md5 = crypt.md5Fingerprint(this.PublicKey);
        return false;
    }
    
    public void setFingerprint(String fingerprint){
        this.md5 = fingerprint;
    }

    public String getFingerprint() {
        if(PublicKey!=null){
            setFingerprint();
        }
        return this.md5;
    }

    public String getUsername() {
        return this.username;
    }

    public Timestamp getCreatedDate() {
        return this.encounter_date;
    }

    public byte[] getEncryptedAES() {
        return this.aes_key;
    }

    public void setEncryptedAES(byte[] encrypted_aes) {
        this.aes_key = encrypted_aes;
    }

    public byte[] getPublicKey() {
        return this.PublicKey;
    }

    public void setPublicKey(byte[] publicKeyByte) {
        this.PublicKey = publicKeyByte;
    }
    public void setId(int sid){
        this.id = sid;
    }
    
    @Override
    public String toString(){
        return this.username;
    }
}
