package org.apache.harmony.jndi.internal;

import java.applet.Applet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * This is a utility class that reads environment properties.
 */
public final class EnvironmentReader {

    private static final String APPLICATION_RESOURCE_FILE = "jndi.properties";

    private static final String PROVIDER_RESOURCE_FILE = "jndiprovider.properties";

    private EnvironmentReader() {
        super();
    }

    public static void mergeEnvironment(final Hashtable<?, ?> src, final Hashtable<Object, Object> dst, final boolean valueAddToList) {
        Object key = null;
        String val = null;
        Enumeration<?> keys = src.keys();
        while (keys.hasMoreElements()) {
            key = keys.nextElement();
            if (!dst.containsKey(key)) {
                dst.put(key, src.get(key));
            } else if (valueAddToList && (LdapContext.CONTROL_FACTORIES.equals(key) || Context.OBJECT_FACTORIES.equals(key) || Context.STATE_FACTORIES.equals(key) || Context.URL_PKG_PREFIXES.equals(key))) {
                val = (String) dst.get(key);
                val = val + ":" + src.get(key);
                dst.put(key, val);
            } else {
            }
        }
    }

    static Hashtable<Object, Object> filterProperties(final JNDIPropertiesSource source) {
        final Hashtable<Object, Object> filteredProperties = new Hashtable<Object, Object>();
        String propValue = null;
        propValue = source.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        if (null != propValue) {
            filteredProperties.put(Context.INITIAL_CONTEXT_FACTORY, propValue);
        }
        propValue = source.getProperty(Context.DNS_URL);
        if (null != propValue) {
            filteredProperties.put(Context.DNS_URL, propValue);
        }
        propValue = source.getProperty(Context.PROVIDER_URL);
        if (null != propValue) {
            filteredProperties.put(Context.PROVIDER_URL, propValue);
        }
        propValue = source.getProperty(Context.OBJECT_FACTORIES);
        if (null != propValue) {
            filteredProperties.put(Context.OBJECT_FACTORIES, propValue);
        }
        propValue = source.getProperty(Context.STATE_FACTORIES);
        if (null != propValue) {
            filteredProperties.put(Context.STATE_FACTORIES, propValue);
        }
        propValue = source.getProperty(Context.URL_PKG_PREFIXES);
        if (null != propValue) {
            filteredProperties.put(Context.URL_PKG_PREFIXES, propValue);
        }
        propValue = source.getProperty(LdapContext.CONTROL_FACTORIES);
        if (null != propValue) {
            filteredProperties.put(LdapContext.CONTROL_FACTORIES, propValue);
        }
        return filteredProperties;
    }

    public static void readSystemProperties(final Hashtable<Object, Object> existingProps) {
        Hashtable<Object, Object> systemProperties = AccessController.doPrivileged(new PrivilegedAction<Hashtable<Object, Object>>() {

            public Hashtable<Object, Object> run() {
                return filterProperties(new SystemPropertiesSource());
            }
        });
        mergeEnvironment(systemProperties, existingProps, false);
    }

    public static void readAppletParameters(Object applet, Hashtable<Object, Object> existingProps) {
        if (null != applet) {
            Hashtable<Object, Object> appletParameters = filterProperties(new AppletParametersSource((Applet) applet));
            mergeEnvironment(appletParameters, existingProps, false);
        }
    }

    static Hashtable<Object, Object> readMultipleResourceFiles(final String name, final Hashtable<Object, Object> existingProps, ClassLoader cl) throws NamingException {
        if (null == cl) {
            cl = ClassLoader.getSystemClassLoader();
        }
        Enumeration<URL> e = null;
        try {
            e = cl.getResources(name);
        } catch (IOException ex) {
            ConfigurationException newEx = new ConfigurationException(Messages.getString("jndi.23"));
            newEx.setRootCause(ex);
            throw newEx;
        }
        URL url = null;
        InputStream is = null;
        final Properties p = new Properties();
        while (e.hasMoreElements()) {
            url = e.nextElement();
            try {
                if (null != (is = url.openStream())) {
                    p.load(is);
                    mergeEnvironment(p, existingProps, true);
                    p.clear();
                }
            } catch (IOException ex) {
                ConfigurationException newEx = new ConfigurationException(Messages.getString("jndi.24"));
                newEx.setRootCause(ex);
                throw newEx;
            } finally {
                try {
                    if (null != is) {
                        is.close();
                    }
                } catch (IOException ex) {
                } finally {
                    is = null;
                }
            }
        }
        return existingProps;
    }

    public static Hashtable<Object, Object> readApplicationResourceFiles(final Hashtable<Object, Object> existingProps) throws NamingException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

