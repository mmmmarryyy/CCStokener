package org.hardtokenmgmt.core.logon;

import iaik.pkcs.pkcs11.TokenException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
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
import org.hardtokenmgmt.core.token.IToken;
import org.hardtokenmgmt.core.token.OperationNotSupportedException;
import org.hardtokenmgmt.core.token.PKCS11Factory;
import org.hardtokenmgmt.core.util.CertUtils;

/**
 * Class responsible for managing the HTTPS logon
 * communication with the WS service.
 * 
 * 
 * @author Philip Vendil 3 feb 2008
 *
 * @version $Id$
 */
public class Pkcs11LogonManager {

    /**
	 * A ToLiMa specific SSL context manager, using the specially written
	 * ToLiMa trust and key managers.
	 * 
	 * @param token the IToken containing the admin certificate.
	 * @param pIN the PIN used to unlock the card
	 * @param logonGUICallback link to the GUI if a confirmation must be displayed.
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 */
    public static void initializeHTTPSContext(IToken token, String pIN, ILogonGUICallback logonGUICallback) {
        try {
            String pkcs11Name = PKCS11Factory.getPKCS11Name().trim();
            KeyStore ks = createPKCS11Provider(token.getSlotId(IToken.PINTYPE_BASIC), pkcs11Name, pIN);
            TrustManager[] tolimaTrustManager = new TrustManager[] { new TolimaTrustManager(logonGUICallback) };
            X509Certificate logonCert = InterfaceFactory.getTokenManager().getLogonCertificateSelector().getLogonCertificate(token);
            KeyManager[] keyManagers = new KeyManager[] { new TolimaKeyManager(ks, pIN, logonCert) };
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
        } catch (UnrecoverableKeyException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (KeyStoreException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (IOException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (KeyManagementException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (CertificateException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (OperationNotSupportedException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (TokenException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        }
    }

    /**
	 * A ToLiMa specific SSL context manager, used for the autologon page
	 * especially.
	 * 
	 * @param slotId the slot containing the admin certificate.
	 * @param pIN the PIN used to unlock the card. 
	 * @param logonGUICallback link to the GUI if a confirmation must be displayed.
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 */
    public static X509Certificate initializeAutoLogonHTTPSContext(long slotId, String pkcs11Name, String pIN, ILogonGUICallback logonGUICallback) {
        X509Certificate retval = null;
        try {
            KeyStore ks = createPKCS11Provider(slotId, pkcs11Name, pIN);
            TrustManager[] tolimaTrustManager = new TrustManager[] { new TolimaTrustManager(logonGUICallback) };
            String dnMatch = InterfaceFactory.getGlobalSettings().getProperties().getProperty("token.defaultlogoncertselector");
            String altDNMatch = InterfaceFactory.getGlobalSettings().getProperties().getProperty("token.defaultlogoncertselector.altdn", "CN=notused");
            dnMatch = CertUtils.stringToBCDNString(dnMatch).trim();
            altDNMatch = CertUtils.stringToBCDNString(altDNMatch).trim();
            X509Certificate logonCert = null;
            Enumeration<String> e = ks.aliases();
            while (e.hasMoreElements()) {
                String nextAlias = e.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(nextAlias);
                if (cert != null) {
                    if (cert.getKeyUsage()[0]) {
                        if (dnMatch.equals(CertUtils.getIssuerDN(cert)) || altDNMatch.equals(CertUtils.getIssuerDN(cert))) {
                            logonCert = cert;
                            break;
                        }
                    }
                }
            }
            if (logonCert == null) {
                LocalLog.getLogger().log(Level.SEVERE, "Error couldn't find logon certificate that matches issuer DN  : " + dnMatch, e);
            }
            retval = logonCert;
            KeyManager[] keyManagers = new KeyManager[] { new TolimaKeyManager(ks, pIN, logonCert) };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(keyManagers, tolimaTrustManager, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                public boolean verify(String string, javax.net.ssl.SSLSession session) {
                    return true;
                }
            });
        } catch (NoSuchAlgorithmException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (UnrecoverableKeyException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (KeyStoreException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (IOException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (KeyManagementException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (CertificateException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (OperationNotSupportedException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        } catch (TokenException e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error initializing HTTPS context : " + e.getMessage(), e);
        }
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

    private static KeyStore createPKCS11Provider(long slotId, String pkcs11Name, String kspassword) throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, OperationNotSupportedException, TokenException {
        InputStream is;
        StringBuffer cardConfig = new StringBuffer();
        cardConfig.append("name = ToLiMaLogon\n");
        cardConfig.append("library = " + pkcs11Name + "\n");
        cardConfig.append("slot = " + slotId);
        is = new ByteArrayInputStream(cardConfig.toString().getBytes());
        Provider pkcs11 = null;
        try {
            Class<?> pkcs11Class = Class.forName("sun.security.pkcs11.SunPKCS11");
            Constructor<?> c = pkcs11Class.getConstructor(new Class[] { InputStream.class });
            pkcs11 = (Provider) c.newInstance(new Object[] { is });
        } catch (Exception e) {
            LocalLog.getLogger().log(Level.SEVERE, "Error instantiating the PKCS11 provider : " + e.getMessage(), e);
        }
        Security.removeProvider("SunPKCS11-ToLiMaLogon");
        Security.addProvider(pkcs11);
        KeyStore ks = KeyStore.getInstance("PKCS11");
        ks.load(null, kspassword == null ? null : kspassword.toCharArray());
        return ks;
    }
}
