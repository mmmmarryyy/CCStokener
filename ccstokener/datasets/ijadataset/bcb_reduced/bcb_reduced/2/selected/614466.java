package javax.crypto;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.jar.*;

/**
 * This class verifies JAR files (and any supporting JAR files), and
 * determines whether they may be used in this implementation.
 *
 * The JCE in OpenJDK has an open cryptographic interface, meaning it
 * does not restrict which providers can be used.  Compliance with
 * United States export controls and with local law governing the
 * import/export of products incorporating the JCE in the OpenJDK is
 * the responsibility of the licensee.
 *
 * @since 1.7
 */
final class JarVerifier {

    private URL jarURL;

    private boolean savePerms;

    private CryptoPermissions appPerms = null;

    /**
     * Creates a JarVerifier object to verify the given URL.
     *
     * @param jarURL the JAR file to be verified.
     * @param savePerms if true, save the permissions allowed by the
     *          exemption mechanism
     */
    JarVerifier(URL jarURL, boolean savePerms) {
        this.jarURL = jarURL;
        this.savePerms = savePerms;
    }

    /**
     * Verify the JAR file is signed by an entity which has a certificate
     * issued by a trusted CA.
     *
     * In OpenJDK, we just need to examine the "cryptoperms" file to see
     * if any permissions were bundled together with this jar file.
     */
    void verify() throws JarException, IOException {
        if (!savePerms) {
            return;
        }
        final URL url = jarURL.getProtocol().equalsIgnoreCase("jar") ? jarURL : new URL("jar:" + jarURL.toString() + "!/");
        JarFile jf = null;
        try {
            try {
                jf = (JarFile) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                    public Object run() throws Exception {
                        JarURLConnection conn = (JarURLConnection) url.openConnection();
                        conn.setUseCaches(false);
                        return conn.getJarFile();
                    }
                });
            } catch (java.security.PrivilegedActionException pae) {
                SecurityException se = new SecurityException("Cannot load " + url.toString());
                se.initCause(pae);
                throw se;
            }
            if (jf != null) {
                JarEntry je = jf.getJarEntry("cryptoPerms");
                if (je == null) {
                    throw new JarException("Can not find cryptoPerms");
                }
                try {
                    appPerms = new CryptoPermissions();
                    appPerms.load(jf.getInputStream(je));
                } catch (Exception ex) {
                    JarException jex = new JarException("Cannot load/parse" + jarURL.toString());
                    jex.initCause(ex);
                    throw jex;
                }
            }
        } finally {
            if (jf != null) {
                jf.close();
            }
        }
    }

    /**
     * Verify that the provided certs include the
     * framework signing certificate.
     *
     * @param certs the list of certs to be checked.
     * @throws Exception if the list of certs did not contain
     *          the framework signing certificate
     */
    static void verifyPolicySigned(java.security.cert.Certificate[] certs) throws Exception {
    }

    /**
     * Returns the permissions which are bundled with the JAR file,
     * aka the "cryptoperms" file.
     *
     * NOTE: if this JarVerifier instance is constructed with "savePerms"
     * equal to false, then this method would always return null.
     */
    CryptoPermissions getPermissions() {
        return appPerms;
    }
}
