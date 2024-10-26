package edu.psu.its.lionshare.security;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cdc.standard.CDCStandardProvider;
import codec.pkcs10.CertificationRequest;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.settings.StringSetting;
import com.limegroup.gnutella.settings.LongSetting;
import edu.internet2.middleware.shibboleth.common.Constants;
import edu.internet2.middleware.shibboleth.common.Trust;
import edu.internet2.middleware.shibboleth.common.provider.ShibbolethTrust;
import edu.internet2.middleware.shibboleth.metadata.AttributeAuthorityDescriptor;
import edu.internet2.middleware.shibboleth.metadata.Endpoint;
import edu.internet2.middleware.shibboleth.metadata.EndpointManager;
import edu.internet2.middleware.shibboleth.metadata.EntitiesDescriptor;
import edu.internet2.middleware.shibboleth.metadata.EntityDescriptor;
import edu.internet2.middleware.shibboleth.metadata.Metadata;
import edu.internet2.middleware.shibboleth.metadata.RoleDescriptor;
import edu.internet2.middleware.shibboleth.metadata.provider.XMLMetadataProvider;
import edu.internet2.middleware.shibboleth.xml.Parser;
import edu.psu.its.lionshare.security.authorization.AssertionValidator;
import edu.psu.its.lionshare.settings.LionShareApplicationSettings;
import edu.psu.sasl_ca.ClientProtocolHandler;
import edu.psu.sasl_ca.ClientProtocolHandlerException;
import edu.psu.sasl_ca.jaas_login;
import edu.psu.sasl_ca.util.GenKeys;
import edu.psu.sasl_ca.util.StringConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.SAMLAttributeDesignator;
import org.opensaml.SAMLAttributeQuery;
import org.opensaml.SAMLBinding;
import org.opensaml.SAMLBindingFactory;
import org.opensaml.SAMLException;
import org.opensaml.SAMLNameIdentifier;
import org.opensaml.SAMLSOAPHTTPBinding;
import org.opensaml.SAMLSubject;
import org.opensaml.SAMLRequest;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLSOAPHTTPBinding.HTTPHook;
import org.opensaml.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SecurityManager prompts the user for credentials, generates keying material
 * and obtains certificates from a SASL-CA. It also loads and manages the
 * federation metadata and the metadata signing cert.
 *
 * @author Geoffrey Schulman
 * @author Lorin Metzger
 * @author Derek Morr (derekmorr@psu.edu)
 * @author Mark Earnest (mxe202psu.edu)
 */
public class DefaultSecurityManager implements SecurityManager {

    private static final Log LOG = LogFactory.getLog(SecurityManager.class);

    private String email = "";

    private String principal = "";

    private static DefaultSecurityManager instance = null;

    private boolean authenticated = false;

    private static ClientProtocolHandler cph = null;

    private static Subject subj = null;

    private TrustServerCallback callback = null;

    private AuthenticateUserCallback aucallback = null;

    private ArrayList listeners;

    private static final Object THREAD_LOCK = new Object();

    private boolean authenticating = false;

    private boolean loaded_from_disk = false;

    private String keyAlgorithm = "RSA";

    private int keySize = 1024;

    private static final String signatureAlgorithm = "SHA1withRSA";

    private static final long refreshInterval = 30 * 60 * 1000;

    /** The metadata for the federation */
    private Metadata federationMetadata;

    private static final String trustStorePassword = "changeit";

    private static final String trustStoreType = "JCEKS";

    /** The set of {@link TrustAnchors} for metadata signing */
    private HashSet<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();

    /** Dummy requestor name for all LS peers */
    private static final String requestor = "urn:mace:psu.edu:lionshare";

    /** The endpoint mapping for the LS endpoint on the AA */
    private static final String lsEndPointName = "/LS";

    /** A List of the SAML AA endpoints for a given IdP */
    private Collection<Endpoint> endPoints = new ArrayList<Endpoint>();

    /** The AA's role descriptor */
    private AttributeAuthorityDescriptor aaDescriptor;

    /** Shibboleth trust validator - used for "RA" checks on the AA's cert */
    private Trust trustProvider = new ShibbolethTrust();

    /** We can share the AssertionValidator between multiple users, so keep a copy here. */
    private AssertionValidator assertionValidator = null;

    /** true is using self-signed certs rather than SAML for authZ */
    private boolean usingSelfSignedCert = false;

    /** the self-signed cert */
    private X509Certificate selfSignedCert;

    /** self-signed cert's key */
    private PrivateKey selfSignedPrivateKey;

    /** timer object for refreshing identity certs **/
    private Timer identityRefreshTimer;

