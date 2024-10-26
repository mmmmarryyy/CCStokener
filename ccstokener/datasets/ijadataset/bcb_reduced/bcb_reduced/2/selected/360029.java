package wjhk.jupload2.context;

import java.awt.Cursor;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.Vector;
import javax.swing.JApplet;
import javax.swing.JFrame;
import wjhk.jupload2.JUploadDaemon;
import wjhk.jupload2.exception.JUploadException;

/**
 * Implementation of the {@link JUploadContext}, for an executable, that is: for
 * a stand alone application. One such context is created at run time. Its main
 * capabilities, is to load the properties either by a file in the jar file (see
 * DAEMON_PROPERTIES_FILE), or an URL given to the
 * {@link JUploadDaemon#main(String[])} method.
 * 
 * @see DefaultJUploadContext
 * @see JUploadDaemon
 * @author etienne_sf
 * @version $Revision: 750 $
 */
public class JUploadContextExecutable extends DefaultJUploadContext {

    static final String DEFAULT_PROPERTIES_FILE = "/conf/default_daemon.properties";

    static final String DAEMON_PROPERTIES_FILE = "/conf/daemon.properties";

    /**
     * The main window of the application.
     */
    private JFrame jframe = null;

    /**
     * Content of the /conf/default_deamon.properties file. These value override
     * default value, that would be wrong values for the daemon standalone
     * application.
     */
    protected Properties defaultProperties = null;

    /**
     * Content of the /conf/_deamon.properties file. These value are the
     * properties given to parameterize the daemon, according to the specific
     * needs of the project.
     */
    protected Properties daemonProperties = null;

    /**
     * This constructor does nothing. It should be used by test case only.
     */
    protected JUploadContextExecutable(JFrame jframe) {
        if (jframe == null) {
            throw new IllegalArgumentException("theApplet may not be null");
        }
        this.jframe = jframe;
    }

    /**
     * The constructor of the context, which needs the top level container to be
     * created.
     * 
     * @param jframe The owner TopLevelWindow
     * @param propertiesURL The URL where the configuration properties for the
     *            daemon can be read. If null, the daemon try to read the
     *            /conf/daemon.properties file, in the current jar.
     */
    public JUploadContextExecutable(JFrame jframe, String propertiesURL) {
        if (jframe == null) {
            throw new IllegalArgumentException("The jframe may not be null");
        }
        this.jframe = jframe;
        this.defaultProperties = loadPropertiesFromFileInJar(DEFAULT_PROPERTIES_FILE, null);
        if (propertiesURL == null) {
            this.daemonProperties = loadPropertiesFromFileInJar(DAEMON_PROPERTIES_FILE, this.defaultProperties);
        } else {
            this.daemonProperties = loadPropertiesFromURL(propertiesURL, this.defaultProperties);
        }
        init(jframe, this.jframe);
    }

    /**
     * Creates and loads a property file, and return the loaded result.
     * 
     * @param filename The name of the file, which contains the properties to
     *            load
     * @param defaultProperties The default properties value. Put null if no
     *            default Properties should be used.
     * @return The loaded properties. It's empty if an error occurs.
     */
    Properties loadPropertiesFromFileInJar(String filename, Properties defaultProperties) {
        Properties properties = new Properties(defaultProperties);
        try {
            InputStream isProperties = Class.forName("wjhk.jupload2.JUploadApplet").getResourceAsStream(filename);
            properties.load(isProperties);
            isProperties.close();
        } catch (IOException e1) {
            System.out.println("Error while loading " + filename + " (" + e1.getClass().getName() + ")");
            e1.printStackTrace();
        } catch (ClassNotFoundException e1) {
            System.out.println("Error while loading " + filename + " (" + e1.getClass().getName() + ")");
            e1.printStackTrace();
        }
        return properties;
    }

