package com.pallas.unicore.client.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.pallas.unicore.client.dialogs.AddToKeystoreDialog;
import com.pallas.unicore.client.dialogs.CertificateGeneralDialog;
import com.pallas.unicore.resourcemanager.ResourceManager;
import com.pallas.unicore.security.CertificateUtility;
import com.pallas.unicore.security.KeyStoreManager;
import com.pallas.unicore.utility.UserMessages;

/**
 * @author Thomas Kentemich
 * @version $Id: UnicorePlugin.java,v 1.1 2004/05/25 14:58:50 rmenday Exp $
 */
public class UnicorePlugin {

    private static Logger logger = Logger.getLogger("com.pallas.unicore.client.util");

    static ResourceBundle res = ResourceBundle.getBundle("com.pallas.unicore.client.util.ResourceStrings");

    private boolean dialogShow = false;

    private URLClassLoader loader = null;

    private String plugin;

    private Class pluginClass = null;

    private boolean trusted = false;

    private X509Certificate userCert = null;

    public UnicorePlugin(String plugin, String pluginClass, URL urls[], URLClassLoader urlLoader) {
        this.plugin = plugin;
        String error = "";
        try {
            trusted = checkJar(urls[0]);
        } catch (CertificateException ce) {
            error = res.getString("CERT_ERROR") + ce.getLocalizedMessage();
        } catch (NoSuchProviderException nspe) {
            error = res.getString("NO_PROVIDER") + nspe.getLocalizedMessage();
        } catch (IOException e) {
            error = e.getLocalizedMessage();
        } catch (Exception ne) {
            error = ne.getLocalizedMessage();
        }
        if (!trusted && !dialogShow) {
            String message = res.getString("SIGNATURE_FAILED") + urls[0];
            if (userCert != null) {
                String userDN = userCert.getSubjectDN().getName();
                userDN.replaceAll(",\t", "\n");
                userDN.replaceAll(", ", "\n");
                userDN.replaceAll(",", "\n");
                message = userDN + "\n\n" + message;
            }
            UserMessages.warning(message, error);
            return;
        } else {
            dialogShow = false;
        }
        this.loader = urlLoader;
        int end = pluginClass.indexOf(".class");
        String theClass = pluginClass.substring(0, end);
        theClass = theClass.replace('/', '.');
        logger.info("Loading class: " + theClass);
        try {
            this.pluginClass = loader.loadClass(theClass);
        } catch (ClassNotFoundException cnfe) {
            UserMessages.error(res.getString("CANNOT_LOAD"), cnfe.getLocalizedMessage());
            logger.log(Level.SEVERE, "", cnfe);
        }
    }

    private boolean checkJar(URL url) throws CertificateException, NoSuchProviderException, IOException, Exception {
        JarEntry next;
        boolean signatureFound = false;
        boolean acceptAllSigner = true;
        userCert = null;
        InputStream is = url.openConnection().getInputStream();
        JarInputStream jis = new JarInputStream(is);
        while ((next = jis.getNextJarEntry()) != null) {
            Certificate[] chain = next.getCertificates();
            if (chain != null) {
                for (int i = 0; i < chain.length; i++) {
                    X509Certificate x509 = (X509Certificate) chain[i];
                }
            }
            if (next.getName().endsWith("RSA")) {
                signatureFound = true;
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                Collection coll = cf.generateCertificates(jis);
                if (coll == null || coll.size() < 1) {
                    UserMessages.error("Plugin " + url + res.getString("NOT SIGNED"));
                    return false;
                }
                X509Certificate userChain[] = CertificateUtility.toX509Chain(coll);
                userCert = userChain[0];
                userCert.checkValidity();
                boolean acceptSigner = false;
                boolean userCertVerified = false;
                KeyStoreManager manager = ResourceManager.getKeystoreManager();
                String signer = manager.getAliasFromCertifcate(userCert);
                if (signer != null) {
                    acceptSigner = true;
                    userCertVerified = true;
                    logger.info("Plugin signer known as: " + signer);
                } else {
                    userCertVerified = manager.verify(userChain);
                    if (!userCertVerified) {
                        CertificateGeneralDialog dialog = new CertificateGeneralDialog(userChain, url.toString());
                        dialog.show();
                        this.dialogShow = true;
                    }
                }
                if (!acceptSigner && userCertVerified) {
                    AddToKeystoreDialog dialog = new AddToKeystoreDialog(ResourceManager.getCurrentInstance(), url, userCert);
                    dialog.show();
                    this.dialogShow = true;
                    if (dialog.getResult() == AddToKeystoreDialog.ACCEPT) {
                        manager.addTrustedCertificate(userCert, true);
                        manager.reWriteKeyStore();
                        acceptSigner = true;
                    } else if (dialog.getResult() == AddToKeystoreDialog.ONLY_FOR_SESSION) {
                        acceptSigner = true;
                    }
                }
                acceptAllSigner = acceptAllSigner && acceptSigner;
            }
            jis.closeEntry();
        }
        jis.close();
        if (!signatureFound) {
            throw new IOException(res.getString("PLUGIN_JAR") + url + res.getString("NOT_SIGNED"));
        }
        return signatureFound && acceptAllSigner;
    }

    public Class getPluginClass() {
        return pluginClass;
    }

    public URLClassLoader getPluginClassloader() {
        return loader;
    }

    public String getPluginName() {
        return plugin;
    }

    public boolean isTrusted() {
        return trusted;
    }
}
