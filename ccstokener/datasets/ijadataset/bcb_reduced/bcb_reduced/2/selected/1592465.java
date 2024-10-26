package gnu.classpath.tools.jarsigner;

import gnu.classpath.Configuration;
import gnu.classpath.SystemProperties;
import gnu.classpath.tools.common.CallbackUtil;
import gnu.classpath.tools.common.ProviderUtil;
import gnu.classpath.tools.getopt.ClasspathToolParser;
import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.OptionGroup;
import gnu.java.security.OID;
import gnu.java.security.Registry;
import gnu.javax.security.auth.callback.ConsoleCallbackHandler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.jar.Attributes.Name;
import java.util.logging.Logger;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * The GNU Classpath implementation of the <i>jarsigner</i> tool.
 * <p>
 * The <i>jarsigner</i> tool is used to sign and verify JAR (Java ARchive)
 * files.
 * <p>
 * This implementation is intended to be compatible with the behaviour
 * described in the public documentation of the same tool included in JDK 1.4.
 */
public class Main {

    protected static final Logger log = Logger.getLogger(Main.class.getName());

    static final String KEYTOOL_TOOL = "jarsigner";

    private static final Locale EN_US_LOCALE = new Locale("en", "US");

    static final String DIGEST = "SHA1-Digest";

    static final String DIGEST_MANIFEST = "SHA1-Digest-Manifest";

    static final Name DIGEST_ATTR = new Name(DIGEST);

    static final Name DIGEST_MANIFEST_ATTR = new Name(DIGEST_MANIFEST);

    static final OID DSA_SIGNATURE_OID = new OID(Registry.DSA_OID_STRING);

    static final OID RSA_SIGNATURE_OID = new OID(Registry.RSA_OID_STRING);

    protected boolean verify;

    protected String ksURL;

    protected String ksType;

    protected String password;

    protected String ksPassword;

    protected String sigFileName;

    protected String signedJarFileName;

    protected boolean verbose;

    protected boolean certs;

    protected boolean internalSF;

    protected boolean sectionsOnly;

    protected String providerClassName;

    protected String jarFileName;

    protected String alias;

    protected Provider provider;

    private boolean providerInstalled;

    private char[] ksPasswordChars;

    private KeyStore store;

    private char[] passwordChars;

    private PrivateKey signerPrivateKey;

    private Certificate[] signerCertificateChain;

    /** The callback handler to use when needing to interact with user. */
    private CallbackHandler handler;

    /** The command line parser. */
    private ToolParser cmdLineParser;

    protected ArrayList fileAndAlias = new ArrayList();

    ;

    private Main() {
        super();
    }

    public static final void main(String[] args) {
        if (Configuration.DEBUG) log.entering(Main.class.getName(), "main", args);
        Main tool = new Main();
        int result = 1;
        try {
            tool.processArgs(args);
            tool.start();
            result = 0;
        } catch (SecurityException x) {
            if (Configuration.DEBUG) log.throwing(Main.class.getName(), "main", x);
            System.err.println(Messages.getString("Main.7") + x.getMessage());
        } catch (Exception x) {
            if (Configuration.DEBUG) log.throwing(Main.class.getName(), "main", x);
            System.err.println(Messages.getString("Main.9") + x);
        } finally {
            tool.teardown();
        }
        if (Configuration.DEBUG) log.exiting(Main.class.getName(), "main", Integer.valueOf(result));
        System.exit(result);
    }

    /**
   * Read the command line arguments setting the tool's parameters in
   * preparation for the user desired action.
   * 
   * @param args an array of options (strings).
   * @throws Exception if an exception occurs during the process.
   */
    private void processArgs(String[] args) throws Exception {
        if (Configuration.DEBUG) log.entering(this.getClass().getName(), "processArgs", args);
        cmdLineParser = new ToolParser();
        cmdLineParser.initializeParser();
        cmdLineParser.parse(args, new ToolParserCallback());
        setupCommonParams();
        if (verify) {
            if (Configuration.DEBUG) {
                log.fine("Will verify with the following parameters:");
                log.fine("     jar-file = '" + jarFileName + "'");
                log.fine("Options:");
                log.fine("     provider = '" + providerClassName + "'");
                log.fine("      verbose ? " + verbose);
                log.fine("        certs ? " + certs);
                log.fine("   internalsf ? " + internalSF);
                log.fine(" sectionsonly ? " + sectionsOnly);
            }
        } else {
            setupSigningParams();
            if (Configuration.DEBUG) {
                log.fine("Will sign with the following parameters:");
                log.fine("     jar-file = '" + jarFileName + "'");
                log.fine("        alias = '" + alias + "'");
                log.fine("Options:");
                log.fine("     keystore = '" + ksURL + "'");
                log.fine("    storetype = '" + ksType + "'");
                log.fine("    storepass = '" + ksPassword + "'");
                log.fine("      keypass = '" + password + "'");
                log.fine("      sigfile = '" + sigFileName + "'");
                log.fine("    signedjar = '" + signedJarFileName + "'");
                log.fine("     provider = '" + providerClassName + "'");
                log.fine("      verbose ? " + verbose);
                log.fine("   internalsf ? " + internalSF);
                log.fine(" sectionsonly ? " + sectionsOnly);
            }
        }
        if (Configuration.DEBUG) log.exiting(this.getClass().getName(), "processArgs");
    }