    /**
     * Creates and loads a property file, and return the loaded result.
     * 
     * @param propertiesURL The url that points to the properties configuration
     *            file for the daemon.
     * @param defaultProperties The default properties value. Put null if no
     *            default Properties should be used.
     * @return The loaded properties. It's empty if an error occurs.
     */
    private Properties loadPropertiesFromURL(String propertiesURL, Properties defaultProperties) {
        Properties properties = new Properties(defaultProperties);
        URL url;
        try {
            url = new URL(propertiesURL);
            URLConnection urlConnection = url.openConnection();
            properties.load(urlConnection.getInputStream());
        } catch (MalformedURLException e) {
            System.out.println("Error while loading url " + propertiesURL + " (" + e.getClass().getName() + ")");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error while loading url " + propertiesURL + " (" + e.getClass().getName() + ")");
            e.printStackTrace();
        }
        return properties;
    }

    /**
     * Get a String parameter value from applet properties or System properties.
     * 
     * @param key The name of the parameter to fetch.
     * @param def A default value which is used, when the specified parameter is
     *            not set.
     * @return The value of the applet parameter (resp. system property). If the
     *         parameter was not specified or no such system property exists,
     *         returns the given default value.
     */
    @Override
    public String getParameter(String key, String def) {
        String paramStr = (this.daemonProperties.getProperty(key) != null ? this.daemonProperties.getProperty(key) : def);
        displayDebugParameterValue(key, paramStr);
        return paramStr;
    }

    /** {@inheritDoc} */
    @Override
    public int getParameter(String key, int def) {
        String paramDef = Integer.toString(def);
        String paramStr = this.daemonProperties.getProperty(key) != null ? this.daemonProperties.getProperty(key) : paramDef;
        displayDebugParameterValue(key, paramStr);
        return parseInt(paramStr, def);
    }

    /** {@inheritDoc} */
    @Override
    public float getParameter(String key, float def) {
        String paramDef = Float.toString(def);
        String paramStr = this.daemonProperties.getProperty(key) != null ? this.daemonProperties.getProperty(key) : paramDef;
        displayDebugParameterValue(key, paramStr);
        return parseFloat(paramStr, def);
    }

    /** {@inheritDoc} */
    @Override
    public long getParameter(String key, long def) {
        String paramDef = Long.toString(def);
        String paramStr = this.daemonProperties.getProperty(key) != null ? this.daemonProperties.getProperty(key) : paramDef;
        displayDebugParameterValue(key, paramStr);
        return parseLong(paramStr, def);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getParameter(String key, boolean def) {
        String paramDef = (def ? "true" : "false");
        String paramStr = this.daemonProperties.getProperty(key) != null ? this.daemonProperties.getProperty(key) : paramDef;
        displayDebugParameterValue(key, paramStr);
        return parseBoolean(paramStr, def);
    }

    /** {@inheritDoc} */
    @Override
    public void displayURL(String url, boolean success) {
        throw new UnsupportedOperationException("JUploadContextExecution.displayURL(): Not implemented yet!");
    }

    /** {@inheritDoc} */
    @Override
    public JApplet getApplet() {
        throw new UnsupportedOperationException("Can't use getApplet(), when using the JUploadDaemon!");
    }

    /** {@inheritDoc} */
    @Override
    public Cursor getCursor() {
        return this.jframe.getCursor();
    }

    /**
     * This class doesn't control the URL. It expects it to be already
     * normalized. No work here. {@inheritDoc}
     */
    @Override
    public String normalizeURL(String url) throws JUploadException {
        return url;
    }

    /** {@inheritDoc} */
    @Override
    public void readCookieFromNavigator(Vector<String> headers) {
        throw new UnsupportedOperationException("Can't use readCookieFromNavigator(), when using the JUploadDaemon!");
    }

    /** {@inheritDoc} */
    @Override
    public void readUserAgentFromNavigator(Vector<String> headers) {
        throw new UnsupportedOperationException("Can't use readUserAgentFromNavigator(), when using the JUploadDaemon!");
    }

    /** {@inheritDoc} */
    @Override
    public Cursor setCursor(Cursor cursor) {
        Cursor previousCursor = this.jframe.getCursor();
        this.jframe.setCursor(cursor);
        return previousCursor;
    }

    /** {@inheritDoc} */
    @Override
    public void showStatus(String status) {
    }
}
