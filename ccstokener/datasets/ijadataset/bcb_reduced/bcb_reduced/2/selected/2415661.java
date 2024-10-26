package nl.chess.it.util.config.resolv;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import nl.chess.it.util.config.ConfigurationException;

/**
 * Opens a file or URL which name is given in an environment variable. Two situations can occur:
 * <ul>
 * <li>file is okay and readable: {@link #getSourceDescription()}and {@link #getProperties()}are
 * garantueed to be not null</li>
 * <li>file is not usable or cannot be read: {@link #getSourceDescription()}and
 * {@link #getProperties()}might be null</li>
 * </ul>
 * <b>Note: </b> this class is still in development; the external interface might change (slightly)
 * later.
 * 
 * @author Guus Bosman (Chess-iT)
 */
public class ConfigurationResolverEnvironmentVariable {

    private Exception exception;

    private String sourceDescription;

    private Properties properties;

    public ConfigurationResolverEnvironmentVariable(final String key) {
        String fileName = System.getProperty(key);
        if (fileName == null) {
            exception = new ConfigurationException("no property for configuration file or URL was given as JVM argument. Please add the following to the java.exe startup command: -D" + key + "=<location of file>");
            return;
        }
        boolean isURL = false;
        try {
            new URL(fileName);
            isURL = true;
        } catch (Exception e) {
            isURL = false;
        }
        if (isURL) {
            String interpretedURLName = null;
            Properties tmpProperties = new Properties();
            try {
                URL url = new URL(fileName);
                interpretedURLName = url.toExternalForm();
                byte[] bytes = readEntireFileContents(url.openStream());
                InputStream is = new ByteArrayInputStream(bytes);
                PropertyInputStreamValidator.validate(is);
                is.reset();
                tmpProperties.load(is);
            } catch (Exception e) {
                exception = new ConfigurationException("environment property '" + key + "' had value '" + fileName + "', which I interpreted as url '" + interpretedURLName + "' but this can't be used to read the configuration from: " + e.getMessage(), e);
                return;
            }
            sourceDescription = "url " + interpretedURLName + " (from environment property '" + key + "')";
            properties = tmpProperties;
        } else {
            File file = new File(fileName);
            String interpretedFileName = null;
            Properties tmpProperties = new Properties();
            try {
                interpretedFileName = file.getCanonicalPath();
                FileInputStream is = new FileInputStream(file);
                PropertyInputStreamValidator.validate(is);
                is = new FileInputStream(file);
                tmpProperties.load(is);
            } catch (Exception e) {
                exception = new ConfigurationException("environment property '" + key + "' had value '" + fileName + "', which I interpreted as '" + interpretedFileName + "' but this can't be used to read the configuration from: " + e.getMessage(), e);
                return;
            }
            sourceDescription = "file " + interpretedFileName + " (from environment property '" + key + "')";
            properties = tmpProperties;
        }
    }

    /**
     * Returns the Exception that occurred while reading from the environment variable (if any).
     * 
     * @return Might be <code>null</code>.
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Description of the file or URL read.
     * 
     * @return If {@link #getException()}was not null then this might be <code>null</code>.
     */
    public String getSourceDescription() {
        return sourceDescription;
    }

    /**
     * Returns Properties object for configuration
     * 
     * @return If {@link #getException()}was not null then this might be <code>null</code>.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Reads a file into a byte-array. When an Exception is thrown the inputstream will always be
     * closed. Memory expensive method, use only with limited size of InputStreams.
     * 
     * @param toRead the file to be read
     * @return a file data byte-array
     * @throws NullPointerException If toRead is <code>null</code>.
     * @throws IOException if reading the file fails somehow
     */
    private static byte[] readEntireFileContents(InputStream toRead) throws IOException {
        if (toRead == null) {
            throw new NullPointerException("toRead cannot be null here.");
        }
        InputStream in = null;
        byte[] buf = null;
        int bufLen = 10000 * 1024;
        try {
            in = new BufferedInputStream(toRead);
            buf = new byte[bufLen];
            byte[] tmp = null;
            int len = 0;
            List data = new ArrayList();
            while ((len = in.read(buf, 0, bufLen)) != -1) {
                tmp = new byte[len];
                System.arraycopy(buf, 0, tmp, 0, len);
                data.add(tmp);
            }
            len = 0;
            if (data.size() == 1) {
                return (byte[]) data.get(0);
            }
            for (int i = 0; i < data.size(); i++) len += ((byte[]) data.get(i)).length;
            buf = new byte[len];
            len = 0;
            for (int i = 0; i < data.size(); i++) {
                tmp = (byte[]) data.get(i);
                System.arraycopy(tmp, 0, buf, len, tmp.length);
                len += tmp.length;
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return buf;
    }
}