    private DefaultSecurityManager() {
        this.listeners = new ArrayList<SecurityManagerListener>();
        this.copyPrefsFile(LionShareApplicationSettings.JAVA_SECURITY_TRUSTANCHOR_KEYSTORE, LionShareApplicationSettings.TRUSTANCHOR_KEYSTORE_LAST_MODIFIED);
        System.setProperty("org.apache.axis.components.net.SecureSocketFactory", "edu.psu.its.lionshare.security.SecureSocketFactoryImpl");
        this.updateFederationMetadata();
        this.parseFederationMetadata();
        this.assertionValidator = new AssertionValidator(this.federationMetadata, this.trustProvider);
        this.setSAMLIdP(LionShareApplicationSettings.IDP_URN.getValue());
        System.setProperty("javax.net.ssl.trustStore", LionShareApplicationSettings.JAVA_SECURITY_TRUSTANCHOR_KEYSTORE.getValue());
        System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        this.loadTrustAnchors(LionShareApplicationSettings.JAVA_SECURITY_TRUSTANCHOR_KEYSTORE.getValue());
        this.checkForSelfSignedCert();
    }

    /** 
     * Copy a file from the LS jar into the user's preferences directory, 
     * if it's not already present. The LionShareApplicationSetting value 
     * for the path name is then updated.
     *
     * @param setting The LionShareApplicationSetting to update
     */
    private void copyPrefsFile(final StringSetting setting, final LongSetting last_modified) {
        String filename = setting.getValue().trim();
        if (!filename.startsWith("etc")) {
            setting.revertToDefault();
            filename = setting.getValue().trim();
        }
        int idx = filename.lastIndexOf(java.io.File.separator);
        if (idx == -1) {
            LOG.debug("idx == -1");
            idx = filename.lastIndexOf("/");
        }
        String trimmedFile = filename.substring(idx, filename.length());
        java.io.File fileHandle = new java.io.File(CommonUtils.getUserSettingsDir().getAbsolutePath() + trimmedFile);
        try {
            Thread a = Thread.currentThread();
            if (!a.isAlive()) {
                LOG.debug("Thread is not alive!");
            }
            ClassLoader c = a.getContextClassLoader();
            URL filePath = c.getResource(filename);
            if (filePath == null) {
                LOG.debug("[NULL]trust.keystore file path: " + filePath.toString());
                return;
            }
            URLConnection url_connect = filePath.openConnection();
            if (!fileHandle.exists()) {
                LOG.debug("no handles!!!");
            }
            if (!fileHandle.exists() || (url_connect.getLastModified() != last_modified.getValue())) {
                LOG.debug("trying to copy from: " + filePath);
                LOG.debug("trying to copy to  : " + fileHandle);
                FileOutputStream out = new FileOutputStream(fileHandle);
                java.io.InputStream in = url_connect.getInputStream();
                byte[] bytes = new byte[4096];
                int read = -1;
                do {
                    read = in.read(bytes);
                    if (read != -1) {
                        out.write(bytes, 0, read);
                    }
                } while (read != -1);
                out.flush();
                out.close();
                in.close();
            }
            last_modified.setValue(url_connect.getLastModified());
            setting.setValue(fileHandle.getAbsolutePath());
            LionShareApplicationSettings.instance().getFactory().save();
        } catch (Exception e) {
            LOG.debug("Reasons for errors 1 : " + e.getMessage());
            LOG.debug("Reasons for errors 2 : " + e.getLocalizedMessage());
            LOG.debug("Reasons for errors 3 : " + e.toString());
            LOG.trace("Unable to copy ", e);
        }
    }

    /**
     * Get the filename of the metadata file on-disk.
     * 
     * @return the name of the on-disk file; <code>null</code> on error.
     */
    private File getMetadataFilename() {
        if (LionShareApplicationSettings.FEDERATION_METADATA_URL.getValue() == null) {
            LOG.error("Unable to refresh federation metadata - no URL set for metadata source");
            return null;
        }
        try {
            URL url = new URL(LionShareApplicationSettings.FEDERATION_METADATA_URL.getValue());
            File mdFile = new File(url.getFile());
            String lsPrefsDir = CommonUtils.getUserSettingsDir().getAbsolutePath();
            mdFile = new File(lsPrefsDir + File.separator + mdFile.getName());
            return mdFile;
        } catch (Exception ex) {
            LOG.error("Error obtaining metadata file, using default: ", ex);
            return null;
        }
    }

