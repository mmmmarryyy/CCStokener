package org.openremote.controller.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.security.AccessController;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.jboss.kernel.Kernel;
import org.jboss.logging.Logger;
import org.jboss.util.naming.NonSerializableFactory;

/**
 * Bootstraps controller and exports kernel related properties and references to JNDI for
 * consumption to other Java services deployed on the controller.
 *
 * This bootstrap bean should be deployed before any other controller Java services to ensure
 * exported variables are available.  <p>
 *
 * After successful deployment of this bean the JNDI will be initialized as follows:   <p>
 *
 * <pre>
 * /kernel                -- non-serializable reference to the microkernel itself
 * /serialnumber          -- String containing the serial number of this box    TODO: this is going away
 * /filesystem/root       -- java.io.File representing root configuration directory of the server
 * /filesystem/downloads  -- java.io.File representing the download directory of the server
 * </pre>
 *
 * If the controller is currently not initialized, a private key will be stored in the filesystem
 * with a keystore named ".keystore" under the JNDI bound "/filesystem/root" location.  As part
 * of the controller registration process, a corresponding public key certificate will be
 * registered with the OpenRemote Online Manager.
 *
 * @see #JNDI_FILESYSTEM_CONTEXT
 * @see #JNDI_FILESYSTEM_DOWNLOADS
 * @see #JNDI_KERNEL
 * @see #JNDI_SERIALNUMBER    TODO: this is going away
 *
 * @author <a href="mailto:juha@juhalindfors.com">Juha Lindfors</a>
 * @version $Id: $
 */
public class Bootstrap {

    /**
   * The name of the filesystem subcontext in JNDI.
   */
    public static final String JNDI_FILESYSTEM_CONTEXT = "/filesystem";

    /**
   * The name of java.io.File bound to JNDI representing the root directory of the filesystem.  <p>
   *
   * Notice that this is not the physical root of the OS level filesystem but only an abstraction
   * to the Java services.
   */
    public static final String JNDI_FILESYSTEM_ROOT = JNDI_FILESYSTEM_CONTEXT + "/root";

    /**
   * The name of java.io.File bound to JNDI representing the download directory on the filesystem.
   */
    public static final String JNDI_FILESYSTEM_DOWNLOADS = JNDI_FILESYSTEM_CONTEXT + "/downloads";

    /**
   * JNDI lookup name for org.jboss.kernel.Kernel reference bound to JNDI.
   */
    public static final String JNDI_KERNEL = "/kernel";

    /**
   * JNDI lookup name for the controller serial number string.    TODO: this is going away
   */
    public static final String JNDI_SERIALNUMBER = "/serialnumber";

    /**
   * Root logging category for controller Java services. It's recommended that loggers use this
   * string as a prefix when creating logging categories.
   */
    public static final String ROOT_LOG_CATEGORY = "OpenRemote.Controller";

    /**
   * Directory name where downloaded device bundles will be saved.
   */
    private static final String DOWNLOAD_DIRECTORY = "downloads";

    /**
   * Name of the default package downloaded from the Online Manager on registration. This
   * contains some core services that are assumed to be almost always used regardless of
   * how the controller is used.  
   */
    private static final String DEFAULT_PROFILE_PACKAGE = "default-profile.jar";

    /**
   * Bootstrap logger
   */
    private static final Logger log = Logger.getLogger(ROOT_LOG_CATEGORY + ".BOOTSTRAP");

    /**
   * Returns controller server root directory. The environment variable is setup by the
   * microkernel and should point to [ROOT]/server/controller directory. <p>
   *
   * @throws Error  if cannot access the system property.
   *
   * @return  java.io.File instance representing server root directory path
   */
    private static File getServerRoot() {
        return AccessController.doPrivileged(new PrivilegedAction<File>() {

            public File run() {
                try {
                    return new File(System.getProperty("jboss.server.home.dir"));
                } catch (SecurityException e) {
                    throw new Error("Cannot access property 'jboss.server.home.dir': " + e, e);
                }
            }
        });
    }

