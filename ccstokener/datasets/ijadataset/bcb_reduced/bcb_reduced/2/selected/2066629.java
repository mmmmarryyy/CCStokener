package org.jgroups.conf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.Map;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;
import org.jgroups.util.Util;
import org.w3c.dom.Element;

/**
 * The ConfigurationFactory is a factory that returns a protocol stack configurator.
 * The protocol stack configurator is an object that read a stack configuration and
 * parses it so that the ProtocolStack can create a stack.
 * <BR>
 * Currently the factory returns one of the following objects:<BR>
 * 1. XmlConfigurator - parses XML files<BR>
 * 2. PlainConfigurator - uses the old style strings UDP:FRAG: etc etc<BR>
 *
 * @author Filip Hanik (<a href="mailto:filip@filip.net">filip@filip.net)
 * @author Bela Ban
 * @version $Id: ConfiguratorFactory.java,v 1.29 2009/05/13 13:07:04 belaban Exp $
 */
public class ConfiguratorFactory {

    public static final String JAXP_MISSING_ERROR_MSG = "JAXP Error: the required XML parsing classes are not available; " + "make sure that JAXP compatible libraries are in the classpath.";

    static final Log log = LogFactory.getLog(ConfiguratorFactory.class);

    protected ConfiguratorFactory() {
    }

    /**
     * Returns a protocol stack configurator based on the XML configuration provided by the specified File.
     *
     * @param file a File with a JGroups XML configuration.
     *
     * @return a <code>ProtocolStackConfigurator</code> containing the stack configuration.
     *
     * @throws ChannelException if problems occur during the configuration of the protocol stack.
     */
    public static ProtocolStackConfigurator getStackConfigurator(File file) throws ChannelException {
        try {
            checkJAXPAvailability();
            InputStream input = getConfigStream(file);
            return XmlConfigurator.getInstance(input);
        } catch (Exception ex) {
            throw createChannelConfigurationException(ex);
        }
    }

    /**
     * Returns a protocol stack configurator based on the XML configuration provided at the specified URL.
     *
     * @param url a URL pointing to a JGroups XML configuration.
     *
     * @return a <code>ProtocolStackConfigurator</code> containing the stack configuration.
     *
     * @throws ChannelException if problems occur during the configuration of the protocol stack.
     */
    public static ProtocolStackConfigurator getStackConfigurator(URL url) throws ChannelException {
        try {
            checkForNullConfiguration(url);
            checkJAXPAvailability();
            return XmlConfigurator.getInstance(url);
        } catch (IOException ioe) {
            throw createChannelConfigurationException(ioe);
        }
    }

    /**
     * Returns a protocol stack configurator based on the XML configuration provided by the specified XML element.
     *
     * @param element a XML element containing a JGroups XML configuration.
     *
     * @return a <code>ProtocolStackConfigurator</code> containing the stack configuration.
     *
     * @throws ChannelException if problems occur during the configuration of the protocol stack.
     */
    public static ProtocolStackConfigurator getStackConfigurator(Element element) throws ChannelException {
        try {
            checkForNullConfiguration(element);
            return XmlConfigurator.getInstance(element);
        } catch (IOException ioe) {
            throw createChannelConfigurationException(ioe);
        }
    }

    /**
     * Returns a protocol stack configurator based on the provided properties
     * string.
     *
     * @param properties an old style property string, a string representing a system resource containing a JGroups
     *                   XML configuration, a string representing a URL pointing to a JGroups XML configuration,
     *                   or a string representing a file name that contains a JGroups XML configuration.
     */
    public static ProtocolStackConfigurator getStackConfigurator(String properties) throws ChannelException {
        if (properties == null) properties = JChannel.DEFAULT_PROTOCOL_STACK;
        XmlConfigurator configurator = null;
        try {
            checkForNullConfiguration(properties);
            configurator = getXmlConfigurator(properties);
        } catch (IOException ioe) {
            throw createChannelConfigurationException(ioe);
        }
        if (configurator != null) {
            return configurator;
        } else {
            return new PlainConfigurator(properties);
        }
    }

    /**
     * Returns a protocol stack configurator based on the properties passed in.<BR>
     * If the properties parameter is a plain string UDP:FRAG:MERGE:GMS etc, a PlainConfigurator is returned.<BR>
     * If the properties parameter is a string that represents a url for example http://www.filip.net/test.xml
     * or the parameter is a java.net.URL object, an XmlConfigurator is returned<BR>
     *
     * @param properties old style property string, url string, or java.net.URL object
     * @return a ProtocolStackConfigurator containing the stack configuration
     * @throws IOException if it fails to parse the XML content
     * @throws IOException if the URL is invalid or a the content can not be reached
     * @deprecated Used by the JChannel(Object) constructor which has been deprecated.
     */
    public static ProtocolStackConfigurator getStackConfigurator(Object properties) throws IOException {
        InputStream input = null;
        if (properties == null) properties = JChannel.DEFAULT_PROTOCOL_STACK;
        if (properties instanceof URL) {
            try {
                input = ((URL) properties).openStream();
            } catch (Throwable t) {
            }
        }
        if (input == null && properties instanceof String) {
            try {
                input = new URL((String) properties).openStream();
            } catch (Exception ignore) {
            }
            if (input == null && ((String) properties).endsWith("xml")) {
                try {
                    input = Util.getResourceAsStream((String) properties, ConfiguratorFactory.class);
                } catch (Throwable ignore) {
                }
            }
            if (input == null) {
                try {
                    input = new FileInputStream((String) properties);
                } catch (Throwable t) {
                }
            }
        }
        if (input == null && properties instanceof File) {
            try {
                input = new FileInputStream((File) properties);
            } catch (Throwable t) {
            }
        }
        if (input != null) {
            return XmlConfigurator.getInstance(input);
        }
        if (properties instanceof Element) {
            return XmlConfigurator.getInstance((Element) properties);
        }
        return new PlainConfigurator((String) properties);
    }

