package es.eucm.eadventure.editor.control.security.jarsigner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.util.jar.JarFile;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class encapsulates paramaters for jarsigner most of which are usually
 * given in command line.
 */
class JSParameters {

    /**
     * Default location of the keystore. Used when the value is not supplied by
     * the user.
     */
    public static final String defaultKeystorePath = System.getProperty("user.home") + File.separator + ".keystore";

    /**
     * The name of the logger for JarSigner.
     */
    public static final String loggerName = "org.apache.harmony.tools.jarsigner.JarSignerLogger";

    private KeyStore keyStore;

    private JarFile jarFile;

    private String jarURIPath;

    private String jarName;

    private String alias;

    private boolean isVerify;

    private String storeURI;

    private String storeType = KeyStore.getDefaultType();

    private char[] storePass;

    private char[] keyPass;

    private String sigFileName;

    private String signedJARName;

    private boolean isCerts;

    private boolean isVerbose;

    private boolean isInternalSF;

    private boolean isSectionsOnly;

    private String provider;

    private String providerName;

    private String certProvider;

    private String certProviderName;

    private String sigProvider;

    private String sigProviderName;

    private String ksProvider;

    private String ksProviderName;

    private String mdProvider;

    private String mdProviderName;

    private URI tsaURI;

    private String tsaCertAlias;

    private String altSigner;

    private String altSignerPath;

    private String helpTopic;

    private boolean isSFNameProcessed;

    private String keyAlg;

    private String sigAlg;

    private boolean isSilent;

    private String proxy;

    private int proxyPort;

    private Proxy.Type proxyType;

    boolean nullStream = false;

    boolean token = false;

    void setDefault() {
        keyStore = null;
        jarFile = null;
        jarName = null;
        jarURIPath = null;
        alias = null;
        storeURI = null;
        storeType = KeyStore.getDefaultType();
        storePass = null;
        keyPass = null;
        sigFileName = null;
        signedJARName = null;
        isVerify = false;
        isCerts = false;
        isVerbose = false;
        isInternalSF = false;
        isSectionsOnly = false;
        provider = null;
        providerName = null;
        certProvider = null;
        certProviderName = null;
        sigProvider = null;
        sigProviderName = null;
        ksProvider = null;
        ksProviderName = null;
        mdProvider = null;
        mdProviderName = null;
        tsaURI = null;
        tsaCertAlias = null;
        altSigner = null;
        altSignerPath = null;
        helpTopic = null;
        isSFNameProcessed = false;
        keyAlg = null;
        sigAlg = null;
        isSilent = false;
        proxy = null;
        proxyPort = 8888;
        proxyType = Proxy.Type.HTTP;
    }

    /**
     * Original name JAR
     * @return
     */
    public String getJarName() {
        return jarName;
    }

    /**
     * @param alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @param altSigner
     */
    public void setAltSigner(String altSigner) {
        this.altSigner = altSigner;
    }

    /**
     * @param altSignerPath
     */
    public void setAltSignerPath(String altSignerPath) {
        this.altSignerPath = altSignerPath;
    }

    /**
     * @param certProvider
     */
    public void setCertProvider(String certProvider) {
        this.certProvider = certProvider;
    }

    /**
     * @param certProviderName
     */
    public void setCertProviderName(String certProviderName) {
        this.certProviderName = certProviderName;
    }

    /**
     * @param helpTopic
     */
    public void setHelpTopic(String helpTopic) {
        this.helpTopic = helpTopic;
    }

    /**
     * @param isCerts
     */
    public void setCerts(boolean isCerts) {
        this.isCerts = isCerts;
    }

    /**
     * @param isInternalSF
     */
    public void setInternalSF(boolean isInternalSF) {
        this.isInternalSF = isInternalSF;
    }

    /**
     * @param isSectionsOnly
     */
    public void setSectionsOnly(boolean isSectionsOnly) {
        this.isSectionsOnly = isSectionsOnly;
    }

    /**
     * @param isVerbose
     */
    public void setVerbose(boolean isVerbose) {
        if (!isSilent) {
            Logger logger = Logger.getLogger(loggerName);
            Handler[] handlers = logger.getHandlers();
            for (Handler handler : handlers) {
                if (isVerbose) {
                    logger.setLevel(Level.FINE);
                    handler.setLevel(Level.FINE);
                } else {
                    logger.setLevel(Level.INFO);
                    handler.setLevel(Level.INFO);
                }
            }
        }
        this.isVerbose = isVerbose;
    }

