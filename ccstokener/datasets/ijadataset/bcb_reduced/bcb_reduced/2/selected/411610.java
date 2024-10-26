package com.wuala.server.common.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.security.KeyPair;
import java.security.PrivateKey;
import com.wuala.loader2.crypto.AsymmetricCrypto;
import com.wuala.loader2.crypto.AsymmetricPrivateKey;
import com.wuala.server.common.varia.ApplicationRuntimeException;

public class ServerKeyLoader {

    protected static final String SERVER_PRIVATE_KEY_FILE = "PrivateRSAKey.sjo";

    private static AsymmetricPrivateKey privateKey;

    public static AsymmetricPrivateKey getPrivateKey() throws IOException {
        if (privateKey == null) {
            URL url = ServerKeyLoader.class.getResource(SERVER_PRIVATE_KEY_FILE);
            privateKey = new AsymmetricPrivateKey((PrivateKey) readObject(url));
        }
        return privateKey;
    }

    private static Object readObject(URL url) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(url.openStream());
        try {
            try {
                return ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new ApplicationRuntimeException(e);
            }
        } finally {
            ois.close();
        }
    }

    protected static void writeObject(File file, Object o) throws IOException {
        ObjectOutputStream oin = new ObjectOutputStream(new FileOutputStream(file));
        try {
            oin.writeObject(o);
        } finally {
            oin.close();
        }
    }

    public static final void main(String[] args) throws IOException {
        KeyPair keyPair = AsymmetricCrypto.getInstance().generateKeyPair();
        writeObject(new File(SERVER_PRIVATE_KEY_FILE), keyPair.getPrivate());
        writeObject(new File("PubKey.sjo"), keyPair.getPublic());
    }
}