    /**
   * Invokes the <code>start()</code> method of the concrete handler.
   * <p>
   * Depending on the result of processing the command line arguments, this
   * handler may be one for signing the jar, or verifying it.
   * 
   * @throws Exception if an exception occurs during the process.
   */
    private void start() throws Exception {
        if (Configuration.DEBUG) log.entering(this.getClass().getName(), "start");
        if (verify) {
            JarVerifier jv = new JarVerifier(this);
            jv.start();
        } else {
            JarSigner js = new JarSigner(this);
            js.start();
        }
        if (Configuration.DEBUG) log.exiting(this.getClass().getName(), "start");
    }

    /**
   * Ensures that the underlying JVM is left in the same state as we found it
   * when we first launched the tool. Specifically, if we have installed a new
   * security provider then now is the time to remove it.
   * <p>
   * Note (rsn): this may not be necessary if we terminate the JVM; i.e. call
   * {@link System#exit(int)} at the end of the tool's invocation. Nevertheless
   * it's good practive to return the JVM to its initial state.
   */
    private void teardown() {
        if (Configuration.DEBUG) log.entering(this.getClass().getName(), "teardown");
        if (providerInstalled) ProviderUtil.removeProvider(provider.getName());
        if (Configuration.DEBUG) log.exiting(this.getClass().getName(), "teardown");
    }

