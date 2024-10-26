package edu.ucsd.ncmir.spl.filesystem;

import edu.ucsd.ncmir.spl.utilities.Base64;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

/**
 *
 * @author spl
 */
class ServerInfoFactory {

    static ServerInfoFactory getInstance() {
        return new ServerInfoFactory();
    }

    ServerInfo getServerInfo(String key, String protocol) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IOException, ClassNotFoundException, IllegalBlockSizeException, BadPaddingException {
        DESedeKeySpec ks = new DESedeKeySpec(Base64.decode(key));
        SecretKeyFactory skf = SecretKeyFactory.getInstance("DESede");
        SecretKey sk = skf.generateSecret(ks);
        Cipher cipher = Cipher.getInstance("DESede");
        cipher.init(Cipher.DECRYPT_MODE, sk);
        ClassLoader cl = this.getClass().getClassLoader();
        URL url = cl.getResource(protocol + ".sobj");
        JarURLConnection jc = (JarURLConnection) url.openConnection();
        ObjectInputStream os = new ObjectInputStream(jc.getInputStream());
        SealedObject so = (SealedObject) os.readObject();
        return (ServerInfo) so.getObject(cipher);
    }
}