    public static InputStream getConfigStream(File file) throws Exception {
        try {
            checkForNullConfiguration(file);
            return new FileInputStream(file);
        } catch (IOException ioe) {
            throw createChannelConfigurationException(ioe);
        }
    }

    public static InputStream getConfigStream(URL url) throws Exception {
        try {
            checkJAXPAvailability();
            return url.openStream();
        } catch (Exception ex) {
            throw createChannelConfigurationException(ex);
        }
    }

    /**
     * Returns a JGroups XML configuration InputStream based on the provided properties string.
     *
     * @param properties a string representing a system resource containing a JGroups XML configuration, a string
     *                   representing a URL pointing to a JGroups ML configuration, or a string representing
     *                   a file name that contains a JGroups XML configuration.
     *
     * @throws IOException  if the provided properties string appears to be a valid URL but is unreachable.
     */
    public static InputStream getConfigStream(String properties) throws IOException {
        InputStream configStream = null;
        try {
            configStream = new FileInputStream(properties);
        } catch (FileNotFoundException fnfe) {
        } catch (AccessControlException access_ex) {
        }
        if (configStream == null) {
            try {
                configStream = new URL(properties).openStream();
            } catch (MalformedURLException mre) {
            }
        }
        if (configStream == null && properties.endsWith("xml")) {
            configStream = Util.getResourceAsStream(properties, ConfiguratorFactory.class);
        }
        return configStream;
    }

    public static InputStream getConfigStream(Object properties) throws IOException {
        InputStream input = null;
        if (properties == null) properties = JChannel.DEFAULT_PROTOCOL_STACK;
        if (properties instanceof URL) {
            try {
                input = ((URL) properties).openStream();
            } catch (Throwable t) {
            }
        }
        if (input == null && properties instanceof String) {
            input = getConfigStream((String) properties);
        }
        if (input == null && properties instanceof File) {
            try {
                input = new FileInputStream((File) properties);
            } catch (Throwable t) {
            }
        }
        if (input != null) return input;
        if (properties instanceof Element) {
            return getConfigStream(properties);
        }
        return new ByteArrayInputStream(((String) properties).getBytes());
    }

    /**
     * Returns an XmlConfigurator based on the provided properties string (if possible).
     *
     * @param properties a string representing a system resource containing a
     *                   JGroups XML configuration, a string representing a URL
     *                   pointing to a JGroups ML configuration, or a string
     *                   representing a file name that contains a JGroups XML
     *                   configuration.
     *
     * @return an XmlConfigurator instance based on the provided properties
     *         string; <code>null</code> if the provided properties string does
     *         not point to an XML configuration.
     *
     * @throws IOException  if the provided properties string appears to be a
     *                      valid URL but is unreachable, or if the JGroups XML
     *                      configuration pointed to by the URL can not be
     *                      parsed.
     */
    static XmlConfigurator getXmlConfigurator(String properties) throws IOException {
        XmlConfigurator returnValue = null;
        InputStream configStream = getConfigStream(properties);
        if (configStream != null) {
            checkJAXPAvailability();
            returnValue = XmlConfigurator.getInstance(configStream);
        }
        return returnValue;
    }

    /**
     * Creates a <code>ChannelException</code> instance based upon a
     * configuration problem.
     *
     * @param cause the exceptional configuration condition to be used as the
     *              created <code>ChannelException</code>'s cause.
     */
    static ChannelException createChannelConfigurationException(Throwable cause) {
        return new ChannelException("unable to load the protocol stack", cause);
    }

    /**
     * Check to see if the specified configuration properties are
     * <code>null</null> which is not allowed.
     *
     * @param properties the specified protocol stack configuration.
     *
     * @throws NullPointerException if the specified configuration properties
     *                              are <code>null</code>.
     */
    static void checkForNullConfiguration(Object properties) {
        if (properties == null) throw new NullPointerException("the specifed protocol stack configuration was null");
    }

    /**
     * Checks the availability of the JAXP classes on the classpath.
     *
     * @throws NoClassDefFoundError if the required JAXP classes are not
     *                              availabile on the classpath.
     */
    static void checkJAXPAvailability() {
        try {
            XmlConfigurator.class.getName();
        } catch (NoClassDefFoundError error) {
            Error tmp = new NoClassDefFoundError(JAXP_MISSING_ERROR_MSG);
            tmp.initCause(error);
            throw tmp;
        }
    }

    /**
     * Replace variables of the form ${var:default} with the getProperty(var,
     * default)
     * 
     * @param configurator
     */
    public static void substituteVariables(ProtocolStackConfigurator configurator) {
        ProtocolData[] protocols = null;
        try {
            protocols = configurator.getProtocolStack();
            for (ProtocolData protocol : protocols) {
                if (protocol != null) {
                    Map<String, ProtocolParameter> parms = protocol.getParameters();
                    ProtocolParameter parm;
                    for (Map.Entry<String, ProtocolParameter> entry : parms.entrySet()) {
                        parm = entry.getValue();
                        String val = parm.getValue();
                        String replacement = Util.substituteVariable(val);
                        if (!replacement.equals(val)) {
                            parm.setValue(replacement);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
