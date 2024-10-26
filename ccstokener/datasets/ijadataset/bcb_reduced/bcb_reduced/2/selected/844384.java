package org.rg.common.util.security;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * provides basic encodeing and decoding of passwords.
 * @author redman
 */
public class AESUtil {

    private static final Log log = LogFactory.getLog("Security");

    /** Temporary storage of the raw secret key to unlock the encrypted DB passwords */
    public String s_SAVED_RAW = null;

    /** AES algorithm key */
    private static final String s_AES_ALG = "AES";

    /** Key used to identify the encoded data in maps */
    public static final String s_ENCODED_DATA = "aesencodeddata";

    /** Key used to identify the encoded key in maps */
    public static final String s_ENCODED_KEY = "aesencodedkey";

    /** The singleton instance of this utility class */
    private static AESUtil s_INSTANCE = null;

    /**
    * Static accessor to get the singleton instance of this utility.
    * @return The singleton reference to this utility class
    */
    public static AESUtil getInstance() {
        if (s_INSTANCE == null) {
            String seedname = System.getProperty("aes.seed");
            if (seedname == null) seedname = "/ld2c.txt";
            URL url = AESUtil.class.getResource(seedname);
            DataInputStream dis;
            String key = null;
            try {
                dis = new DataInputStream(url.openConnection().getInputStream());
                key = dis.readLine();
                dis.close();
            } catch (IOException e) {
                log.fatal("The file, ld2c.txt, but the encryption seed could not be read from that file.");
                System.exit(-1);
            } catch (Throwable e) {
                log.fatal("The seed required for encryption was not found. This encryption " + "seed should be in a file in the application working directory named ld2c.txt.");
                System.exit(-1);
            }
            s_INSTANCE = new AESUtil(key);
        }
        return s_INSTANCE;
    }

    /**
    * we have to have a seed, it is expected to be passed in.
    * @param key the encription seed.
    */
    private AESUtil(String key) {
        if (key == null) {
            log.fatal("Can't use a null encryption seed.");
            System.exit(-1);
        }
        s_SAVED_RAW = key;
    }

    /**
    * Call to decode encrypted data that has been encoded in Hex. The key used will be the default that is stored in the
    * utility temporarily. In the future, perhaps it will be read in or definable.
    * @param encodedHex The data to be decoded and decrypted
    * @return The un-encoded data in the form of a <code>byte[]</code>
    */
    public byte[] decodeHex(String encodedHex) {
        return decodeHex(encodedHex, s_SAVED_RAW);
    }

    /**
    * Call to decode encrypted data that has been encoded in Hex with a specific key.
    * @param encodedHex The data to be decoded and decrypted
    * @param hexKey The String format of the key that is encoded in hex
    * @return The un-encoded data in the form of a <code>byte[]</code>
    */
    public byte[] decodeHex(String encodedHex, String hexKey) {
        try {
            byte[] raw = Hex.decodeHex(hexKey.toCharArray());
            SecretKeySpec skeySpec = new SecretKeySpec(raw, s_AES_ALG);
            Cipher cipher = Cipher.getInstance(s_AES_ALG);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] original = cipher.doFinal(Hex.decodeHex(encodedHex.toCharArray()));
            return original;
        } catch (Exception e) {
            log.error("AESUtil: Error in decodeHex ", e);
            return null;
        }
    }

    /**
    * Call to encrypt the specified bytes and encode the value as Hex. Utilizes the dfault key to perform the
    * encryption.
    * @param data The <code>byte[]</code> of data to be encrypted
    * @return A <code>Map</code> that has the encoded data and encoded key as values.
    */
    public Map<String, String> encodeToHex(byte[] data) {
        try {
            return encodeToHex(data, Hex.decodeHex(s_SAVED_RAW.toCharArray()));
        } catch (DecoderException e) {
            System.out.println("Error in encodeToHex(arg) : " + e);
            e.printStackTrace();
            return null;
        }
    }

    /**
    * Call to encrypt the specified bytes and encode the value as Hex while using the specified key.
    * @param data The <code>byte[]</code> of data to be encrypted
    * @param key The <code>byte[]</code> that represents the key to the encryption
    * @return A <code>Map</code> that has the encoded data and encoded key as values.
    */
    public Map<String, String> encodeToHex(byte[] data, byte[] key) {
        try {
            byte[] raw = key;
            if (raw == null) {
                KeyGenerator kgen = KeyGenerator.getInstance(s_AES_ALG);
                kgen.init(128);
                SecretKey skey = kgen.generateKey();
                raw = skey.getEncoded();
            }
            String encodedKey = new String(Hex.encodeHex(raw));
            SecretKeySpec skeySpec = new SecretKeySpec(raw, s_AES_ALG);
            Cipher cipher = Cipher.getInstance(s_AES_ALG);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(data);
            String encodedData = new String(Hex.encodeHex(encrypted));
            Map<String, String> retMap = new HashMap<String, String>(2);
            retMap.put(s_ENCODED_KEY, encodedKey);
            retMap.put(s_ENCODED_DATA, encodedData);
            return retMap;
        } catch (Exception ex) {
            System.out.println("AESUtil - error in encodeToHex : " + ex);
            ex.printStackTrace();
            return null;
        }
    }

    /**
    * This program generates a AES key, retrieves its raw bytes, and then reinstantiates a AES key from the key bytes.
    * The reinstantiated key is used to initialize a AES cipher for encryption and decryption.
    * @param args the arguments.
    * @throws Exception
    */
    public static void main(String[] args) throws Exception {
        String message = "mr_roboto";
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128);
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        String savedRaw = "9b15a842cc4ef2bd744008eba11b7a50";
        System.out.println("Saved raw is " + savedRaw);
        raw = Hex.decodeHex(savedRaw.toCharArray());
        System.out.println("raw hex is " + new String(Hex.encodeHex(raw)));
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal((args.length == 0 ? message : args[0]).getBytes());
        System.out.println("encrypted string: " + new String(Hex.encodeHex(encrypted)));
        skeySpec = new SecretKeySpec(raw, "AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] original = cipher.doFinal(encrypted);
        String originalString = new String(original);
        System.out.println("Original string: " + originalString + " " + new String(Hex.encodeHex(original)));
    }
}