    /**
     * @param isVerify
     */
    public void setVerify(boolean isVerify) {
        this.isVerify = isVerify;
    }

    /**
     * @param jarFile
     */
    public void setJarFile(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * @param jarURIPath
     */
    public void setJarURIorPath(String jarURIPath) {
        this.jarURIPath = jarURIPath;
    }

    /**
     * @param keyAlg
     */
    void setKeyAlg(String keyAlg) {
        this.keyAlg = keyAlg;
    }

    /**
     * @param keyPass
     */
    public void setKeyPass(char[] keyPass) {
        this.keyPass = keyPass;
    }

    /**
     * @param keyStore
     */
    void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * @param ksProvider
     */
    public void setKsProvider(String ksProvider) {
        this.ksProvider = ksProvider;
    }

    /**
     * @param ksProviderName
     */
    public void setKsProviderName(String ksProviderName) {
        this.ksProviderName = ksProviderName;
    }

    /**
     * @param mdProvider
     */
    public void setMdProvider(String mdProvider) {
        this.mdProvider = mdProvider;
    }

    /**
     * @param mdProviderName
     */
    public void setMdProviderName(String mdProviderName) {
        this.mdProviderName = mdProviderName;
    }

    /**
     * @param provider
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * @param providerName
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    /**
     * @param proxy 
     */
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    /**
     * @param proxyPort 
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * @param proxyType 
     */
    public void setProxyType(Proxy.Type proxyType) {
        this.proxyType = proxyType;
    }

    /**
     * @param sigAlg
     */
    void setSigAlg(String sigAlg) {
        this.sigAlg = sigAlg;
    }

    /**
     * @param sigFileName
     */
    public void setSigFileName(String sigFileName) {
        this.jarName = sigFileName;
        this.sigFileName = sigFileName;
        isSFNameProcessed = false;
    }

    /**
     * @param signedJARName
     */
    public void setSignedJARName(String signedJARName) {
        this.signedJARName = signedJARName;
    }

    /**
     * @param sigProvider
     */
    public void setSigProvider(String sigProvider) {
        this.sigProvider = sigProvider;
    }

    /**
     * @param sigProviderName
     */
    public void setSigProviderName(String sigProviderName) {
        this.sigProviderName = sigProviderName;
    }

    /**
     * @param isSilent
     */
    public void setSilent(boolean isSilent) {
        Logger logger = Logger.getLogger(loggerName);
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            if (isSilent) {
                logger.setLevel(Level.OFF);
            } else {
                if (isVerbose) {
                    logger.setLevel(Level.FINE);
                    handler.setLevel(Level.FINE);
                } else {
                    logger.setLevel(Level.INFO);
                    handler.setLevel(Level.INFO);
                }
            }
        }
        this.isSilent = isSilent;
    }

    /**
     * @param storePass
     */
    public void setStorePass(char[] storePass) {
        this.storePass = storePass;
    }

    /**
     * @param storeType
     */
    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    /**
     * @param storeURI
     */
    public void setStoreURI(String storeURI) {
        this.storeURI = storeURI;
    }

    /**
     * @param tsaCertAlias
     */
    public void setTsaCertAlias(String tsaCertAlias) {
        this.tsaCertAlias = tsaCertAlias;
    }

    /**
     * @param tsaURI
     */
    public void setTsaURI(URI tsaURI) {
        this.tsaURI = tsaURI;
    }

    /**
     * @return
     */
    String getAlias() {
        return alias;
    }

    /**
     * @return
     */
    String getAltSigner() {
        return altSigner;
    }

    /**
     * @return
     */
    String getAltSignerPath() {
        return altSignerPath;
    }

    /**
     * @return
     */
    String getCertProvider() {
        return certProvider;
    }

    /**
     * @return
     */
    String getCertProviderName() {
        return certProviderName;
    }

    /**
     * @return
     */
    String getHelpTopic() {
        return helpTopic;
    }

    /**
     * @return
     */
    boolean isCerts() {
        return isCerts;
    }

    /**
     * @return
     */
    boolean isInternalSF() {
        return isInternalSF;
    }

