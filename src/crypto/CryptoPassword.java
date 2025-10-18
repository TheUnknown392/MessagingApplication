/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package crypto;

import database.UserInfo;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 *
 * @author theunknown
 */
public class CryptoPassword {

    private final int AES_LEN = 128;
    public boolean debug;

    public CryptoPassword(boolean debug) {
        this.debug = debug;
    }

    /**
     * returns false if password is correct
     *
     * @param user
     * @param password
     * @return
     */
    public boolean verifyPassword(UserInfo user, String password) {
        try {
            byte[] salt = user.getSalt();
            byte[] expectedHash = user.getPasswordHashed();

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 310_000, AES_LEN);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] testHash = skf.generateSecret(spec).getEncoded();
            return !Arrays.equals(expectedHash, testHash);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CryptoPassword.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(CryptoPassword.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * set hashed password and its salt into correct property of UserInfo
     *
     * @param user
     * @param password
     */
    public void setPasswordHashedSalt(UserInfo user, String password) {
        try {
            CryptoMessage crypt = new CryptoMessage(this.debug);
            byte[] salt = crypt.generateSalt();

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 310_000, AES_LEN);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] passwordHashed = skf.generateSecret(spec).getEncoded();

            user.setSalt(salt);
            user.setPasswordHashed(passwordHashed);

        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CryptoPassword.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(CryptoPassword.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
