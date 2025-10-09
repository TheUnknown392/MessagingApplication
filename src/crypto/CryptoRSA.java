package crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author theunknown
 */
public class CryptoRSA {
    private static final String ALGORITHM = "RSA";
    private static final int DEFAULT_KEY_SIZE = 2048;
    
    /**
     * Generate RSA key pair
     * @return 
     */
public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGenerator.initialize(DEFAULT_KEY_SIZE, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CryptoRSA.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * Convert PublicKey to Base64 string
     * @param publicKey
     * @return 
     */
    public String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    /**
     * Convert PrivateKey to Base64 string
     * @param privateKey
     * @return 
     */
    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    
    /**
     * Convert Base64 string back to PublicKey
     * @param publicKeyString
     * @return 
     */
    public static PublicKey getPublicKeyFromString(String publicKeyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            try {
                return keyFactory.generatePublic(spec);
            } catch (InvalidKeySpecException ex) {
                Logger.getLogger(CryptoRSA.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CryptoRSA.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * Convert Base64 string back to PrivateKey
     * @param privateKeyString
     * @return
     */
    public PrivateKey getPrivateKeyFromString(String privateKeyString){
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            return keyFactory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CryptoRSA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(CryptoRSA.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * saves publicKey in location
     * @param location
     * @param publicKey
     * @return 
     */
    public boolean savePublicKey(String location, PublicKey publicKey) {
        byte[] data = publicKeyToString(publicKey).getBytes();
        File outFile = new File(location);
        try (BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(outFile))) {
            bout.write(data);
            bout.flush();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(CryptoRSA.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /**
     * saves privateKey in location
     * @param location
     * @param privateKey
     * @return 
     */
    public boolean savePrivateKey(String location, PrivateKey privateKey) {
        byte[] data = privateKeyToString(privateKey).getBytes();
        File outFile = new File(location);
        try (BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(outFile))) {
            bout.write(data);
            bout.flush();
            // TODO: Try to make the file accessable to owner only
            return true;
        } catch (IOException ex) {
            Logger.getLogger(CryptoRSA.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    /**
     * loads publickey from location
     * @param location
     * @return 
     */
    public PublicKey loadPublicKeyFromFile(String location) {
        File inFile = new File(location);
        if (!inFile.exists()) {
            return null;
        }
        try (BufferedInputStream bin = new BufferedInputStream(new FileInputStream(inFile));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = bin.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            String b64 = baos.toString();
            return getPublicKeyFromString(b64.trim());
        } catch (IOException ex) {
            Logger.getLogger(CryptoRSA.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * loads privateKey from location
     * @param location
     * @return 
     */
    public PrivateKey loadPrivateKeyFromFile(String location) {
        File inFile = new File(location);
        if (!inFile.exists()) {
            return null;
        }
        try (BufferedInputStream bin = new BufferedInputStream(new FileInputStream(inFile));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = bin.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            String b64 = baos.toString();
            return getPrivateKeyFromString(b64.trim());
        } catch (IOException ex) {
            Logger.getLogger(CryptoRSA.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
