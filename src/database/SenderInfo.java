package database;

import crypto.CryptoRSA;
import java.sql.Timestamp;
import java.util.Base64;

/**
 *
 * @author theunknown
 */
public class SenderInfo {
    //            String sql = "CREATE TABLE IF NOT EXISTS senders ("
//                + "sid BIGINT PRIMARY KEY AUTO_INCREMENT,"
//                + "username TEXT NOT NULL,"
//                + "public_key VARBINARY(600) NOT NULL,"
//                + "encrypted_aes_key VARBINARY(600),"
//                + "encounter_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
//                + ");";

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
}
