/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package crypto;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.AEADBadTagException;

/**
 *
 * @author theunknown
 */
public class CryptoMessage {

    private final int AES_LEN = 128;
    private boolean debug;

    public CryptoMessage(boolean debug) {
        this.debug = debug;
    }

    /**
     * generates Salt
     * @return 
     */
    public byte[] generateSalt() {
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        if (debug) {
            System.out.println("Salt created");
        }
        return salt;
    }

    /**
     * generates IV
     * @return 
     */
    private byte[] generateIV() {
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        if (debug) {
            System.out.println("iv created");
        }
        random.nextBytes(iv);
        return iv;
    }

    /**
     * generates AES from password
     * @param password
     * @param salt
     * @return 
     */
    public SecretKey getAESKeyFromPassword(String password, byte[] salt) {
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 310_000, AES_LEN);

            try {
                if (debug) {
                    System.out.println("AES form Password created");
                }
                return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            } catch (InvalidKeySpecException ex) {
                if (debug) {
                    System.err.println("AES form Password not created, InvalidKeySecException");
                }

                Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (NoSuchAlgorithmException ex) {
                if (debug) {
                    System.err.println("AES form Password not created, NoSuchAlgorithmException");
                }
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    /**
     * stores AES key to location
     * @param location
     * @param aesKey 
     */
    public void storeAESkey(String location, SecretKey aesKey){
        try(FileOutputStream output = new FileOutputStream(location)) {
            output.write(aesKey.getEncoded());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * gets saved AES key from location
     * @param location
     * @return 
     */
    public SecretKey readAESkey(String location){
        try {
            byte[] aesByte = Files.readAllBytes(Paths.get(location));
            return new SecretKeySpec(aesByte,"AES");
        } catch (IOException ex) {
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * gets AES SecretKey from aesByte
     * @param location
     * @return 
     */
    public SecretKey readAESkey(byte[] aesByte){
        return new SecretKeySpec(aesByte,"AES");
    }

    /**
     * encrypts plaintext with aesKey 
     * @param plaintext
     * @param aesKey
     * @return
     */
    public EncryptedMessage encryptMessage(String plaintext, SecretKey aesKey) {

        try {
           byte[] iv = new byte[12];
            iv = generateIV(); 
            
            // AES Encrypt
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(AES_LEN, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
            byte[] cipherText = cipher.doFinal(plaintext.getBytes());
            
            return new EncryptedMessage(cipherText, iv);
        } catch (Exception ex) {
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * decrypts encryptedMessage with aesKey
     * @param encryptedMessage
     * @param aesKey
     * @return 
     */
    public String decryptMessage(EncryptedMessage encryptedMessage, SecretKey aesKey){

        try {
            // 1. Initialize cipher with IV
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(AES_LEN, encryptedMessage.iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

            // 2. Decrypt
            try {
                byte[] plainBytes = cipher.doFinal(encryptedMessage.cypherText);
                return new String(plainBytes);
            } catch (AEADBadTagException e) {
                System.out.println("IV, salt or password are incorrect: " + e.getMessage());
                System.exit(2);
            } catch (GeneralSecurityException e) {
                System.out.println("Some security Error: " + e.getMessage());
                System.exit(3);
            }
        } catch (Exception ex) {
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
