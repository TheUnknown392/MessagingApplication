package crypto;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CryptoMessage {
    private static final int GCM_LEN = 128;
    private boolean debug;

    public CryptoMessage(boolean debug) {
        this.debug = debug;
    }

    public CryptoMessage() {
        this.debug = false;
    }

    public byte[] generateSalt() {
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        if (debug) System.out.println("Salt created");
        return salt;
    }

    private static byte[] generateIV() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    public SecretKey getAESKeyFromPassword(String password, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 310_000, GCM_LEN);
            if (debug) System.out.println("AES from Password created");
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (Exception ex) {
            if (debug) System.err.println("AES from Password failed: " + ex.getMessage());
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public byte[] getAESKeyBytesFromPassword(String password, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 310_000, GCM_LEN);
            if (debug) System.out.println("AES key bytes created");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            if (debug) System.err.println("AES key bytes failed: " + ex.getMessage());
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void storeAESkey(String location, SecretKey aesKey) {
        try (FileOutputStream output = new FileOutputStream(location)) {
            output.write(aesKey.getEncoded());
        } catch (IOException ex) {
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public SecretKey readAESkey(String location) {
        try {
            byte[] aesByte = Files.readAllBytes(Paths.get(location));
            return new SecretKeySpec(aesByte, "AES");
        } catch (IOException ex) {
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public SecretKey readAESkey(byte[] aesByte) {
        return new SecretKeySpec(aesByte, "AES");
    }

    
    public static String encryptMessage(String plaintext, byte[] aesKeyBytes) {
        try {
            byte[] iv = generateIV();
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_LEN, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            
            byte[] combined = new byte[cipherText.length + iv.length];
            System.arraycopy(cipherText, 0, combined, 0, cipherText.length);           
            System.arraycopy(iv, 0, combined, cipherText.length, iv.length);            
            
            System.out.println("Message encrypted successfully");
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            System.err.println("Encryption failed: " + ex.getMessage());
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static String decryptMessage(String base64Combined, byte[] aesKeyBytes) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64Combined);
            SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            int ivLength = 16;
            
            if (combined.length < ivLength) return null;
            
            byte[] iv = new byte[ivLength];
            byte[] cipherText = new byte[combined.length - ivLength];
            
            
            System.arraycopy(combined, 0, cipherText, 0, cipherText.length);              
            System.arraycopy(combined, cipherText.length, iv, 0, ivLength);               
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_LEN, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
            
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static String decryptMessage(byte[] iv, byte[] byteSeperatedMessage, byte[] aesKeyBytes) {
        try {
            SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_LEN, iv);
            
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
            byte[] plainBytes = cipher.doFinal(byteSeperatedMessage);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Logger.getLogger(CryptoMessage.class.getName()).log(Level.SEVERE, "Split decryption failed", ex);
            return null;
        }
    }

    public static byte[] safeBase64Decode(String rawAes) {
        return Base64.getDecoder().decode(rawAes.trim().replaceAll("[\\r\\n]", ""));
    }
}