                public Void run() throws NamingException {
                    readMultipleResourceFiles(APPLICATION_RESOURCE_FILE, existingProps, Thread.currentThread().getContextClassLoader());
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            Exception rootCause = e.getException();
            if (rootCause instanceof NamingException) {
                throw (NamingException) rootCause;
            } else if (rootCause instanceof RuntimeException) {
                throw (RuntimeException) rootCause;
            } else {
            }
        }
        return existingProps;
    }

    public static Hashtable<Object, Object> readLibraryResourceFile(final Hashtable<Object, Object> existingProps) throws NamingException {
        final String sep = System.getProperty("file.separator");
        String resPath = null;
        resPath = AccessController.doPrivileged(new PrivilegedAction<String>() {

            public String run() {
                return System.getProperty("java.home");
            }
        });
        if (!resPath.endsWith(sep)) {
            resPath += sep;
        }
        resPath += "lib" + sep + APPLICATION_RESOURCE_FILE;
        InputStream is = null;
        final File resFile = new File(resPath);
        final Properties p = new Properties();
        boolean resFileExists = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

            public Boolean run() {
                return Boolean.valueOf(resFile.exists());
            }
        }).booleanValue();
        if (resFileExists) {
            try {
                is = AccessController.doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {

                    public FileInputStream run() throws IOException {
                        FileInputStream localInputStream = new FileInputStream(resFile);
                        p.load(localInputStream);
                        return localInputStream;
                    }
                });
                mergeEnvironment(p, existingProps, true);
            } catch (PrivilegedActionException e) {
                ConfigurationException newEx = new ConfigurationException(Messages.getString("jndi.25"));
                newEx.setRootCause(e.getException());
                throw newEx;
            } finally {
                try {
                    if (null != is) {
                        is.close();
                    }
                } catch (IOException ex) {
                }
            }
        }
        return existingProps;
    }

    public static Hashtable<Object, Object> readProviderResourceFiles(final Context context, final Hashtable<Object, Object> existingProps) throws NamingException {
        String factory = context.getClass().getName();
        String resPath = null;
        int len = factory.lastIndexOf('.');
        if (-1 == len) {
            resPath = PROVIDER_RESOURCE_FILE;
        } else {
            resPath = factory.substring(0, len + 1);
            resPath = resPath.replace('.', '/');
            resPath += PROVIDER_RESOURCE_FILE;
        }
        try {
            final String finalResPath = resPath;
            AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {

                public String run() throws NamingException {
                    readMultipleResourceFiles(finalResPath, existingProps, context.getClass().getClassLoader());
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            Exception rootCause = e.getException();
            if (rootCause instanceof NamingException) {
                throw (NamingException) rootCause;
            } else if (rootCause instanceof RuntimeException) {
                throw (RuntimeException) rootCause;
            } else {
                throw new AssertionError(rootCause);
            }
        }
        return existingProps;
    }

    public static String[] getFactoryNamesFromEnvironmentAndProviderResource(Hashtable<?, ?> envmt, Context ctx, String key) throws NamingException {
        List<String> fnames = new ArrayList<String>();
        if (null != envmt) {
            String str = (String) envmt.get(key);
            if (null != str) {
                StringTokenizer st = new StringTokenizer(str, ":");
                while (st.hasMoreTokens()) {
                    fnames.add(st.nextToken());
                }
            }
        }
        if (null != ctx) {
            Hashtable<Object, Object> h = new Hashtable<Object, Object>();
            EnvironmentReader.readProviderResourceFiles(ctx, h);
            String str = (String) h.get(key);
            if (null != str) {
                StringTokenizer st = new StringTokenizer(str, ":");
                while (st.hasMoreTokens()) {
                    fnames.add(st.nextToken());
                }
            }
        }
        if (Context.URL_PKG_PREFIXES.equals(key)) {
            fnames.add("com.sun.jndi.url");
            fnames.add("org.apache.harmony.jndi.provider");
        }
        return fnames.toArray(new String[fnames.size()]);
    }

    private interface JNDIPropertiesSource {

        String getProperty(final String propName);
    }

    private static class SystemPropertiesSource implements JNDIPropertiesSource {

        public SystemPropertiesSource() {
            super();
        }

        public String getProperty(final String propName) {
            return System.getProperty(propName);
        }
    }

    private static class AppletParametersSource implements JNDIPropertiesSource {

        private Applet applet;

        public AppletParametersSource(Applet applet) {
            this.applet = applet;
        }

        public String getProperty(final String propName) {
            return applet.getParameter(propName);
        }
    }
}
