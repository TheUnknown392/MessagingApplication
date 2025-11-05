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
    private byte[] encrypted_aes_key;

    public int getId() {
        return this.id;
    }

    public boolean setFingerprint() {
        if (this.PublicKey == null) {
            return true;
        }
        CryptoRSA crypt = new CryptoRSA();
        this.md5 = crypt.md5Fingerprint(this.PublicKey);
        return false;
    }

    public String getFingerpring() {
        if(PublicKey==null){
            if(setFingerprint()){
                return null;
            }
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
        return this.encrypted_aes_key;
    }

    public void setEncryptedAES(byte[] encrypted_aes) {
        this.encrypted_aes_key = encrypted_aes;
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
}