    /**
     * Fetch a new copy of the federation metadata.
     */
    public synchronized void updateFederationMetadata() {
        try {
            URL url = new URL(LionShareApplicationSettings.FEDERATION_METADATA_URL.getValue());
            long fileUpdateTime = 0;
            File mdFile = this.getMetadataFilename();
            if (mdFile.exists()) {
                fileUpdateTime = mdFile.lastModified();
            }
            URLConnection conn = null;
            try {
                conn = url.openConnection();
                conn.setIfModifiedSince(fileUpdateTime);
                conn.connect();
            } catch (Exception exp) {
                LOG.error("Could load read updated federation metadata", exp);
                url = Thread.currentThread().getContextClassLoader().getResource("edu/psu/its/lionshare/security/InCommon-metadata.xml");
                conn = url.openConnection();
                conn.setIfModifiedSince(fileUpdateTime);
                conn.connect();
            }
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                int responseCode = httpConn.getResponseCode();
                if (responseCode != 200) {
                    LOG.error("Unable to refresh federation metadata - received: " + httpConn.getResponseMessage());
                    return;
                }
            }
            int contentLength = conn.getContentLength();
            if (contentLength > 0) {
                LOG.info("Updating federation metadata file with new version from : " + new Date(conn.getLastModified()));
                BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                FileOutputStream fos = new FileOutputStream(mdFile);
                byte[] buffer = new byte[20480];
                int count;
                while ((count = bis.read(buffer)) > -1) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
            }
            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).disconnect();
            }
        } catch (Exception ex) {
            LOG.fatal("Unable to update federation metadata", ex);
        }
    }

    public static synchronized DefaultSecurityManager getInstance() {
        if (instance == null) {
            instance = new DefaultSecurityManager();
        }
        return instance;
    }

    public void initialize(Object parameter) {
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isAuthenticating() {
        return authenticating;
    }

    private void setAuthenticating(boolean auth) {
        authenticating = auth;
    }

    public synchronized void unAuthenticate() {
        authenticated = false;
        email = null;
        principal = null;
        fireUpdate();
        KeystoreManager.getInstance().unAuthenticate();
    }

    public void setLoadedCertificatesFromDisk(boolean disk) {
        loaded_from_disk = disk;
    }

    public boolean isCertificateLoadedFromDisk() {
        return loaded_from_disk;
    }

    /**
     * Authenticate a user to the SASL-CA and obtain credentials.
     *
     * @param parameter Must be an instance of {@link CallbackHandler} or the method will abort.
     *
     * @return <code>true</code> if the user authenticated, otherwise <code>false</code>.
     */
    public boolean authenticate(Object parameter) {
        CallbackHandler handler = null;
        if (parameter instanceof CallbackHandler) {
            handler = (CallbackHandler) parameter;
        } else {
            return false;
        }
        synchronized (THREAD_LOCK) {
            if (isAuthenticating()) {
                return false;
            }
            setAuthenticating(true);
        }
        try {
            if (authenticated == true) {
                return authenticated;
            }
            Security.addProvider(new CDCStandardProvider());
            subj = jaas_login.doLogin(handler);
            String prin = "";
            java.util.Set prins = subj.getPrincipals();
            for (Iterator i = prins.iterator(); i.hasNext(); ) {
                Principal princ = ((Principal) i.next());
                prin = princ.getName();
                break;
            }
            principal = prin;
            aucallback.authenticationInProgress("<html><b>Obtaining certificate for:</b><p>\n<p align=center>" + prin + "</html>");
            String secLayerString = "auth";
            Map<String, String> secLayerMap = this.setupSaslSecurityLayer(secLayerString);
            String hostname = LionShareApplicationSettings.SASL_CA_HOSTNAME.getValue();
            int port = LionShareApplicationSettings.SASL_CA_PORT.getValue();
            cph = new ClientProtocolHandler(hostname, port, handler, secLayerMap);
            KeyPair identityKeyPair = GenKeys.generateKeyPair(keyAlgorithm, keySize);
            KeyPair opaqueKeyPair = GenKeys.generateKeyPair(keyAlgorithm, keySize);
            CertificationRequest identityCSR = cph.generateCSR(StringConstants.identityCertIdentifier, identityKeyPair, signatureAlgorithm);
            CertificationRequest opaqueCSR = cph.generateCSR(StringConstants.opaqueCertIdentifier, opaqueKeyPair, signatureAlgorithm);
            cph.addCSR(identityCSR);
            cph.addCSR(opaqueCSR);
            Map<CertificationRequest, X509Certificate[]> certMap = (Map<CertificationRequest, X509Certificate[]>) Subject.doAs(subj, cph);
            X509Certificate[] identityCerts = certMap.get(identityCSR);
            LOG.debug("adding identity keypair");
            KeystoreManager.getInstance().addKey(identityKeyPair.getPrivate(), identityCerts, false);
            this.email = SubjectDNParser.getPrincipal(identityCerts[0].getSubjectX500Principal().toString());
            X509Certificate[] opaqueCerts = certMap.get(opaqueCSR);
            LOG.debug("adding opaque keypair");
            KeystoreManager.getInstance().addKey(opaqueKeyPair.getPrivate(), opaqueCerts, true);
            authenticated = true;
            aucallback.authenticationSuccessful(SubjectDNParser.getUserData(identityCerts[0].getSubjectX500Principal().toString()).getName());
            this.identityRefreshTimer = new Timer("identity credential refresh", false);
            Date renewalTime = new Date(identityCerts[0].getNotAfter().getTime() - refreshInterval);
            LOG.debug("scheduling identity credential renewal for " + renewalTime);
            this.identityRefreshTimer.schedule(new IdentityCertRefresher(), renewalTime);
        } catch (java.net.ConnectException ce) {
            aucallback.authenticationFailed("The the SASL-CA appears to be down \n" + ce.getMessage() + "\n\n" + "Please e-mail support@lionshare.its.psu.edu\n\n");
            authenticated = false;
        } catch (javax.security.auth.login.LoginException le) {
            if (le.getMessage().equals("Clock skew too great (37) - PREAUTH_FAILED")) {
                aucallback.authenticationFailed("Login Failed! Time Skew To Great.\n " + "Please set your computer clock to " + " the current data/time\n");
            } else if (le.getMessage().equals("Integrity check on decrypted field" + " failed (31) - PREAUTH_FAILED")) {
                aucallback.authenticationFailed("Login failed! Invalid password" + " provided for username.\n");
            } else if (le.getMessage().equals("Client not found in Kerberos database" + " (6) - CLIENT_NOT_FOUND")) {
                aucallback.authenticationFailed("Login failed! Invalid username" + " provided.\n");
            } else if (le.getMessage().equals("Client's entry in database expired" + " (1) - CLIENT EXPIRED")) {
                aucallback.authenticationFailed("Login failed! User account has " + "expired.\n");
            } else {
                aucallback.authenticationFailed("Login failed invalid for unknown" + " reason\n" + le.getMessage() + ".\n");
            }
            LOG.trace("", le);
            authenticated = false;
        } catch (Exception e) {
            LOG.trace("", e);
            aucallback.authenticationFailed("UnExpected Exception while trying to" + " authenticate:\n" + e + "\n\n" + "Please contact support@lionshare.its.psu.edu");
            authenticated = false;
            LOG.trace(" ", e);
        } finally {
        }
        fireUpdate();
        setAuthenticating(false);
        return authenticated;
    }

    public Object getCredentials(Object params) {
        SAMLNameIdentifier nameID = new SAMLNameIdentifier();
        nameID.setFormat(Constants.SHIB_NAMEID_FORMAT_URI);
        nameID.setNameQualifier(LionShareApplicationSettings.IDP_URN.getValue());
        String handle = "null";
        List parameters = null;
        if (params instanceof DefaultCredentialRequestParameters) {
            handle = ((DefaultCredentialRequestParameters) params).getGUID();
            parameters = ((DefaultCredentialRequestParameters) params).getAttributes();
        } else {
            try {
                X509Certificate cert = KeystoreManager.getInstance().getCertificate(true);
                handle = SubjectDNParser.getCryptoShibHandle(cert.getSubjectX500Principal().toString());
                LOG.debug("HANDLE:" + handle);
                parameters = (List) params;
            } catch (Exception e) {
                LOG.trace("", e);
            }
        }
        nameID.setName(handle);
        List designator = new ArrayList();
        if (parameters != null && parameters instanceof List) {
            for (Object attributeDesignator : (List) parameters) {
                LOG.debug((String) attributeDesignator);
                SAMLAttributeDesignator tempDesignator = new SAMLAttributeDesignator();
                tempDesignator.setNamespace(Constants.SHIB_ATTRIBUTE_NAMESPACE_URI);
                tempDesignator.setName((String) attributeDesignator);
                designator.add(tempDesignator);
            }
        }
        SAMLSubject subject = new SAMLSubject();
        try {
            subject.setNameIdentifier(nameID);
            SAMLAttributeQuery query = new SAMLAttributeQuery(subject, requestor, designator);
            SAMLRequest samlRequest = new SAMLRequest(query);
            samlRequest.setMinorVersion(1);
            SAMLResponse response = null;
            LOG.debug("idp: " + LionShareApplicationSettings.IDP_URN.getValue());
            for (Endpoint endpoint : this.endPoints) {
                SAMLSOAPHTTPBinding binding = (SAMLSOAPHTTPBinding) SAMLBindingFactory.getInstance(endpoint.getBinding());
                binding.addHook(new LSHttpHook(this.aaDescriptor, this.trustProvider));
                String location = endpoint.getLocation();
                location = location.substring(0, location.lastIndexOf("/")) + lsEndPointName;
                LOG.debug("Attempting to contact endpoint: " + location);
                KeyManager keyManager = BasicSecureSocketFactory.getInstance().getKeyManager(handle);
                response = binding.send(location, samlRequest, keyManager);
                if (response != null) {
                    break;
                }
            }
            if (response == null) {
                return null;
            }
            Iterator<SAMLAssertion> assertionIterator = response.getAssertions();
            SAMLAssertion assertion = assertionIterator.next();
            LOG.debug(assertion.toString());
            return assertion;
        } catch (Exception ex) {
            LOG.error("Error obtaining SAML credentials", ex);
            return null;
        }
    }

    public String getEmail() {
        return email;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public void setTrustServerCallback(TrustServerCallback call) {
        this.callback = call;
    }

    public void setAuthenticateUserCallback(AuthenticateUserCallback callback) {
        this.aucallback = callback;
    }

    public synchronized void addListener(SecurityManagerListener listr) {
        listeners.add(listr);
    }

    public synchronized void removeListener(SecurityManagerListener listr) {
        listeners.remove(listr);
    }

    public synchronized void fireUpdate() {
        ListIterator iter = listeners.listIterator();
        while (iter.hasNext()) {
            SecurityManagerListener listr = (SecurityManagerListener) iter.next();
            listr.fireSecurityStateChanged();
        }
    }

    public Object getPersonalAttributes(Object parameter) {
        return null;
    }

    public String getCredentialIdentifier(Object parameter) {
        try {
            return SubjectDNParser.getCryptoShibHandle(((X509Certificate[]) parameter)[0].getSubjectX500Principal().getName());
        } catch (Exception e) {
            LOG.trace("Could not parse subject dn", e);
        }
        return "";
    }

    public boolean doesUserTrustServer(Object server_credentials) {
        LOG.debug("DOES USER TRUST PEERSERVER IS BEING CALLED");
        boolean result = true;
        if (server_credentials instanceof java.security.cert.X509Certificate) {
            java.security.cert.X509Certificate crt = (java.security.cert.X509Certificate) server_credentials;
            if (callback != null) {
                int returned = callback.promptUser(crt);
                if (returned == TrustServerCallback.DO_NOT_ACCEPT) {
                    result = false;
                } else if (returned == TrustServerCallback.ACCEPT_PERMANENT) {
                    TruststoreManager.getInstance().addTrustedCert(crt);
                    result = true;
                } else if (returned == TrustServerCallback.ACCEPT_TEMPORARY) {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Parse the SASL-CA seclayer string and setup the SASL security layer properties.
     *
     * A SASL security layer can be null (SASL only authenticates the user), or it can optionally
     * provide integrity protection or confidentiality. Futher, it can require mutual authentication
     * of the client and server.
     *
     * @param securityString A {@link String} representation of the security layer properties.
     *
     * @return A {@link Map} of SASL security layer properties.
     *
     * @throws IllegalArgumentException If any argument is <code>null</code> or if <code>properties</code>
     *                                  does not contain the necessary entries.
     *
     * @see Sasl#createSaslClient
     */
    private static Map<String, String> setupSaslSecurityLayer(final String securityString) throws IllegalArgumentException {
        if (securityString == null) {
            throw new IllegalArgumentException("securityString is null");
        }
        Map<String, String> securityLayerProps = new HashMap<String, String>();
        StringBuilder qopString = new StringBuilder();
        for (String component : securityString.split(" ")) {
            if (component.equals(StringConstants.secLayerMutualAuth)) {
                LOG.debug("enabling mutual auth");
                securityLayerProps.put(javax.security.sasl.Sasl.SERVER_AUTH, "true");
            } else if (component.equals(StringConstants.authOnly)) {
                if (qopString.length() > 0) {
                    qopString.append(",");
                }
                qopString.append("auth");
            } else if (component.equals(StringConstants.authInt)) {
                if (qopString.length() > 0) {
                    qopString.append(",");
                }
                qopString.append("auth-int");
            } else if (component.equals(StringConstants.authConf)) {
                if (qopString.length() > 0) {
                    qopString.append(",");
                }
                qopString.append("auth-conf");
            }
        }
        String s = qopString.toString();
        if (!s.equals("")) {
            securityLayerProps.put(javax.security.sasl.Sasl.QOP, s);
        }
        LOG.debug("QOP: " + s);
        return securityLayerProps;
    }

    /**
     * Parse the federation metadata.
     *
     * If there is an error parsing the file, this.federationMetadata is set to <code>null</code>
     */
    public synchronized void parseFederationMetadata() throws NullPointerException {
        File federationMetadataFile = this.getMetadataFilename();
        try {
            if (federationMetadataFile == null) {
                LOG.debug("Federation metadata file is null");
                throw new NullPointerException("Federation metadata file is null; please set a value");
            }
            URL metadataURL = federationMetadataFile.toURL();
            LOG.debug("parsing federation metadata");
            Document metadataDocument = Parser.loadDom(metadataURL, false);
            if (metadataDocument == null) {
                LOG.error("Parse error while parsing metadata");
                throw new NullPointerException("Parse error while parsing metadata file: " + federationMetadataFile);
            }
            Element metadata = metadataDocument.getDocumentElement();
            XMLMetadataProvider mdp = new XMLMetadataProvider(metadata);
            if (mdp == null) {
                throw new NullPointerException("error creating XMLMetadataProvider from parsed XML metadata");
            }
            this.federationMetadata = mdp;
        } catch (Exception ex) {
            LOG.error("Error loading federation metadata", ex);
        }
    }

    /**
     * Load the {@link TrustAnchor}s for signing file metadata.
     *
     * Normally there will only be one of these, and it will be for USHER.
     *
     * The {@link TrustAnchor}s are read in from a JCEKS {@link KeyStore}.
     * All of the TrustedCertificate aliases in the KeyStore are converted into
     * {@link TrustAnchor}s. Becuase of this, you should use a seperate
     * {@link KeyStore} for the {@link TrustAnchor}. 
     *
     * On error, <code>this.trustAnchors</code> is set to <code>null</code>.
     *
     * @see java.security.cert.KeyStore.TrustedCertificateEntry;
     */
    private void loadTrustAnchors(final String keystoreLocation) {
        LOG.debug("keystore location: " + keystoreLocation);
        try {
            if (keystoreLocation == null) {
                throw new NullPointerException("No TrustAnchor KeyStore name is set");
            }
            InputStream keyStoreStream = null;
            if (new File(keystoreLocation).exists()) {
                keyStoreStream = new FileInputStream(keystoreLocation);
            } else if (new File("../trust1.keystore").exists()) {
                keyStoreStream = new FileInputStream(new File("../trust1.keystore"));
            } else if (new File("trust1.keystore").exists()) {
                keyStoreStream = new FileInputStream(new File("../trust1.keystore"));
            } else {
                URL url = Thread.currentThread().getContextClassLoader().getResource("trust1.keystore");
                if (url != null) keyStoreStream = new BufferedInputStream(url.openStream());
            }
            KeyStore ks = KeyStore.getInstance(trustStoreType);
            ks.load(keyStoreStream, trustStorePassword.toCharArray());
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                LOG.debug("inspecting alias " + alias);
                if (ks.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry.class)) {
                    LOG.debug("Adding TrustAnchor: " + ((X509Certificate) ks.getCertificate(alias)).getSubjectX500Principal().getName());
                    TrustAnchor ta = new TrustAnchor((X509Certificate) (ks.getCertificate(alias)), null);
                    this.trustAnchors.add(ta);
                }
            }
        } catch (Exception ex) {
            LOG.error("Error loading TrustAnchors", ex);
            this.trustAnchors = null;
        }
    }

    /**
     * Get an instance of {@link MetadataSignatureManager}.
     *
     * note: this is currently a hack. it returns a new MSM for each call.
     * I'd like it to reuse MSM, but I need to go thru the code that calls it
     * and rework it.
     *
     * @return An instance of {@link MetadataSignatureManager} with <code>this.trustAnchors</code>.
     */
    public MetadataSignatureManager getMetadataSignatureManager() {
        return new MetadataSignatureManager(this.trustAnchors);
    }

    /**
     * Get a copy of the AssertionValidator.
     *
     * @return The {@link AssertionValidator}.
     */
    public AssertionValidator getAssertionValidator() {
        return this.assertionValidator;
    }

    /**
     * Get a new opaque cert on the basis of an existing one.
     */
    public LionShareCredential getNewOpaqueCert() {
        if (this.usingSelfSignedCert) {
            return new LionShareCredential(this.selfSignedPrivateKey, new X509Certificate[] { this.selfSignedCert });
        }
        try {
            LionShareCredential oldOpaqueCreds = KeystoreManager.getInstance().getOpaqueCredentials();
            X509Certificate oldOpaqueCert = oldOpaqueCreds.getCertChain()[0];
            PrivateKey oldOpaqueKey = oldOpaqueCreds.getPrivateKey();
            KeyPair newOpaqueKeyPair = GenKeys.generateKeyPair(keyAlgorithm, keySize);
            X509Certificate[] newCertChain = this.cph.refreshCertificate(oldOpaqueCert, StringConstants.opaqueCertIdentifier, oldOpaqueKey, newOpaqueKeyPair.getPublic(), signatureAlgorithm);
            boolean result = KeystoreManager.getInstance().addKey(newOpaqueKeyPair.getPrivate(), newCertChain, true);
            if (!result) {
                LOG.error("unable to register new opaque cert with KeystoreManager");
            }
            LionShareCredential newCreds = new LionShareCredential(newOpaqueKeyPair.getPrivate(), newCertChain);
            return newCreds;
        } catch (Exception ex) {
            LOG.debug("error refresing cert", ex);
            return null;
        }
    }

    /**
     * Extracts a {@link Map} of all AttributeAuthority endpoints from SAML metadata. 
     * This endpoints are keyed by their Organization's DisplayName.
     *
     * This method only supports SAML 1.1 endpoints at the moment.
     * When OpenSAML and Shibboleth support SAML 2.0 endpoint, it will be upgraded.
     */
    public Map<String, String> getSAMLIdPs() {
        TreeMap<String, String> wayfData = new TreeMap<String, String>();
        EntitiesDescriptor entities = this.federationMetadata.getRootEntities();
        Iterator<EntityDescriptor> entitiesIterator = entities.getEntityDescriptors();
        while (entitiesIterator.hasNext()) {
            EntityDescriptor descriptor = entitiesIterator.next();
            if (descriptor.isValid() && descriptor.getAttributeAuthorityDescriptor(XML.SAML11_PROTOCOL_ENUM) != null) {
                String entityID = descriptor.getId();
                String displayName = descriptor.getOrganization().getDisplayName();
                wayfData.put(displayName, entityID);
            }
        }
        return wayfData;
    }

    /** 
     * Set the users's home entity
     *
     * @param entity The user's home entity
     * 
     * @return <code>true</code> if entity is valid, otherwise <code>false</code>
     */
    public boolean setSAMLIdP(final String entity) {
        EntityDescriptor desc = this.federationMetadata.lookup(entity);
        if (desc == null) {
            return false;
        }
        LOG.debug("setting new IdP entityId to: " + entity);
        LionShareApplicationSettings.IDP_URN.setValue(entity);
        LionShareApplicationSettings.instance().getFactory().save();
        this.aaDescriptor = desc.getAttributeAuthorityDescriptor(XML.SAML11_PROTOCOL_ENUM);
        this.extractAAEndpoints(entity);
        return true;
    }

    /**
     * Extract all of the AA endpoints for a user's IdP
     * and filter those that don't support SAMLSOAP.
     */
    private void extractAAEndpoints(final String entityId) {
        EndpointManager manager = this.aaDescriptor.getAttributeServiceManager();
        Iterator<Endpoint> endpointIterator = manager.getEndpoints();
        while (endpointIterator.hasNext()) {
            Endpoint endpoint = endpointIterator.next();
            if (endpoint.getBinding().equals(SAMLBinding.SOAP)) {
                this.endPoints.add(endpoint);
            }
        }
    }

    /**
     * Check if the peer is using a self-signed cert.
     * If so, extract it and its private key from the appropriate keystore.
     */
    private void checkForSelfSignedCert() {
        this.usingSelfSignedCert = LionShareApplicationSettings.SELF_SIGNED_USE.getValue();
        if (this.usingSelfSignedCert) {
            FileInputStream fis = null;
            try {
                String ksType = LionShareApplicationSettings.SELF_SIGNED_KEYSTORE_TYPE.getValue();
                String ksFile = LionShareApplicationSettings.SELF_SIGNED_CERT_KEYSTORE_LOCATION.getValue();
                char[] ksPass = LionShareApplicationSettings.SELF_SIGNED_KEYSTORE_PASSWORD.getValue().toCharArray();
                String ksAlias = LionShareApplicationSettings.SELF_SIGNED_KEYSTORE_ALIAS.getValue();
                char[] ksKeyPass = LionShareApplicationSettings.SELF_SIGNED_PRIVKEY_PASSWORD.getValue().toCharArray();
                KeyStore ks = KeyStore.getInstance(ksType);
                fis = new FileInputStream(ksFile);
                ks.load(fis, ksPass);
                fis.close();
                this.selfSignedCert = (X509Certificate) ks.getCertificate(ksAlias);
                this.selfSignedPrivateKey = (PrivateKey) ks.getKey(ksAlias, ksKeyPass);
                LOG.debug("using self-signed cert with DN: " + this.selfSignedCert.getSubjectX500Principal().getName());
            } catch (Exception ex) {
                LOG.error("Error loading self-signed certificate", ex);
                this.selfSignedCert = null;
                this.selfSignedPrivateKey = null;
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ioex) {
                        LOG.error("Error closing self-signed keystore file");
                    }
                }
            }
        }
    }

    /**
     * Renew an identity cert before it expires.
     */
    public void renewIdentityCertificate() throws NoSuchAlgorithmException, KeyStoreException, ClientProtocolHandlerException {
        KeyPair keyPair = GenKeys.generateKeyPair(this.keyAlgorithm, this.keySize);
        LionShareCredential opaqueCreds = this.getNewOpaqueCert();
        X509Certificate existingCert = opaqueCreds.getCertChain()[0];
        PrivateKey existingKey = opaqueCreds.getPrivateKey();
        X509Certificate[] identityChain = this.cph.refreshCertificate(existingCert, StringConstants.identityCertIdentifier, existingKey, keyPair.getPublic(), signatureAlgorithm);
        KeystoreManager.getInstance().addKey(keyPair.getPrivate(), identityChain, false);
        this.fireUpdate();
    }

    public static class DefaultCredentialRequestParameters {

        private List attrs;

        private String guid;

        public DefaultCredentialRequestParameters(String guid, List attr) {
            this.attrs = attr;
            this.guid = guid;
        }

        public String getGUID() {
            return guid;
        }

        public List getAttributes() {
            return attrs;
        }
    }

    /**
     * {@link HTTPHook} implementation used to set SSL trust-
     * and keymanagers when contacting the AA. Based on the
     * ShibHttpHook code in Shibboleth 1.3.
     */
    private class LSHttpHook implements HTTPHook {

        /** The AA's {@link RoleDescriptor} from the federation metadata */
        RoleDescriptor role;

        /** A {@link ShibbolethTrust} object */
        Trust trust;

        /**
         * @param role
         */
        public LSHttpHook(final RoleDescriptor role, final Trust trust) {
            super();
            this.role = role;
            this.trust = trust;
        }

        public boolean incoming(HttpServletRequest r, Object globalCtx, Object callCtx) throws SAMLException {
            LOG.error("LSHttpHook method incoming-1 should not have been called.");
            return true;
        }

        public boolean outgoing(HttpServletResponse r, Object globalCtx, Object callCtx) throws SAMLException {
            LOG.error("LSHttpHook method outgoing-1 should not have been called.");
            return true;
        }

        public boolean incoming(HttpURLConnection conn, Object globalCtx, Object callCtx) throws SAMLException {
            return true;
        }

        /**
         * After the URLConnection has been initialized and before 
         * the connect() method is called, this exit has a chance to
         * do additional processing.
         * 
         * <p>If this is an HTTPS session, configure the SocketFactory
         * to use a custom TrustManager for Certificate processing.</p>
         *
         * @param callCtx should be a {@link KeyManager} with the opaque credentials.
         */
        public boolean outgoing(HttpURLConnection conn, Object globalCtx, Object callCtx) throws SAMLException {
            conn.setRequestProperty("Shibboleth", Constants.SHIB_VERSION);
            if (!(conn instanceof HttpsURLConnection)) {
                return true;
            }
            HttpsURLConnection sslconn = (HttpsURLConnection) conn;
            KeyManager keymgr;
            if (!(callCtx instanceof KeyManager)) {
                LOG.error("Must pass a KeyManager to outgoing()");
            }
            keymgr = (KeyManager) callCtx;
            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("TLS");
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Cannot find required SSL support");
                return true;
            }
            TrustManager[] tms = new TrustManager[] { new ShibTrustManager(role, trust) };
            KeyManager[] kms = new KeyManager[] { keymgr };
            try {
                sslContext.init(kms, tms, new java.security.SecureRandom());
            } catch (KeyManagementException e) {
                return false;
            }
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            sslconn.setSSLSocketFactory(socketFactory);
            return true;
        }
    }

    /**
     * Called to approve or reject an SSL Server Certificate.
     * In practice this is the Certificate of the AA.
     * 
     * <p>A TrustManager handles Certificate approval at either end
     * of an SSL connection, but this code is in the SP and is only 
     * inserted into the Attribute Query to the AA. When the AA is
     * configured to use HTTPS and presents an SSL Server Certficate,
     * call the commmon code to validate that this Certificate is in
     * the Metadata.</p>
     */
    class ShibTrustManager implements X509TrustManager {

        /** The AA's {@link RoleDescriptor} from the federation metadata */
        RoleDescriptor role;

        /** A {@link ShibbolethTrust} object */
        Trust trust;

        public ShibTrustManager(final RoleDescriptor role, final Trust trust) {
            this.role = role;
            this.trust = trust;
        }

        public X509Certificate[] getAcceptedIssuers() {
            LOG.error("LSHttpHook method getAcceptedIssuers should not have been called.");
            return new X509Certificate[0];
        }

        public void checkClientTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException {
            LOG.error("LSHttpHook method checkClientTrusted should not have been called.");
        }

        public void checkServerTrusted(final X509Certificate[] certs, final String arg1) throws CertificateException {
            LOG.debug("certs[0]: " + certs[0].getSubjectX500Principal().getName());
            for (X509Certificate cert : certs) {
                LOG.debug("cert: " + cert.getSubjectX500Principal().getName());
            }
            LOG.debug("this.trust: " + this.trust);
            LOG.debug("this.role: " + this.role);
            if (trust.validate(certs[0], certs, role)) {
                LOG.debug("LSHttpHook accepted AA Server Certificate.");
                return;
            }
            LOG.info("LSHttpHook rejected AA Server Certificate.");
            throw new CertificateException("Cannot validate AA Server Certificate in Metadata");
        }
    }

    class IdentityCertRefresher extends TimerTask {

        public IdentityCertRefresher() {
        }

        public void run() {
            try {
                LOG.info("attempting to renew identity certificate");
                renewIdentityCertificate();
                LOG.info("Successfully renewed identity certificate");
                fireUpdate();
                X509Certificate idCert = KeystoreManager.getInstance().getCertificate(false);
                Date renewalTime = new Date(idCert.getNotAfter().getTime() - refreshInterval);
                LOG.debug("scheduling identity credential renewal for " + renewalTime);
                identityRefreshTimer.schedule(new IdentityCertRefresher(), renewalTime);
            } catch (Exception ex) {
                LOG.error("Unable to renew identity credentials", ex);
            }
        }
    }
}
