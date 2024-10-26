package org.ensembl.registry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import org.ensembl.driver.ConfigurationException;
import org.ensembl.util.PropertiesUtil;
import org.python.core.PyException;
import org.python.util.PythonInterpreter;

/**
 * Runs a jython script that can load DriverGroups into a registry.
 * 
 * TODO describe how to create jython script.
 * 
 * TODO implement script support (this is a stub class at moment).
 * 
 * TODO validate script in constructors?
 * 
 * TODO investigate security implications of allowing user to run arbitrary
 * script!
 * 
 * Jython script can be used as glue to load registry based on data from ANY
 * datasource.
 * 
 * Requires including jython.jar in the CLASSPATH.
 * 
 * Errors in the script are detected at runtime when load(..) is called.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp </a>
 */
public class RegistryLoaderJython implements RegistryLoader {

    private static final Logger logger = Logger.getLogger(RegistryLoaderJython.class.getName());

    private byte[] script = null;

    private String scriptDescription = null;

    /**
   * Loads DriverGroups for configuration defined in _srcConfig_.
   * 
   * @param config
   *          configuration.
   * @param configDescription
   *          description of configuration.
   * @throws ConfigurationException
   * @throws IOException
   */
    public RegistryLoaderJython(InputStream config, String configDescription) throws ConfigurationException, IOException {
        script = stream2String(config);
    }

    /**
   * Loads the script stored in byte array.
   * 
   * This can be used for loading configuration values from a a String like
   * this: <code>new RegistryLoaderJython(string.getBytes(), "my script")</code>.
   * 
   * @param script
   *          jython script.
   * @param scriptDescription
   *          description of configuration.
   * @throws ConfigurationException
   * @throws IOException
   */
    public RegistryLoaderJython(byte[] script, String scriptDescription) throws ConfigurationException, IOException {
        this.script = script;
        this.scriptDescription = scriptDescription;
    }

    /**
   * Loads the script stored at _url_.
   * 
   * @param url
   *          url of script.
   * @throws IOException
   */
    public RegistryLoaderJython(String url) throws IOException {
        URL realUrl = PropertiesUtil.stringToURL(url);
        if (realUrl == null) throw new RuntimeException("Can not find registry file:" + url);
        scriptDescription = url;
        InputStream is = realUrl.openStream();
        script = stream2String(is);
        is.close();
    }

    /**
   * Loads DriverGroups for configuration at _url_.
   * 
   * @param url
   *          url of configuration file.
   */
    public RegistryLoaderJython(URL url) throws IOException {
        scriptDescription = url.toExternalForm();
        InputStream is = url.openStream();
        script = stream2String(is);
        is.close();
    }

    private byte[] stream2String(InputStream scriptStream) throws ConfigurationException, IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        for (byte b; (b = (byte) scriptStream.read()) != -1; ) buf.write(b);
        return buf.toByteArray();
    }

    /**
   * Runs the script with _registry_ available in the environment.
   * 
   * @throws PyException
   *           if there was a problem executing the script.
   */
    public synchronized void load(Registry registry) {
        logger.info("registry BEFORE running script: " + registry);
        PythonInterpreter interp = new PythonInterpreter();
        interp.set("registry", registry);
        interp.execfile(new ByteArrayInputStream(script), scriptDescription);
        logger.info("registry AFTER: running script" + registry);
    }
}
