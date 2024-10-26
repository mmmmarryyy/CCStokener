package org.hardtokenmgmt.core.ejbca;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * @author Philip Vendil 2006-sep-02
 *
 * @version $Id$
 */
public class HTTPSTest {

    public static void main(String[] args) {
        String configName = "c:\\eclipse\\workspace\\hardtokenmgmt\\pkcs11.cfg";
        Provider p = new sun.security.pkcs11.SunPKCS11(configName);
        Security.addProvider(p);
        try {
            System.setProperty("javax.net.ssl.keyStoreType", "PKCS11");
            System.setProperty("javax.net.ssl.keyStore", "NONE");
            System.setProperty("javax.net.ssl.trustStore", "c:\\trust.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "6631");
            System.setProperty("javax.net.ssl.trustStorePassword", "foo123");
            KeyStore ks = KeyStore.getInstance("PKCS11");
            ks.load(null, "6631".toCharArray());
            String httpsURL = "https://localhost:8443/";
            URL url = new URL(httpsURL);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            InputStream ins = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(ins);
            BufferedReader in = new BufferedReader(isr);
            String inputLine;
            while ((inputLine = in.readLine()) != null) System.out.println(inputLine);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
