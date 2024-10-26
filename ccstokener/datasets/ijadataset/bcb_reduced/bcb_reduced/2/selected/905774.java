package org.dbreplicator.repconsole;

import java.io.File;
import java.util.Properties;
import java.io.FileInputStream;
import org.dbreplicator.replication._Publication;
import org.dbreplicator.replication.RepException;
import org.dbreplicator.replication._ReplicationServer;
import org.dbreplicator.replication.ReplicationServer;
import org.dbreplicator.replication.RepConstants;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import java.security.*;
import java.io.ObjectInputStream;
import java.io.*;

public class PubConfiguration {

    private String pubDbDriver, pubDbURL, pubDbUser, pubDbPassword, keys;

    private String pubName;

    private String pubSysName;

    private String pubPort;

    private _ReplicationServer repPublication;

    private _Publication pub;

    private DesEncrypter desEncrypter;

    public PubConfiguration() {
        File f = new File("." + File.separator + "pubconfig.ini");
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(f));
            initialisePubInformation(p);
            repPublication = initialiseReplicationServer();
            pub = getPublication();
        } catch (Exception ex) {
            RepConstants.writeERROR_FILE(ex);
        }
    }

    private void initialisePubInformation(Properties p) {
        pubDbDriver = p.getProperty("DRIVER");
        pubDbURL = p.getProperty("URL");
        pubDbUser = p.getProperty("USER");
        pubDbPassword = p.getProperty("PASSWORD");
        pubDbPassword = decryptPassword(pubDbPassword);
        pubName = p.getProperty("PUBNAME");
        pubSysName = p.getProperty("SYSTEMNAME");
        pubPort = p.getProperty("PORT");
    }

    private _Publication getPublication() throws Exception {
        _Publication pub = repPublication.getPublication(pubName);
        if (pub == null) {
            throw new RepException("REP048", new Object[] { pubName });
        } else {
            return pub;
        }
    }

    private _ReplicationServer initialiseReplicationServer() throws RepException {
        _ReplicationServer rep = ReplicationServer.getInstance(Integer.parseInt(pubPort), pubSysName);
        rep.setDataSource(pubDbDriver, pubDbURL, pubDbUser, pubDbPassword);
        return rep;
    }

    private String decryptPassword(String password) {
        SecretKey sk = getSecretKey();
        desEncrypter = new DesEncrypter(sk);
        return desEncrypter.decrypt(password);
    }

    private SecretKey getSecretKey() {
        try {
            String path = "/org.dbreplicator/repconsole/secretKey.obj";
            java.net.URL url1 = getClass().getResource(path);
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(url1.openStream()));
            SecretKey sk = (SecretKey) ois.readObject();
            return sk;
        } catch (IOException ex) {
            RepConstants.writeERROR_FILE(ex);
        } catch (ClassNotFoundException ex) {
            RepConstants.writeERROR_FILE(ex);
        }
        return null;
    }

    public static void main(String[] args) {
        PubConfiguration pc = new PubConfiguration();
    }
}
