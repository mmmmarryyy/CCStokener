package org.epoline.jsf.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.epoline.jsf.entries.ServiceAttribute;

public final class Util {

    static final Logger logger = Log4jManager.getLogger(Util.class.getName());

    /**
	 * Get the interface of a certain class
     * except:
     * <UL>
     * <LI>Serializable</LI>
     * <LI>net.jini.*</LI>
     * <LI>JSFAdministrable</LI>
     * </UL>
	 * @param proxy
	 * @return Class	the interface used by the <code>proxy</code>.
	 */
    public static final Class getServiceInterface(Object proxy) {
        if (proxy == null) return null;
        Class[] clazz = proxy.getClass().getInterfaces();
        for (int walk = 0; walk < clazz.length; walk++) {
            if ((!clazz[walk].getName().equals("java.io.Serializable")) && (!clazz[walk].getName().startsWith("net.jini")) && (!clazz[walk].getName().equals("org.epoline.jsf.services.admin.JSFAdministrable"))) {
                return clazz[walk];
            }
        }
        return null;
    }

    /**
	 * Set the properties from the file passed to this JVM
	 * identified by 'startupURL'.
     * <p>
     * Note: The loaded properties are added to the System properties
     * </p>
	 * @param startupURL Specifies the location of the startup-properties file (URL or File based)
	 * @deprecated Assuming System Properties can be dangerous. Use {@link #getJVMProperties} instead.
	 * @since v1_0_rc3
	 */
    public static final void ReadJVMProperties(String startupURL) throws IOException {
        Properties properties = getJVMProperties("startupURL", startupURL);
        if (null == properties) return;
        Properties systemProperties = System.getProperties();
        for (Enumeration walk = properties.keys(); walk.hasMoreElements(); ) {
            String property = (String) walk.nextElement();
            systemProperties.put(property, (String) properties.get(property));
        }
        System.setProperties(systemProperties);
    }

    /**
     * Get the properties from the file passed to this JVM
     * identified by 'startupURL'.
     * @param propertyName Specifies the location of the startup-properties file (URL or File based)
     * @param propertyValue value of propertyName
     * @return properties The props read from the URL as specified in startupURL
     */
    public static final Properties getJVMProperties(String propertyName, String propertyValue) throws IOException {
        Properties properties = new Properties();
        if (propertyValue == null) {
            return null;
        } else {
            URL url = null;
            try {
                url = new URL(propertyValue);
                properties.load(url.openStream());
            } catch (IOException e) {
                throw new IOException("Error reading property file: " + e.getMessage());
            }
        }
        return properties;
    }

    /**
	 * Convert a CSV list like "test,intg,prod" to String[]
     * where ',' is specified as sep
	 * @param csv The list of values
	 * @param sep The seperator character
	 * @return The list of values as String[], or null when csv or sep are null
	 */
    public static String[] CSV2StringArray(String csv, String sep) {
        if (csv == null || sep == null) return null;
        StringTokenizer st = new StringTokenizer(csv, sep);
        String[] result = new String[st.countTokens()];
        st = new StringTokenizer(csv, sep);
        for (int i = 0; st.hasMoreTokens(); i++) {
            result[i] = st.nextToken();
        }
        return (result);
    }

    /**
     * Convert a CSV list like "test,intg,prod" to String[]
     * @param csv The list of values
     * @return The list of values as String[], or null if csv is null
     */
    public static String[] CSV2StringArray(String csv) {
        if (csv == null) return null;
        return CSV2StringArray(csv, ",");
    }

    public static ServiceAttribute[] NvpList2ServiceAttribute(String[] list) {
        ServiceAttribute[] attrs = new ServiceAttribute[list.length];
        StringTokenizer st = null;
        String n, v = null;
        for (int walk = 0, all = list.length; walk < all; walk++) {
            st = new StringTokenizer(list[walk], "=");
            try {
                while (st.hasMoreTokens()) {
                    n = st.nextToken();
                    v = st.nextToken();
                    attrs[walk] = new ServiceAttribute(n, v);
                }
            } catch (NoSuchElementException nsee) {
                logger.warn(nsee.getMessage());
            }
        }
        return attrs;
    }

    /**
	 * Writes set properties for current JVM to disk
	 * @param jsfContext
	 */
    public static void writeContextToDisk(Properties jsfContext, String fileName) {
        if (logger.isDebugEnabled()) logger.debug("+writeContextToDisk");
        if (jsfContext == null) {
            if (logger.isDebugEnabled()) logger.debug("-writeContextToDisk Property parameter is null");
            return;
        }
        if (fileName == null || fileName.trim().length() == 0) {
            if (logger.isDebugEnabled()) logger.debug("-writeContextToDisk Filename is null or empty");
            return;
        }
        try {
            jsfContext.store(new FileOutputStream(fileName), "ServiceProvider context. Generated " + new Date());
        } catch (IOException e) {
            logger.debug("Cannot store property because of an error. Probable reason might be that a property is of type array[].");
        }
        if (logger.isDebugEnabled()) logger.debug("-writeContextToDisk");
    }

    /**
	 * @return list of n &lt;name&gt; = &lt;value&gt;
	 */
    public static String toText(ServiceAttribute[] list) {
        StringBuffer b = new StringBuffer(128);
        for (int walk = 0; walk < list.length; walk++) {
            b.append(walk).append(" name='").append(list[walk].name).append("', value='").append(list[walk].value).append("' ");
        }
        return b.toString();
    }
}
