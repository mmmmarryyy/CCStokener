package org.eclipse.osgi.internal.verifier;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

/**
 * Class to manage the different KeyStores we should check for certificates of
 * Signed JAR
 */
public class KeyStores {

    /**
	 * java.policy files properties of the java.security file
	 */
    private static final String JAVA_POLICY_URL = "policy.url.";

    /**
	 * Default keystore type in java.security file
	 */
    private static final String DEFAULT_KEYSTORE_TYPE = "keystore.type";

    /**
	 * List of KeyStores
	 */
    private List keyStores;

    /**
	 * KeyStores constructor comment.
	 */
    public KeyStores() {
        super();
        initializeDefaultKeyStores();
    }

    private void processKeyStore(String urlSpec, String type, URL rootURL) {
        if (type == null) type = KeyStore.getDefaultType();
        InputStream in = null;
        try {
            URL url;
            try {
                url = new URL(urlSpec);
            } catch (MalformedURLException mue) {
                url = new URL(rootURL, urlSpec);
            }
            KeyStore ks = KeyStore.getInstance(type);
            try {
                in = url.openStream();
            } catch (IOException ioe) {
            }
            if (in != null) {
                ks.load(in, null);
                keyStores.add(ks);
            }
        } catch (Exception e) {
            SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.WARNING, e);
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * populate the list of Keystores should be done with Dialog with
	 * Cancel/Skip button if the connection to the URL is down...
	 */
    private void initializeDefaultKeyStores() {
        keyStores = new ArrayList(5);
        String defaultType = Security.getProperty(DEFAULT_KEYSTORE_TYPE);
        String urlSpec = "file:" + FrameworkProperties.getProperty("java.home") + File.separator + "lib" + File.separator + "security" + File.separator + "cacerts";
        processKeyStore(urlSpec, defaultType, null);
        urlSpec = "file:" + FrameworkProperties.getProperty("user.home") + File.separator + ".keystore";
        processKeyStore(urlSpec, defaultType, null);
        urlSpec = FrameworkProperties.getProperty("osgi.framework.keystore");
        if (urlSpec != null) processKeyStore(urlSpec, defaultType, null);
        int index = 1;
        String java_policy = Security.getProperty(JAVA_POLICY_URL + index);
        while (java_policy != null) {
            processKeystoreFromLocation(java_policy);
            index++;
            java_policy = Security.getProperty(JAVA_POLICY_URL + index);
        }
    }

    /**
	 * retrieve the keystore from java.policy file
	 */
    private void processKeystoreFromLocation(String location) {
        InputStream in = null;
        char[] buff = new char[4096];
        int indexOf$ = location.indexOf("${");
        int indexOfCurly = location.indexOf('}', indexOf$);
        if (indexOf$ != -1 && indexOfCurly != -1) {
            String prop = FrameworkProperties.getProperty(location.substring(indexOf$ + 2, indexOfCurly));
            String location2 = location.substring(0, indexOf$);
            location2 += prop;
            location2 += location.substring(indexOfCurly + 1);
            location = location2;
        }
        try {
            URL url = new URL(location);
            in = url.openStream();
            Reader reader = new InputStreamReader(in);
            int result = reader.read(buff);
            StringBuffer contentBuff = new StringBuffer();
            while (result != -1) {
                contentBuff.append(buff, 0, result);
                result = reader.read(buff);
            }
            if (contentBuff.length() > 0) {
                String content = new String(contentBuff.toString());
                int indexOfKeystore = content.indexOf("keystore");
                if (indexOfKeystore != -1) {
                    int indexOfSemiColumn = content.indexOf(';', indexOfKeystore);
                    processKeystoreFromString(content.substring(indexOfKeystore, indexOfSemiColumn), url);
                    return;
                }
            }
        } catch (MalformedURLException e) {
            SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.WARNING, e);
        } catch (IOException e) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
	 * retrieve the keystore from java.policy file
	 */
    private void processKeystoreFromString(String content, URL rootURL) {
        String keyStoreType = null;
        int indexOfSpace = content.indexOf(' ');
        if (indexOfSpace == -1) return;
        int secondSpace = content.lastIndexOf(',');
        if (secondSpace == -1) {
            secondSpace = content.length();
        } else {
            keyStoreType = content.substring(secondSpace + 1, content.length()).trim();
        }
        processKeyStore(content.substring(indexOfSpace, secondSpace), keyStoreType, rootURL);
    }

    public boolean isTrusted(Certificate cert) {
        Iterator it = keyStores.iterator();
        while (it.hasNext()) {
            KeyStore ks = (KeyStore) it.next();
            try {
                if (ks.getCertificateAlias(cert) != null) {
                    return true;
                }
            } catch (KeyStoreException e) {
                SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.WARNING, e);
            }
        }
        return false;
    }
}
