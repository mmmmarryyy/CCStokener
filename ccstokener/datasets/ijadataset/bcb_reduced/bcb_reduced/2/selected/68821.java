package org.gbif.ecat.cfg;

import org.gbif.utils.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Constant values used throughout ECAT applications.
 */
public class DataDirConfig {

    protected static final Logger log = LoggerFactory.getLogger(DataDirConfig.class);

    protected static final String TMP_DIR = "tmp";

    protected static final String LOGGING_DIR = "logging";

    private static final String IMPORT_DIR = "checklists";

    private static final String LUCENE_DIR = "lucene";

    public static final String SESSION_USER = "user";

    protected static final String EXPORT_DIR = "export";

    protected Properties properties;

    protected File dataDir;

    private DataDirConfig() {
    }

    public DataDirConfig(InputStream configStream) throws IOException {
        this.properties = new Properties();
        this.properties.load(configStream);
        setDataDir(properties.getProperty("app.dataDir"));
        log.info("Using DATA DIRECTORY: " + this.dataDir());
    }

    public DataDirConfig(Properties properties) throws IOException {
        this.properties = properties;
        setDataDir(properties.getProperty("app.dataDir"));
        log.info("Using DATA DIRECTORY: " + this.dataDir());
    }

    public static void main(String[] args) {
        DataDirConfigFactory f = new DataDirConfigFactory();
        DataDirConfig cfg = f.getCfg();
    }

    @Deprecated
    public static File systemClasspathFile(String path) {
        File f = null;
        URL url = ClassLoader.getSystemResource(path);
        if (url != null) {
            f = new File(url.getFile());
        }
        return f;
    }

    public static InputStream systemClasspathStream(String path) {
        InputStream in = null;
        URL url = ClassLoader.getSystemResource(path);
        if (url != null) {
            try {
                in = url.openStream();
            } catch (IOException e) {
                log.warn("Cannot open system classpath stream " + path, e);
            }
        }
        return in;
    }

    public InputStream classpathStream(String path) {
        InputStream in = null;
        URL url = getClass().getClassLoader().getResource(path);
        if (url != null) {
            try {
                in = url.openStream();
            } catch (IOException e) {
                log.warn("Cannot open classpath stream " + path, e);
            }
        }
        return in;
    }

    public File dataDir() {
        return dataDir;
    }

    public File dataFile(String path) {
        if (path.startsWith("/")) {
            return new File(path);
        }
        return new File(dataDir, path);
    }

    public String domain() {
        String base = properties.getProperty("app.domain");
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    public File exportFile(String path) {
        return dataFile(EXPORT_DIR + "/" + path);
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(String property) {
        return properties.getProperty(property);
    }

    /**
   * Gets integer property as Integer object and catches exceptions for wrongly formated numbers, returning null in
   * those cases.
   * 
   * @param property
   * @return the property value as an Integer or NULL if not found or other value than integer
   */
    public Integer getPropertyAsInteger(String property) {
        try {
            return Integer.valueOf(properties.getProperty(property));
        } catch (NumberFormatException e) {
        }
        return null;
    }

    /**
   * @return file to the checklist folder used for importing
   */
    public File importDir() {
        return new File(dataDir, IMPORT_DIR);
    }

    public File importDir(Integer resourceID) {
        return new File(dataDir, IMPORT_DIR + "/" + resourceID.toString());
    }

    public File importFile(Integer resourceID, String filename) {
        return new File(importDir(resourceID), filename);
    }

    public File loggingDir() {
        return dataFile(LOGGING_DIR);
    }

    public File loggingFile(String path) {
        return dataFile(LOGGING_DIR + "/" + path);
    }

    public File luceneDir() {
        return dataFile(LUCENE_DIR);
    }

    public File luceneFile(String path) {
        return dataFile(LUCENE_DIR + "/" + path);
    }

    public void setDataDir(String datadir) {
        while (datadir.endsWith("/")) {
            datadir = datadir.substring(0, datadir.length() - 1);
        }
        if (datadir.startsWith("/")) {
            dataDir = new File(datadir);
        } else {
            log.warn("DataDir is a unpredictable relative path");
            dataDir = new File(datadir);
        }
    }

    public void setProperty(String property, String value) {
        properties.setProperty(property, value);
    }

    /**
   * @return file to tmp folder within the data dir
   */
    public File tmpDir() {
        return dataFile(TMP_DIR);
    }

    /**
   * @return file inside the datadir tmp folder with the given subpath
   */
    public File tmpFile(String path) {
        return dataFile(TMP_DIR + "/" + path);
    }

    /**
   * @return file inside the datadir tmp folder that maps to a given URL
   */
    public File tmpFile(URL url) {
        return dataFile(TMP_DIR + "/" + FileUtils.toFilePath(url));
    }
}