    /**
     * @return
     */
    boolean isSectionsOnly() {
        return isSectionsOnly;
    }

    /**
     * @return
     */
    boolean isSilent() {
        return isSilent;
    }

    /**
     * @return
     */
    boolean isVerbose() {
        return isVerbose;
    }

    /**
     * @return
     */
    boolean isVerify() {
        return isVerify;
    }

    /**
     * @return
     * @throws IOException 
     */
    JarFile getJarFile() throws IOException {
        if (jarFile == null) {
            try {
                File file;
                try {
                    URI jarURI = new URI(jarURIPath);
                    file = new File(jarURI);
                } catch (URISyntaxException e) {
                    file = new File(jarURIPath);
                } catch (IllegalArgumentException e) {
                    file = new File(jarURIPath);
                }
                jarFile = new JarFile(file, isVerify);
            } catch (IOException e) {
                throw (IOException) new IOException("Failed to load JAR file " + jarURIPath).initCause(e);
            }
        }
        return jarFile;
    }

    /**
     * @return
     */
    String getJarURIorPath() {
        return jarURIPath;
    }

    /**
     * @return
     */
    String getKeyAlg() {
        return keyAlg;
    }

    /**
     * @return
     */
    char[] getKeyPass() {
        return keyPass;
    }

    /**
     * @return
     * @throws JarSignerException
     */
    KeyStore getKeyStore() throws JarSignerException {
        if (keyStore == null) {
            KeyStore store = null;
            if (providerName == null) {
                try {
                    store = KeyStore.getInstance(this.storeType);
                } catch (KeyStoreException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    store = KeyStore.getInstance(storeType, providerName);
                } catch (KeyStoreException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                }
            }
            if (storeURI == null) {
                throw new JarSignerException("Cannot load the keystore " + " error con el keystore");
            }
            try {
                storeURI = storeURI.replace(File.separatorChar, '/');
                URL url = null;
                try {
                    url = new URL(storeURI);
                } catch (java.net.MalformedURLException e) {
                    url = new File(storeURI).toURI().toURL();
                }
                InputStream is = null;
                try {
                    is = url.openStream();
                    store.load(is, storePass);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (Exception e) {
                throw new JarSignerException("Cannot load the keystore " + storeURI, e);
            }
            keyStore = store;
        }
        return keyStore;
    }

    /**
     * @return
     */
    String getKsProvider() {
        return ksProvider;
    }

    /**
     * @return
     */
    String getKsProviderName() {
        return ksProviderName;
    }

    /**
     * @return
     */
    String getMdProvider() {
        return mdProvider;
    }

    /**
     * @return
     */
    String getMdProviderName() {
        return mdProviderName;
    }

    /**
     * @return
     */
    String getProvider() {
        return provider;
    }

    /**
     * @return
     */
    String getProviderName() {
        return providerName;
    }

    /**
     * @return 
     */
    String getProxy() {
        return proxy;
    }

    /**
     * @return 
     */
    int getProxyPort() {
        return proxyPort;
    }

    /**
     * @return 
     */
    Proxy.Type getProxyType() {
        return proxyType;
    }

    /**
     * @return
     */
    String getSigAlg() {
        return sigAlg;
    }

    /**
     * @return
     */
    String getSigFileName() {
        if (!isSFNameProcessed) {
            sigFileName = FileNameGenerator.generateFileName(sigFileName, alias);
            isSFNameProcessed = true;
        }
        return sigFileName;
    }

    /**
     * @return
     */
    String getSignedJARName() {
        return signedJARName;
    }

    /**
     * @return
     */
    String getSigProvider() {
        return sigProvider;
    }

    /**
     * @return
     */
    String getSigProviderName() {
        return sigProviderName;
    }

    /**
     * @return
     */
    char[] getStorePass() {
        return storePass;
    }

    /**
     * @return
     */
    String getStoreType() {
        if (storeType == null) {
            storeType = KeyStore.getDefaultType();
        }
        return storeType;
    }

    /**
     * @return
     */
    String getStoreURI() {
        return storeURI;
    }

    /**
     * @return
     */
    String getTsaCertAlias() {
        return tsaCertAlias;
    }

    /**
     * @return
     */
    URI getTsaURI() {
        return tsaURI;
    }
}