    /**
   * After processing the command line arguments, this method is invoked to
   * process the common parameters which may have been encountered among the
   * actual arguments.
   * <p>
   * Common parameters are those which are allowed in both signing and
   * verification modes.
   * 
   * @throws InstantiationException if a security provider class name is
   *           specified but that class name is that of either an interface or
   *           an abstract class.
   * @throws IllegalAccessException if a security provider class name is
   *           specified but no 0-arguments constructor is defined for that
   *           class.
   * @throws ClassNotFoundException if a security provider class name is
   *           specified but no such class was found in the classpath.
   * @throws IOException if the JAR file name for signing, or verifying, does
   *           not exist, exists but denotes a directory, or is not readable.
   */
    private void setupCommonParams() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
        if (Configuration.DEBUG) log.entering(this.getClass().getName(), "setupCommonParams");
        File jar = new File(jarFileName);
        if (!jar.exists()) throw new FileNotFoundException(jarFileName);
        if (jar.isDirectory()) throw new IOException(Messages.getFormattedString("Main.70", jarFileName));
        if (!jar.canRead()) throw new IOException(Messages.getFormattedString("Main.72", jarFileName));
        if (providerClassName != null && providerClassName.length() > 0) {
            provider = (Provider) Class.forName(providerClassName).newInstance();
            String providerName = provider.getName();
            Provider installedProvider = Security.getProvider(providerName);
            if (installedProvider != null) {
                if (Configuration.DEBUG) log.finer("Provider " + providerName + " is already installed");
            } else installNewProvider();
        }
        if (!verbose && certs) {
            if (Configuration.DEBUG) log.fine("Option <certs> is set but <verbose> is not. Ignored");
            certs = false;
        }
        if (Configuration.DEBUG) log.exiting(this.getClass().getName(), "setupCommonParams");
    }

    /**
   * Install the user defined security provider in the underlying JVM.
   * <p>
   * Also record this fact so we can remove it when we exit the tool.
   */
    private void installNewProvider() {
        if (Configuration.DEBUG) log.entering(this.getClass().getName(), "installNewProvider");
        providerInstalled = ProviderUtil.addProvider(provider) != -1;
        if (Configuration.DEBUG) log.exiting(this.getClass().getName(), "installNewProvider");
    }

    /**
   * After processing the command line arguments, this method is invoked to
   * process the parameters which may have been encountered among the actual
   * arguments, and which are specific to the signing action of the tool.
   * 
   * @throws KeyStoreException if no implementation of the designated (or
   *           default type) of a key store is availabe.
   * @throws IOException if an I/O related exception occurs during the process.
   * @throws NoSuchAlgorithmException if an implementation of an algorithm used
   *           by the key store is not available.
   * @throws CertificateException if an exception occurs while reading a
   *           certificate from the key store.
   * @throws UnsupportedCallbackException if no implementation of a password
   *           callback is available.
   * @throws UnrecoverableKeyException if the wrong password was used to unlock
   *           the key store.
   * @throws SecurityException if the designated alias is not known to the key
   *           store or is not an Alias of a Key Entry.
   */
    private void setupSigningParams() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnsupportedCallbackException, UnrecoverableKeyException {
        if (Configuration.DEBUG) log.entering(this.getClass().getName(), "setupSigningParams");
        if (ksURL == null || ksURL.trim().length() == 0) {
            String userHome = SystemProperties.getProperty("user.home");
            if (userHome == null || userHome.trim().length() == 0) throw new SecurityException(Messages.getString("Main.85"));
            ksURL = "file:" + userHome.trim() + "/.keystore";
        } else {
            ksURL = ksURL.trim();
            if (ksURL.indexOf(":") == -1) ksURL = "file:" + ksURL;
        }
        if (ksType == null || ksType.trim().length() == 0) ksType = KeyStore.getDefaultType(); else ksType = ksType.trim();
        store = KeyStore.getInstance(ksType);
        if (ksPassword == null) {
            PasswordCallback pcb = new PasswordCallback(Messages.getString("Main.92"), false);
            getCallbackHandler().handle(new Callback[] { pcb });
            ksPasswordChars = pcb.getPassword();
        } else ksPasswordChars = ksPassword.toCharArray();
        URL url = new URL(ksURL);
        InputStream stream = url.openStream();
        store.load(stream, ksPasswordChars);
        if (!store.containsAlias(alias)) throw new SecurityException(Messages.getFormattedString("Main.6", alias));
        if (!store.isKeyEntry(alias)) throw new SecurityException(Messages.getFormattedString("Main.95", alias));
        Key key;
        if (password == null) {
            passwordChars = ksPasswordChars;
            try {
                key = store.getKey(alias, passwordChars);
            } catch (UnrecoverableKeyException x) {
                String prompt = Messages.getFormattedString("Main.97", alias);
                PasswordCallback pcb = new PasswordCallback(prompt, false);
                getCallbackHandler().handle(new Callback[] { pcb });
                passwordChars = pcb.getPassword();
                key = store.getKey(alias, passwordChars);
            }
        } else {
            passwordChars = password.toCharArray();
            key = store.getKey(alias, passwordChars);
        }
        if (!(key instanceof PrivateKey)) throw new SecurityException(Messages.getFormattedString("Main.99", alias));
        signerPrivateKey = (PrivateKey) key;
        signerCertificateChain = store.getCertificateChain(alias);
        if (Configuration.DEBUG) log.fine(String.valueOf(signerCertificateChain));
        if (sigFileName == null) sigFileName = alias;
        sigFileName = sigFileName.toUpperCase(EN_US_LOCALE);
        if (sigFileName.length() > 8) sigFileName = sigFileName.substring(0, 8);
        char[] chars = sigFileName.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (!(Character.isLetter(c) || Character.isDigit(c) || c == '_' || c == '-')) chars[i] = '_';
        }
        sigFileName = new String(chars);
        if (signedJarFileName == null) signedJarFileName = jarFileName;
        if (Configuration.DEBUG) log.exiting(this.getClass().getName(), "setupSigningParams");
    }

    boolean isVerbose() {
        return verbose;
    }

    boolean isCerts() {
        return certs;
    }

    String getSigFileName() {
        return this.sigFileName;
    }

    String getJarFileName() {
        return this.jarFileName;
    }

    boolean isSectionsOnly() {
        return this.sectionsOnly;
    }

    boolean isInternalSF() {
        return this.internalSF;
    }

    PrivateKey getSignerPrivateKey() {
        return this.signerPrivateKey;
    }

    Certificate[] getSignerCertificateChain() {
        return signerCertificateChain;
    }

    String getSignedJarFileName() {
        return this.signedJarFileName;
    }

    /**
   * Return a CallbackHandler which uses the Console (System.in and System.out)
   * for interacting with the user.
   * <p>
   * This method first finds all currently installed security providers capable
   * of providing such service and then in turn attempts to instantiate the
   * handler from those providers. As soon as one provider returns a non-null
   * instance of the callback handler, the search stops and that instance is
   * set to be used from now on.
   * <p>
   * If no installed providers were found, this method falls back on the GNU
   * provider, by-passing the Security search mechanism. The default console
   * callback handler implementation is {@link ConsoleCallbackHandler}.
   * 
   * @return a console-based {@link CallbackHandler}.
   */
    protected CallbackHandler getCallbackHandler() {
        if (handler == null) handler = CallbackUtil.getConsoleHandler();
        return handler;
    }

    private class ToolParserCallback extends FileArgumentCallback {

        public void notifyFile(String fileArgument) {
            fileAndAlias.add(fileArgument);
        }
    }

    private class ToolParser extends ClasspathToolParser {

        public ToolParser() {
            super(KEYTOOL_TOOL, true);
        }

        protected void validate() throws OptionException {
            if (fileAndAlias.size() < 1) throw new OptionException(Messages.getString("Main.133"));
            jarFileName = (String) fileAndAlias.get(0);
            if (!verify) if (fileAndAlias.size() < 2) {
                if (Configuration.DEBUG) log.fine("Missing ALIAS argument. Will use [mykey] instead");
                alias = "mykey";
            } else alias = (String) fileAndAlias.get(1);
        }

        public void initializeParser() {
            setHeader(Messages.getString("Main.2"));
            setFooter(Messages.getString("Main.1"));
            OptionGroup signGroup = new OptionGroup(Messages.getString("Main.0"));
            signGroup.add(new Option("keystore", Messages.getString("Main.101"), Messages.getString("Main.102")) {

                public void parsed(String argument) throws OptionException {
                    ksURL = argument;
                }
            });
            signGroup.add(new Option("storetype", Messages.getString("Main.104"), Messages.getString("Main.105")) {

                public void parsed(String argument) throws OptionException {
                    ksType = argument;
                }
            });
            signGroup.add(new Option("storepass", Messages.getString("Main.107"), Messages.getString("Main.108")) {

                public void parsed(String argument) throws OptionException {
                    ksPassword = argument;
                }
            });
            signGroup.add(new Option("keypass", Messages.getString("Main.110"), Messages.getString("Main.111")) {

                public void parsed(String argument) throws OptionException {
                    password = argument;
                }
            });
            signGroup.add(new Option("sigfile", Messages.getString("Main.113"), Messages.getString("Main.114")) {

                public void parsed(String argument) throws OptionException {
                    sigFileName = argument;
                }
            });
            signGroup.add(new Option("signedjar", Messages.getString("Main.116"), Messages.getString("Main.117")) {

                public void parsed(String argument) throws OptionException {
                    signedJarFileName = argument;
                }
            });
            add(signGroup);
            OptionGroup verifyGroup = new OptionGroup(Messages.getString("Main.118"));
            verifyGroup.add(new Option("verify", Messages.getString("Main.120")) {

                public void parsed(String argument) throws OptionException {
                    verify = true;
                }
            });
            verifyGroup.add(new Option("certs", Messages.getString("Main.122")) {

                public void parsed(String argument) throws OptionException {
                    certs = true;
                }
            });
            add(verifyGroup);
            OptionGroup commonGroup = new OptionGroup(Messages.getString("Main.123"));
            commonGroup.add(new Option("verbose", Messages.getString("Main.125")) {

                public void parsed(String argument) throws OptionException {
                    verbose = true;
                }
            });
            commonGroup.add(new Option("internalsf", Messages.getString("Main.127")) {

                public void parsed(String argument) throws OptionException {
                    internalSF = true;
                }
            });
            commonGroup.add(new Option("sectionsonly", Messages.getString("Main.129")) {

                public void parsed(String argument) throws OptionException {
                    sectionsOnly = true;
                }
            });
            commonGroup.add(new Option("provider", Messages.getString("Main.131"), Messages.getString("Main.132")) {

                public void parsed(String argument) throws OptionException {
                    providerClassName = argument;
                }
            });
            add(commonGroup);
        }
    }
}