    /**
   * Returns the location of download directory. The path will be relative to
   * {@link #getServerRoot()}. The name of the download directory is specified in
   * {@link #DOWNLOAD_DIRECTORY}.  <p>
   *
   * If 'ServerRoot/DOWNLOAD_DIRECTORY' does not exist, an attempt is made to
   * create it. <p>
   *
   * @return file reference to 'ServerRoot/DOWNLOAD_DIRECTORY'.
   *
   * @throws Error              if 'ServerRoot/DOWNLOAD_DIRECTORY' exists but is not a directory,
   *                            or the directory could not be created, or the security manager
   *                            has denied access to downloads directory
   */
    private static File getDownloadDirectory() {
        final File downloads = new File(getServerRoot(), DOWNLOAD_DIRECTORY);
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            public Void run() {
                try {
                    if (!downloads.exists()) {
                        boolean created = downloads.mkdir();
                        if (!created) {
                            throw new Error("Cannot create '" + getServerRoot() + File.separator + DOWNLOAD_DIRECTORY + "'. ");
                        }
                    } else if (!downloads.isDirectory()) {
                        throw new Error(getServerRoot() + File.separator + DOWNLOAD_DIRECTORY + " is not " + "a directory. Rename or move file '" + DOWNLOAD_DIRECTORY + "' and restart " + "the server.");
                    }
                    return null;
                } catch (SecurityException e) {
                    throw new Error("Security manager has denied access to download directory (" + DOWNLOAD_DIRECTORY + "): " + e, e);
                }
            }
        });
        return downloads;
    }

    /**
   * Reference to JNDI established in the constructor.
   */
    private Context naming = null;

    /**
   * Will initialize the controller environment and export properties to JNDI
   * for consumption to Java services deployed on the controller.  <p>
   *
   * Successful instantiation will initialize JNDI server with a root level subcontext
   * {@link #JNDI_FILESYSTEM_CONTEXT} under which {@link #JNDI_FILESYSTEM_ROOT} and
   * {@link #JNDI_FILESYSTEM_DOWNLOADS} names will be bound. Note that this constructor
   * will also create a downloads directory on the controller filesystem if it is missing. <p>
   *
   * All objects bound under FILESYSTEM_CONTEXT should be of java.io.File type.   <p>
   *
   * Successful lookup from the JNDI name FILESYSTEM_ROOT will yield a reference
   * to java.io.File representing the root directory of the controller server configuration.   <p>
   *
   * Succcessful lookup from the JNDI name FILESYSTEM_DOWNLOADS will yield a
   * reference to java.io.File representing the controller download directory.  <p>
   *
   * Note that additionally the {@link #setKernel(org.jboss.kernel.Kernel)} method will export
   * the kernel reference to JNDI.   <p>
   *
   * @see #setKernel(org.jboss.kernel.Kernel)
   * @see #start()
   *
   * @see #JNDI_FILESYSTEM_CONTEXT
   * @see #JNDI_FILESYSTEM_ROOT
   * @see #JNDI_FILESYSTEM_DOWNLOADS
   *
   * @throws Error  if security manager denies access to the system properties set by the
   *                microkernel, or to the download directory of the home box; if there are
   *                any problems accessing the download directory; or if binding variables
   *                to JNDI fails
   */
    public Bootstrap() {
        try {
            setSecurityProvider();
            naming = new InitialContext();
            naming.bind(JNDI_SERIALNUMBER, "123456789");
            Context filesystem = naming.createSubcontext(JNDI_FILESYSTEM_CONTEXT);
            filesystem.bind(JNDI_FILESYSTEM_ROOT, getServerRoot());
            filesystem.bind(JNDI_FILESYSTEM_DOWNLOADS, getDownloadDirectory());
            log.info("Reference to controller root directory bound to '" + JNDI_FILESYSTEM_ROOT + "'.");
            log.info("Reference to controller downloads directory bound to '" + JNDI_FILESYSTEM_DOWNLOADS + "'.");
        } catch (NamingException e) {
            throw new Error("Cannot initialize naming context: " + e, e);
        }
    }

    /**
   * This property is injected by the kernel when this bean is deployed. The injected kernel
   * reference will be bound to JNDI naming context under name {@link #JNDI_KERNEL} as
   * a non-serializable reference.  <p>
   *
   * This is a mandatory property for the bootstrap bean configuration. This method should
   * not be invoked directly.
   *
   * @param kernel    reference to the kernel injected by the kernel itself on deploying this bean
   */
    public void setKernel(Kernel kernel) {
        try {
            NonSerializableFactory.rebind(naming, JNDI_KERNEL, kernel);
            log.info("Kernel bound to JNDI name '" + JNDI_KERNEL + "'.");
        } catch (NamingException e) {
            log.fatal("Unable to bind kernel reference to JNDI: " + e.toString(), e);
        }
    }

    /**
   * Configures the Online Manager URL(s) this connector can use to register itself. The values
   * will be injected by the kernel configuration service.
   *
   * This is a mandatory property of the bootstrap bean configuration. This method should not
   * be invoked directly.
   *
   * @param urls  list of URLs for locating OpenRemote Online Manager(s) for controller
   *              registration
   */
    public void setOnlineManagerURLs(List<URL> urls) {
        ControllerRegistrationConnection.setServiceURLs(urls);
    }

    /**
   * Optionally configures the registration connection reattempt delay for this controller.
   * If this property is configured, the value will be injected by the kernel configuration
   * service. <p>
   *
   * This method should not be invoked directly.
   *
   * @param seconds   connection reattempt delay in seconds
   */
    public void setRegistrationReattemptDelay(int seconds) {
        if (seconds < 0) {
            log.warn("Negative registration reattempt delay value ignored.");
            return;
        }
        ControllerRegistrationConnection.setReattemptDelay(seconds);
    }

    /**
   * Optionally configures the web application context path used for locating the Online
   * Manager's REST interface handling controller's registration.  <p>
   *
   * If this property is configured, the value will be injected by the kernel configuration
   * service. <p>
   *
   * This method should not be invoked directly.
   *
   * @param webContext  web application context path for the Online Manager's REST interface
   *                    managing controller registrations
   */
    public void setControllerRegistrationWebContext(String webContext) {
        ControllerRegistrationConnection.setControllerRegistrationWebContext(webContext);
    }

    /**
   * Service start method. This is invoked by the kernel configuration service once the properties
   * on this been have been set.  <p>
   *
   * This implementation will check the controller state, validate some configuration property
   * values and initiate the controller registration process if the controller is in an
   * unregistered state.
   */
    public void start() {
        if (ControllerRegistrationConnection.serviceURLs == null) {
            log.error("Configuration error: missing Online Manager URL(s).");
            setOnlineManagerURLs(new ArrayList<URL>());
        }
        try {
            if (!checkDefaultProfile()) {
                initializeDefaultProfile();
            }
        } catch (Error e) {
            log.error("Registration failed due to software implementation error. " + "Software update may be necessary (" + e + ").", e);
        }
    }

    /**
   * Checks for the existence of default profile package in the downloads directory.
   *
   * @return true if the default profile is already present; false otherwise
   *
   * @throws Error    if accessing the download directory is not possible for any reason
   */
    private boolean checkDefaultProfile() {
        File downloadDir = getDownloadDirectory();
        final File defaultProfile = new File(downloadDir, DEFAULT_PROFILE_PACKAGE);
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

            public Boolean run() {
                try {
                    return defaultProfile.exists();
                } catch (SecurityException e) {
                    throw new Error("Security manager has denied read access to " + defaultProfile + ": " + e, e);
                }
            }
        });
    }

    /**
   * Starts the controller registration process.    <p>
   *
   * This includes checking if a key pair has been generated and stored for the controller, and
   * if not creating and storing one in the controller's keystore and attempting to register
   * the corresponding public key certificate with the Online Manager.
   *
   * @throws Error   in case of an implementation error, or security manager access restrictions
   */
    private void initializeDefaultProfile() {
        log.info("#################################################################################");
        log.info("  Default profile not found. Initiating registration process...");
        log.info("#################################################################################");
        Certificate certificate;
        if (!ControllerKeyStore.keyExists()) {
            ControllerKeyStore keystore;
            if (!ControllerKeyStore.exists()) keystore = new ControllerKeyStore(true); else keystore = new ControllerKeyStore(false);
            certificate = keystore.createKey();
            log.info("Controller key generated and stored.");
            log.info("----- Public Certificate -----");
            log.info("\n" + certificate);
        } else {
            certificate = getPublicCertificate();
        }
        ControllerRegistrationConnection connection = new ControllerRegistrationConnection();
        connection.sendCertificate(certificate);
        log.info("#################################################################################");
        log.info("  Registration Complete.");
        log.info("#################################################################################");
    }

    /**
   * Returns the public certificate of this controller. To ensure the keystore has been created
   * prior calling this method, invoke ControllerKeyStore.keyExists().
   *
   * @return  the public certificate of this controller.
   *
   * @throws Error    in case of implementation errors
   */
    private Certificate getPublicCertificate() {
        ControllerKeyStore keystore = new ControllerKeyStore(false);
        return keystore.getPublicCertificate();
    }

    /**
   * Sets up bouncycastle as the most preferred security provider.
   */
    private void setSecurityProvider() {
        final Provider provider = new BouncyCastleProvider();
        int providerPosition = -1;
        try {
            providerPosition = AccessController.doPrivileged(new PrivilegedAction<Integer>() {

                public Integer run() {
                    return Security.insertProviderAt(provider, 1);
                }
            });
        } catch (SecurityException e) {
            log.warn("Cannot install security provider due to security manager restrictions.", e);
        }
        if (providerPosition == -1) {
            log.debug("Provider '" + provider.getName() + "' already installed.");
        }
    }

    /**
   * This nested class encapsulates the HTTP PUT connection from the controller to Online Manager
   * for registering the controller's public certificate.
   */
    private static class ControllerRegistrationConnection {

        /**
     * Unmodifiable list of URLs to Online Manager(s) that host controller registration service.
     */
        private static List<URL> serviceURLs = null;

        /**
     * Default web application context path for the REST implementation handling the controller
     * registrations. This context path is appended to the service URLs to create the final
     * URL attempted for connection.
     */
        private static String controllerRegistrationWebContext = "ControllerRegistration";

        /**
     * Default reattempt delay for registration connection in case none of the configured
     * Online Manager URLs answer or can complete controller registration.
     */
        private static int reattemptDelay = 60 * 1000;

        /**
     * Returns a user agent string for outgoing HTTP requests from this controller. This should
     * be included with all HTTP request headers.
     *
     * The user agent string contains the controller name followed by forward slash which separates
     * a version number string from the name. The version number is followed by a white space which
     * is followed by a three letter ISO language code in square brackets.
     *
     * The language code can be used to localize content between an online manager and
     * controller client applications.
     *
     * See the ISO 639-2 language codes: http://www.loc.gov/standards/iso639-2/englangn.html
     *
     * An example user agent string is:
     *   "OpenRemote Controller/1.0 [eng]"
     *
     * Note that the language bracket may have no content if the current Java VM locale doesn't
     * return a language code.
     *
     * @return user agent string for OpenRemote Controller
     */
        private static String getUserAgent() {
            String language = "";
            try {
                language = Locale.getDefault().getISO3Language();
            } catch (MissingResourceException e) {
                log.warn("Current locale " + Locale.getDefault() + " does not specify a language. " + "Localization features may be disabled. (" + e + ")");
            }
            return "OpenRemote Controller/0.1 [" + language + "]";
        }

        /**
     * Sets the URL(s) for Online Manager(s) capable of receiving controller registrations.
     * These URLs would be shared by all connections although the current implementation only
     * creates a single instance.
     *
     * Only URLs with HTTP scheme are valid.
     *
     * The invocation of this method is triggered by the kernel configuration service. Invoking
     * this methid directly is not recommended.
     *
     * @param urls    URLs to Online Manager(s) hosting controller registration service
     */
        private static void setServiceURLs(List<URL> urls) {
            serviceURLs = Collections.unmodifiableList(urls);
        }

        /**
     * Sets the web context path of the receiving REST implementation. This would affect
     * equally all connections although current implementation only creates a single instance.
     *
     * This path string is appended to the Online Manager URLs configured for this bootstrap
     * service. The combined URL should point to an Online Manager's URL which hosts the REST
     * interface for controller registration.
     *
     * Usually the default value in controllerRegistrationWebContext is sufficient but should
     * the URL change, it can be configured via this method.
     *
     * The invocation of this method is triggered by the kernel configuration service in the
     * current implementation. Invoking this method directly is not recommended.
     *
     * @param controllerRegistrationWebCtx  web application context path to Online Manager's
     *                                      REST API for controller registration
     */
        private static void setControllerRegistrationWebContext(String controllerRegistrationWebCtx) {
            controllerRegistrationWebContext = controllerRegistrationWebCtx;
        }

        /**
     * Sets the connection reattempt delay in case connecting to all service URLs has failed.
     * This would affect equally all connections although current implementation only creates
     * a single instance.
     *
     * Usually the default value in reattemptDelay is sufficient but it can be optionally
     * customized via this method.
     *
     * The invocation of this method is triggered by the kernel configuration service in the
     * current implementation. Invoking this method directly is not recommended.
     *
     * @param delay   reattempt delay in *seconds*
     */
        private static void setReattemptDelay(int delay) {
            reattemptDelay = delay * 1000;
        }

        /**
     * This iterator hangs on to current state of URLs we have attempted to connect to, basically
     * implementing a round-robin policy if multiple URLs are being used.
     */
        private Iterator<URL> urlListPosition = serviceURLs.iterator();

        /**
     * Controller serial number
     */
        private String serialNumber = null;

        /**
     * Constructs a helper class for handling the controller registration HTTP connection to
     * Online Manager.
     *
     * Notice that a lot of the configuration is currently done via static class members. If
     * more than one instance of ControllerRegistrationConnection is ever created by the
     * enclosing class (currently it is instantiated just once) then care should be taken
     * to treat the static members as immutable references -- mutating the values after the
     * service initialization is likely to land you in trouble!
     */
        private ControllerRegistrationConnection() {
            try {
                serialNumber = (String) new InitialContext().lookup(JNDI_SERIALNUMBER);
            } catch (NamingException e) {
                throw new Error(e);
            }
        }

        /**
     * Attempts to connect to and register controller's public key certificate with the
     * OpenRemote Online Manager.
     *
     * The connection is made via HTTP to online manager's REST interface. See the REST API
     * documentation for details. TODO : link to REST API spec
     *
     * Multiple target URLs are supported for redundancy and failover.
     *
     * @param certificate controller's X.509 certificate
     *
     * @throws Error  If there's an implementation error with regards to certificate encoding,
     *                or an implementation error in handling HTTP connection API. In all cases
     *                it is likely that the certification registration cannot be completed.
     *                However, catching and handling this error may still keep the controller
     *                in a responsive state (albeit unable to register).
     */
        private void sendCertificate(Certificate certificate) {
            while (true) {
                while (urlListPosition.hasNext()) {
                    URL homeURL = urlListPosition.next();
                    try {
                        URL url = new URL(homeURL, controllerRegistrationWebContext + "/" + serialNumber);
                        log.info("Attempting to connect to " + url);
                        byte[] encodedCertificate = certificate.getEncoded();
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setAllowUserInteraction(false);
                        connection.setUseCaches(false);
                        connection.setRequestMethod("PUT");
                        connection.setRequestProperty("Content-Type", "application/octet-stream");
                        connection.setRequestProperty("Content-Length", Integer.toString(encodedCertificate.length));
                        connection.setRequestProperty("User-Agent", getUserAgent());
                        BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream());
                        try {
                            out.write(encodedCertificate);
                            out.flush();
                        } finally {
                            out.close();
                        }
                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) return;
                        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                            log.info("You haven't created a user account yet!");
                        }
                    } catch (ClassCastException e) {
                        log.warn("Configuration error: " + homeURL + " is not a HTTP URL.");
                    } catch (MalformedURLException e) {
                        log.warn("Configuration error: " + homeURL + " is not valid URL.");
                    } catch (ConnectException e) {
                        log.debug("Cannot connect to " + homeURL + ": " + e + ". Moving on...");
                    } catch (CertificateEncodingException e) {
                        throw new Error("Can't register due to certificate implementation error: " + e, e);
                    } catch (UnknownServiceException e) {
                        throw new Error("Can't register due to HTTP connection implementation error: " + e, e);
                    } catch (ProtocolException e) {
                        throw new Error("Implementation Error: unknown HTTP method - " + e, e);
                    } catch (IOException e) {
                        log.debug("Failed to send certificate to " + homeURL + ": " + e.toString(), e);
                    }
                }
                try {
                    int seconds = reattemptDelay / 1000;
                    log.info("Failed to connect to controller registration service. " + "Will try again in " + seconds + " seconds...");
                    Thread.sleep(reattemptDelay);
                } catch (InterruptedException ignored) {
                }
                urlListPosition = serviceURLs.iterator();
            }
        }
    }

    /**
   * Encapsulates controller key store related methods and variables
   */
    private static class ControllerKeyStore {

        /**
     * Filename of the keystore used for storing controller's private key.
     */
        private static final String KEYSTORE_FILENAME = ".keystore";

        /**
     * The name used to locate the key entry from the keystore.
     */
        private static final String KEY_ALIAS = "ControllerPrivateKey";

        /**
     * Key algorithm for PPK pair.
     */
        private static final String KEY_ALGORITHM = "RSA";

        /**
     * Key size should be considered with the used algorithm. For RSA, a 2048 bit key size is
     * currently estimated to be good until 2030. Some of the default 1024 bit key sizes in
     * default security providers (SUN) should be considered vulnerable.
     */
        private static final int KEY_SIZE = 2048;

        /**
     * Checks that controller keystore exists and contains a controller private key alias
     * (as defined in KEY_ALIAS).
     *
     * @return  true if the keystore has a controller private key; false otherwise
     *
     * @throws Error    if access to environment properties in JNDI fails or security manager
     *                  denies access to keystore
     */
        private static boolean keyExists() {
            try {
                if (exists()) {
                    KeyStore keystore = getKeyStore(getKeyStoreInputStream());
                    return keystore.containsAlias(KEY_ALIAS);
                }
            } catch (KeyStoreException e) {
                throw new Error("Implementation error, keystore was not loaded before contains() call: " + e, e);
            }
            return false;
        }

        /**
     * Checks that controller keystore exists.
     *
     * @return  true if keystore for the controller exists; false otherwise
     *
     * @throws Error    if access to environment properties in JNDI fails or security manager
     *                  denies access to keystore
     */
        private static boolean exists() {
            final File keystoreFile = getKeyStoreFile();
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

                public Boolean run() {
                    try {
                        return keystoreFile.exists();
                    } catch (SecurityException e) {
                        throw new Error("Security manager has denied read access to " + keystoreFile + ": " + e, e);
                    }
                }
            });
        }

        /**
     * Returns a file representing the controller keystore.
     *
     * @return  controller keystore
     *
     * @throws Error    if can't access JNDI
     */
        private static File getKeyStoreFile() {
            try {
                Context naming = new InitialContext();
                File root = (File) naming.lookup(JNDI_FILESYSTEM_ROOT);
                return new File(root, KEYSTORE_FILENAME);
            } catch (NamingException e) {
                throw new Error("Can't access JNDI property '" + JNDI_FILESYSTEM_ROOT + "': " + e, e);
            }
        }

        /**
     * Loads a key store (or creates a new one)
     *
     * @param in  input stream to keystore file (or null to create a new one)
     *
     * @return  controller key store
     *
     * @throws Error  if none of the installed security providers contain implementation for the
     *                required keystore type, or loading the keystore fails for any other reason
     */
        private static KeyStore getKeyStore(InputStream in) {
            final String KEYSTORE_TYPE = KeyStore.getDefaultType();
            try {
                KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE);
                keystore.load(in, getKeyStorePassword());
                return keystore;
            } catch (KeyStoreException e) {
                throw new Error("No security provider found for " + KEYSTORE_TYPE + " keystore type (" + e.toString() + ").", e);
            } catch (NoSuchAlgorithmException e) {
                throw new Error("Required keystore algorithm '" + KEYSTORE_TYPE + "' not found: " + e.toString(), e);
            } catch (CertificateException e) {
                throw new Error("Can't load keystore: " + e.toString(), e);
            } catch (IOException e) {
                throw new Error("Can't load keystore: " + e.toString(), e);
            }
        }

        /**
     * Opens an output stream to keystore file.
     *
     * @return  buffered output stream to keystore
     *
     * @throws Error              if write access to keystore is denied by security manager;
     *                            the keystore file is not found, or access to JNDI to locate
     *                            keystore file fails
     */
        private static BufferedOutputStream getKeyStoreOutputStream() {
            final File keystoreFile = getKeyStoreFile();
            return AccessController.doPrivileged(new PrivilegedAction<BufferedOutputStream>() {

                public BufferedOutputStream run() {
                    try {
                        return new BufferedOutputStream(new FileOutputStream(keystoreFile));
                    } catch (FileNotFoundException e) {
                        throw new Error("Keystore file '" + keystoreFile + "' was not found: " + e, e);
                    } catch (SecurityException e) {
                        throw new Error("Security manager has denied write access to file '" + keystoreFile + "'.");
                    }
                }
            });
        }

        /**
     * Opens input stream to controller keystore.
     *
     * The keystore file is accessed within a privileged code block. Therefore this method
     * should not be exposed in a public API.
     *
     * @return buffered input stream to controller keystore
     *
     * @throws Error    if there's an error accessing JNDI properties, keystore cannot be
     *                  read due to security restrictions or the keystore file is not found
     */
        private static BufferedInputStream getKeyStoreInputStream() {
            final File keystoreFile = getKeyStoreFile();
            return AccessController.doPrivileged(new PrivilegedAction<BufferedInputStream>() {

                public BufferedInputStream run() {
                    try {
                        return new BufferedInputStream(new FileInputStream(keystoreFile));
                    } catch (FileNotFoundException e) {
                        throw new Error("Keystore file '" + keystoreFile + "' was not found: " + e, e);
                    } catch (SecurityException e) {
                        throw new Error("Security manager has denied read access to file '" + keystoreFile + "'.");
                    }
                }
            });
        }

        /**
     * TODO:
     *  Password for the keystore -- currently undecided where this is going to come from.
     *  Ideally we should have as little user overhead wrt security as possible, one secret
     *  at registration and rest should be possible to automate. So this might eventually be
     *  the registration code, or the WiFi key (the two might in fact be the same)
     *
     * @return
     */
        private static char[] getKeyStorePassword() {
            return "83409863093409123578495982346590873245".toCharArray();
        }

        /**
     * TODO:
     *  see getKeyStorePassword
     *
     *  This could be additional security if we end up storing more than one key.
     *  More than likely however it will be the same as keystore password (or null).
     *
     * @return
     */
        private static KeyStore.ProtectionParameter getControllerKeyPassword() {
            return new KeyStore.PasswordProtection(getKeyStorePassword());
        }

        /**
     * In-memory keystore instance.
     */
        private KeyStore keystore;

        /**
     * Instantiates a new controller key store.
     *
     * An existing keystore can be loaded or a new created depending on the constructor parameters.
     * When attempting to create a new keystore instance use the ControllerKeyStore.exists() first
     * to check for existence of a controller keystore.
     *
     * @param create  true to create a new keystore; false to load an existing one.
     */
        private ControllerKeyStore(boolean create) {
            if (create) keystore = getKeyStore(null); else keystore = getKeyStore(getKeyStoreInputStream());
        }

        /**
     * Creates and stores a public-private key pair for this controller. The keystore must be
     * either loaded or created by the constructor prior to calling this method.
     *
     * @return public key certificate for this controller
     *
     * @throws Error  if creating the certificate fails or writing it to the keystore fails
     *                for any reason
     */
        private Certificate createKey() {
            KeyPair keyPair = generateKeyPair();
            Certificate certificate = createCertificate(keyPair);
            storeKey(keyPair, certificate);
            return certificate;
        }

        /**
     * Returns the public certificate of this controller.
     *
     * @return  controller certificate
     *
     * @throws Error  if this method is called prior to keystore being loaded
     */
        private Certificate getPublicCertificate() {
            try {
                return keystore.getCertificate(KEY_ALIAS);
            } catch (KeyStoreException e) {
                throw new Error("Implementation Error: keystore has not been loaded.");
            }
        }

        /**
     * Stores a controller private key to keystore with a given certificate chain
     *
     * @param keyPair       controller PPK pair
     * @param certificate   controller key certificate
     *
     * @throws Error  if the key cannot be written for any reason
     */
        private void storeKey(KeyPair keyPair, Certificate certificate) {
            try {
                KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new java.security.cert.Certificate[] { certificate });
                keystore.setEntry(KEY_ALIAS, privateKeyEntry, getControllerKeyPassword());
                BufferedOutputStream out = getKeyStoreOutputStream();
                try {
                    keystore.store(out, getKeyStorePassword());
                    out.flush();
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        log.warn("Unable to close file '" + KEYSTORE_FILENAME + "'.");
                    }
                }
            } catch (KeyStoreException e) {
                throw new Error("Unable to store controller key: " + e, e);
            } catch (IOException e) {
                throw new Error("Unable to write controller key to keystore: " + e, e);
            } catch (NoSuchAlgorithmException e) {
                throw new Error("Security provider does not support required key store algorithm: " + e, e);
            } catch (CertificateException e) {
                throw new Error("Cannot store controller certificate: " + e, e);
            }
        }

        /**
     * Generates a key pair for this controller.
     *
     * @return generated key pair
     *
     * @throws Error  if the required algorithm is not supported by the security provider
     */
        private KeyPair generateKeyPair() {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
                try {
                    keyGen.initialize(KEY_SIZE);
                } catch (InvalidParameterException e) {
                    log.warn("Security provider '" + keyGen.getProvider().getName() + "' does not support " + KEY_SIZE + " bit keysize. Falling back to default keysize.", e);
                }
                log.info("Generating key...");
                return keyGen.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new Error("No security provider found for " + KEY_ALGORITHM + " algorithm.");
            }
        }

        /**
     * Creates a self-signed certificate for this controller.
     *
     * The certificate will be valid for approximately 10 years.
     *
     * @param keyPair   public-private key pair of this controller
     *
     * @return  a self-signed X.509 certificate
     *
     * @throws Error  if creating a certificate fails for any reason (implementation errors)
     */
        private Certificate createCertificate(KeyPair keyPair) {
            final String CERTIFICATE_ALGORITHM = "SHA1with" + KEY_ALGORITHM;
            final long SECOND = 1000;
            final long MINUTE = 60 * SECOND;
            final long HOUR = 60 * MINUTE;
            final long DAY = 24 * HOUR;
            final long YEAR = 365 * DAY;
            try {
                X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
                X509Name issuerName = new X509Name("O=www.openremote.org, CN=OpenRemote Online Manager");
                X509Name subjectName = new X509Name("CN=OpenRemote Controller");
                certGenerator.setPublicKey(keyPair.getPublic());
                certGenerator.setSubjectDN(subjectName);
                certGenerator.setIssuerDN(issuerName);
                long time = System.currentTimeMillis();
                certGenerator.setNotBefore(new Date(0));
                certGenerator.setNotAfter(new Date(time + 10 * YEAR));
                certGenerator.setSerialNumber(new BigInteger(Long.toString(time)));
                certGenerator.setSignatureAlgorithm(CERTIFICATE_ALGORITHM);
                return certGenerator.generate(keyPair.getPrivate());
            } catch (CertificateEncodingException e) {
                throw new Error("Implementation Error -- Cannot create certificate: " + e, e);
            } catch (NoSuchAlgorithmException e) {
                throw new Error("Implementation Error -- Certificate algorithm is not available: " + e, e);
            } catch (SignatureException e) {
                throw new Error("Implementation Error -- Cannot create certificate: " + e, e);
            } catch (InvalidKeyException e) {
                throw new Error("Implementation Error -- Controller key pair is not valid: " + e, e);
            } catch (IllegalStateException e) {
                throw new Error("Implementation Error -- Cannot create certificate: " + e, e);
            }
        }
    }
}
