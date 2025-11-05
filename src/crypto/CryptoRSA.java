package crypto;

import database.UserInfo;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
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
    public static final int DEFAULT_KEY_SIZE = 2048;

    /**
     * Generate RSA key pair
     *
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
     * saves public key into the UserInfo and saves private key into a file
     * @param user
     * @param username
     * @throws IOException 
     */
    public void saveNewKeys(UserInfo user, String username) throws IOException {
        KeyPair key = generateKeyPair();
        // TODO: make a folder to store key of different users
        String filePath = System.getProperty("user.home") + "/." + "messagingAppication" + username + ".key";
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(privateKeyToString(key.getPrivate()));
        writer.close();

        user.setPublicKey(key.getPublic().getEncoded());

    }

    /**
     * Convert PublicKey to Base64 string
     *
     * @param publicKey
     * @return
     */
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    /**
     * Convert byte[] public key to Base64 string
     *
     * @param publicKey
     * @return
     */
    public static String publicKeyToString(byte[] publicKey) {
        return Base64.getEncoder().encodeToString(publicKey);
    }

    /**
     * Convert PrivateKey to Base64 string
     *
     * @param privateKey
     * @return
     */
    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * Convert Base64 string back to PublicKey
     *
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
     *
     * @param KeyString
     * @return
     */
    public PrivateKey getPrivateKeyFromString(String KeyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(KeyString);
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
     *
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
     *
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
     *
     * @param location
     * @return
     */
    public PublicKey loadPublicKeyFromFile(String location) {
        File inFile = new File(location);
        if (!inFile.exists()) {
            return null;
        }
        try (BufferedInputStream bin = new BufferedInputStream(new FileInputStream(inFile)); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
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
     *
     * @param location
     * @return
     */
    public PrivateKey loadPrivateKeyFromFile(String location) {
        File inFile = new File(location);
        if (!inFile.exists()) {
            return null;
        }
        try (BufferedInputStream bin = new BufferedInputStream(new FileInputStream(inFile)); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
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
    
    /**
     * converts public key to md5 fingerprint string
     * @param publicKeyBytes
     * @return 
     */
    public static String md5Fingerprint(byte[] publicKeyBytes){
        StringBuilder md5 = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(publicKeyBytes);
            
            md5 = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                md5.append(String.format("%02x", b));
            }
            return md5.toString();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CryptoRSA.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
