package org.hardtokenmgmt.core.logon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.logging.Level;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import org.hardtokenmgmt.core.InterfaceFactory;
import org.hardtokenmgmt.core.log.LocalLog;
import org.hardtokenmgmt.core.settings.GlobalSettings;

/**
 * Class responsible for managing the HTTPS logon
 * communication with the WS service.
 * 
 * 
 * @author Philip Vendil 3 feb 2008
 *
 * @version $Id$
 */
public class SoftTokenLogonManager {

    /**
	 * A ToLiMa specific SSL context manager, using the specially written
	 * ToLiMa trust and key managers.
	 * 
	 * @param token the IToken containing the admin certificate.
	 * @param pIN the PIN used to unlock the card
	 * @param logonGUICallback link to the GUI if a confirmation must be displayed.

	 */
    public static void initializeHTTPSContext(String keyStorePath, String password, ILogonGUICallback logonGUICallback) {
        try {
            KeyStore ks = getKeyStoreInstance(keyStorePath, password);
            TrustManager[] tolimaTrustManager = new TrustManager[] { new TolimaTrustManager(logonGUICallback) };
            X509Certificate logonCert = getLogonCert(ks);
            KeyManager[] keyManagers = new KeyManager[] { new TolimaKeyManager(ks, password, logonCert) };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(keyManagers, tolimaTrustManager, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (NoSuchAlgorithmException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (KeyStoreException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (IOException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (KeyManagementException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (CertificateException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        }
    }

    /**
	 * Method that finds the first end entity certificate and uses it.
	 * @param ks keystoer to use
	 * @return the first end entity certificate found in key store.
	 * @throws KeyStoreException 
	 */
    private static X509Certificate getLogonCert(KeyStore ks) throws KeyStoreException {
        X509Certificate retval = null;
        Enumeration<?> e = ks.aliases();
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            java.security.cert.Certificate cert = ks.getCertificate(alias);
            if (cert instanceof X509Certificate) {
                X509Certificate xCert = (X509Certificate) cert;
                if (xCert.getBasicConstraints() == -1) {
                    retval = xCert;
                    break;
                }
            }
        }
        return retval;
    }

    public static X509Certificate getLogonCert(String keyStorePath, String password) {
        X509Certificate retval = null;
        try {
            KeyStore ks = getKeyStoreInstance(keyStorePath, password);
            return getLogonCert(ks);
        } catch (CertificateException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (KeyStoreException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (FileNotFoundException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (IOException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        }
        return retval;
    }

    private static KeyStore getKeyStoreInstance(String keyStorePath, String password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
        KeyStore retval = null;
        String keyStorePathLower = keyStorePath.toLowerCase();
        if (keyStorePathLower.endsWith(".jks")) {
            retval = KeyStore.getInstance("JKS");
        }
        if (keyStorePathLower.endsWith(".p12") || keyStorePathLower.endsWith(".pfx")) {
            retval = KeyStore.getInstance("PKCS12");
        }
        if (retval == null) {
            throw new KeyStoreException("Error unsupported soft token, only JKS and P12 keystores can be used.");
        }
        retval.load(new FileInputStream(keyStorePath), password.toCharArray());
        return retval;
    }

    /**
	 * Method used to test if HTTPS is configured correctly.
	 * 
	 * @return true if connection was succesfull.
	 */
    public static boolean testConnection() {
        boolean retval = false;
        String connURL = InterfaceFactory.getGlobalSettings().getProperties().getProperty(GlobalSettings.EJBCAWS_URL);
        if (connURL == null || connURL.trim().equals("")) {
            connURL = InterfaceFactory.getGlobalSettings().getProperties().getProperty(GlobalSettings.WSRA_URL).trim();
        } else {
            connURL = connURL.trim();
        }
        try {
            URL url = new URL(connURL);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            if (br.readLine() != null) {
                retval = true;
            }
        } catch (MalformedURLException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error couldn't connect to WebService interface : " + e.getMessage(), e);
        } catch (IOException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error couldn't connect to WebService interface : " + e.getMessage(), e);
        }
        return retval;
    }

    public static boolean isValidKeyStore(String keyStorePath) {
        String keyStorePathLowerCase = keyStorePath.toLowerCase();
        if (!keyStorePathLowerCase.endsWith(".jks") && !keyStorePathLowerCase.endsWith(".p12") && !keyStorePathLowerCase.endsWith(".pfx")) {
            return false;
        }
        File keyStore = new File(keyStorePath);
        if (keyStore.exists() && keyStore.isFile() && keyStore.canRead()) {
            return true;
        }
        return false;
    }

    public static boolean checkPassword(String keyStorePath, String password) {
        boolean retval = true;
        try {
            getKeyStoreInstance(keyStorePath, password);
        } catch (Exception e) {
            retval = false;
        }
        return retval;
    }
}
