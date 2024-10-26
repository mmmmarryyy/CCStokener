package org.dbreplicator.repconsole;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.File;
import org.dbreplicator.replication._ReplicationServer;
import org.dbreplicator.replication.ReplicationServer;
import org.dbreplicator.replication.RepException;
import org.dbreplicator.replication._Subscription;
import org.dbreplicator.replication.RepConstants;
import javax.crypto.SecretKey;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

public class Subconfiguration {

    private String subDbDriver, subDbURL, subDbUser, subDbPassword;

    private String subName;

    private String portNumber;

    private String subSysName;

    private String pubName;

    private String pubSysName;

    private String pubPortNumber;

    private String replicationType;

    _ReplicationServer repSubscription;

    _Subscription sub;

    private DesEncrypter desEncrypter;

    public Subconfiguration() {
        try {
            File f = new File("." + File.separator + "subconfig.ini");
            Properties p = new Properties();
            p.load(new FileInputStream(f));
            initialiseSubAndPubInformation(p);
            repSubscription = initialiseReplicationServer();
            sub = getSubscription(subName, pubName);
            performReplication(sub, replicationType);
        } catch (Exception ex) {
            RepConstants.writeERROR_FILE(ex);
            System.out.println(" Problem occured in pull replication. For details see the error.lg");
        }
    }

    private void performReplication(_Subscription sub, String replicationType) throws RepException {
        try {
            sub.pull();
            System.out.println("Pulled  successfully ");
        } finally {
            System.exit(1);
        }
    }

    private void initialiseSubAndPubInformation(Properties p) {
        subDbDriver = p.getProperty("DRIVER");
        subDbURL = p.getProperty("URL");
        subDbUser = p.getProperty("USER");
        subDbPassword = p.getProperty("PASSWORD");
        subDbPassword = decryptPassword(subDbPassword);
        subName = p.getProperty("SUBNAME");
        portNumber = p.getProperty("PORT");
        subSysName = p.getProperty("SYSTEMNAME");
        pubName = p.getProperty("PUBNAME");
        pubSysName = p.getProperty("PUB_SYSTEMNAME");
        pubPortNumber = p.getProperty("PUB_PORT");
        replicationType = p.getProperty("REPLICATIONTYPE");
    }

    private _ReplicationServer initialiseReplicationServer() throws RepException {
        _ReplicationServer repSubscription = ReplicationServer.getInstance(Integer.parseInt(portNumber), subSysName);
        repSubscription.setDataSource(subDbDriver, subDbURL, subDbUser, subDbPassword);
        return repSubscription;
    }

    private _Subscription getSubscription(String subName, String pubName) throws RepException {
        _Subscription sub = repSubscription.getSubscription(subName);
        if (sub == null) {
            sub = repSubscription.createSubscription(subName, pubName);
            sub.setRemoteServerPortNo(Integer.parseInt(pubPortNumber));
            sub.setRemoteServerUrl(pubSysName);
            try {
                sub.subscribe();
            } catch (RepException ex) {
                throw ex;
            }
        } else {
            sub.setRemoteServerPortNo(Integer.parseInt(pubPortNumber));
            sub.setRemoteServerUrl(pubSysName);
        }
        return sub;
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
        Subconfiguration sc = new Subconfiguration();
    }
}
