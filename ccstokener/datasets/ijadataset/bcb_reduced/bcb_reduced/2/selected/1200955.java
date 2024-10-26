package uk.co.marcoratto.util;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import javax.naming.InitialContext;
import javax.naming.Context;
import java.io.FileNotFoundException;
import java.net.URLConnection;
import javax.naming.NamingException;
import org.apache.log4j.Logger;

/**
 * Questa &egrave; la classe base per la gestione dei file di <code>properties</code>. Per utilizzarla &egrave; necessario sviluppae una classe che la estende.
 * <BR>Si consiglia di sviluppare le sottoclassi in modo che implementino il Pattern Singleton.
 * <BR>Inoltre, nel costruttore della sottoclasse, ricordarsi di invocare il costruttore della superclasse (<code>super()</code>).
 * @author Marco Ratto
 */
public abstract class AbstractPropertiesManager {

    protected static Logger logger = Logger.getLogger("uk.co.marcoratto.scp");

    /**
   * Contenitore per tutte le properties contenute nel file.
   */
    protected Properties prop = null;

    /**
   * Questo costruttore deve essere invocato nel costruttore di ogni sottoclasse.
   */
    protected AbstractPropertiesManager() {
        prop = new Properties();
    }

    protected abstract String getPropertiesFilename();

    public void readFileProperties() throws PropertiesManagerException {
        String filename = this.getPropertiesFilename();
        try {
            logger.info("readFileProperties(" + filename + "): looking for file by URL...");
            String fileURL = this.getFileURL();
            InitialContext context = new InitialContext();
            Context c = (Context) context.lookup("java:comp/env");
            logger.info("readFileProperties(" + filename + "): looking up java:comp/env... OK");
            java.net.URL url = (java.net.URL) c.lookup(fileURL);
            logger.info("readFileProperties(" + filename + "): looking up " + url + "... OK");
            logger.info("readFileProperties(" + filename + "): object retrieved " + url.toString());
            readFileProperties(url);
        } catch (NamingException ne) {
            logger.info("readFileProperties(" + filename + "): errore in lookup. " + ne.getMessage());
            logger.info("readFileProperties(" + filename + "): looking for file in classloader...");
            InputStream is = AbstractPropertiesManager.class.getResourceAsStream(filename);
            if (is != null) {
                logger.info("readFileProperties(" + filename + "): looking for file in classloader... OK");
                this.readFileProperties(is);
            } else {
                try {
                    logger.info("readFileProperties(" + filename + "): looking for file in file system...");
                    is = new FileInputStream(filename);
                    logger.info("readFileProperties(" + filename + "): looking for file in file system... OK");
                    this.readFileProperties(is);
                } catch (FileNotFoundException e) {
                    throw new PropertiesManagerException(e);
                }
            }
        }
    }

    /**
   * Acquisisce dal file di properties tutti i parametri, valorizzando l'attributo <code>prop</code>.
   * <BR>Il file di properties &egrave; passato in formato <code>String</code>;
   * <BR>il path pu&ograve; essere relativo al Classloader oppure assoluto sul file system.
   * <BR>Prima si cerca nel classloader e poi nel file system.
   * @param String filename
   * @exception java.io.IOException
   */
    public void readFileProperties(String filename) throws PropertiesManagerException {
        try {
            InputStream is = AbstractPropertiesManager.class.getResourceAsStream(filename);
            if (is == null) {
                is = new FileInputStream(filename);
            }
            prop.load(is);
        } catch (IOException e) {
            throw new PropertiesManagerException(e);
        }
    }

    /**
   * Acquisisce dal file di properties tutti i parametri, valorizzando l'attributo <code>prop</code>.
   * <BR>Il file di properties &egrave; passato in formato <code>InputStream</code>.
   * @param InputStream is
   * @exception java.io.IOException
   */
    public void readFileProperties(InputStream is) throws PropertiesManagerException {
        logger.info("readFileProperties(InputStream): loading file...");
        try {
            prop.load(is);
        } catch (IOException e) {
            throw new PropertiesManagerException(e);
        }
        logger.info("readFileProperties(InputStream): loading file... OK");
    }

    /**
   * Acquisisce dal file di properties tutti i parametri, valorizzando l'attributo <code>prop</code>.
   * <BR>Il file di properties &egrave; passato in formato <code>File</code> ed il suo pathname deve essere assoluto partendo dalla root delle classi.
   * @exception java.io.IOException
   * @param File propFile
   */
    public void readFileProperties(File propFile) throws PropertiesManagerException {
        InputStream is = null;
        try {
            is = new FileInputStream(propFile);
            prop.load(is);
        } catch (FileNotFoundException e) {
            throw new PropertiesManagerException(e);
        } catch (IOException e) {
            throw new PropertiesManagerException(e);
        }
    }

    /**
   * Acquisisce dal file di properties tutti i parametri, valorizzando l'attributo <code>prop</code>.
   * <BR>Il file di properties &egrave; passato in formato <code>URL</code>.
   * @exception java.io.IOException
   * @param URL propUrl
   */
    public void readFileProperties(URL url) throws PropertiesManagerException {
        logger.info("readFileProperties(URL): opening connection...");
        URLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = (URLConnection) url.openConnection();
            if (connection != null) {
                logger.info("readFileProperties(URL): opening connection... OK");
            } else {
                logger.info("readFileProperties(URL): connection null");
            }
            logger.info("readFileProperties(URL): getting inputStream...");
            inputStream = connection.getInputStream();
            logger.info("readFileProperties(URL): getting inputStream... OK");
            this.readFileProperties(inputStream);
        } catch (IOException e) {
            throw new PropertiesManagerException(e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.warn(e);
            }
        }
    }

    /**
   * Il metodo restituisce il valore della property di formato <code>String</code> identificata dal parametro <code>key</code>. Se la property non esiste, il metodo ritorna <code>null</code>.
   * @param String key
   * @return String
   */
    public String getStringProperty(String key) {
        return prop.getProperty(key);
    }

    /**
   * Il metodo restituisce il valore della property di formato <code>String</code> identificata dal parametro <code>key</code>. Se la property non esiste, il metodo ritorna il parametro <code>defaultValue</code>.
   * @param String key
   * @param String defaultValue
   * @return String
   */
    public String getStringProperty(String key, String defaultValue) {
        return prop.getProperty(key, defaultValue);
    }

    /**
   * Imposta una nella properties una coppia <code>(key,value)</code>, con <code>value</code> di tipo <code>String</code>.
   * @throws EccezioneGrave
   * @param String key
   * @param String value
   */
    public void setStringProperty(String key, String value) {
        prop.setProperty(key, value);
    }

    /**
   * Il metodo restituisce il valore della property identificata dal parametro <code>key</code> nel formato <code>int</code>. Se la property non esiste, il metodo ritorna il parametro <code>defaultValue</code>.
   * @param String key
   * @param int defaultValue
   * @return int
   */
    public int getIntProperty(String key, int defaultValue) {
        int value = -1;
        String s = this.getStringProperty(key, null);
        try {
            value = Integer.parseInt(s);
        } catch (Exception nfe) {
            value = defaultValue;
        }
        return value;
    }

    /**
   * Imposta una nella properties una coppia <code>(key,value)</code>, con <code>value</code> di tipo <code>int</code>.
   * @param String key
   * @param int value
   */
    public void setIntProperty(String key, int value) {
        this.setStringProperty(key, "" + value);
    }

    /**
   * Il metodo restituisce il valore della property identificata dal parametro <code>key</code> nel formato <code>long</code>. Se la property non esiste, il metodo ritorna il parametro <code>defaultValue</code>.
   * @param String key
   * @param long defaultValue
   * @return long
   */
    public long getLongProperty(String key, long defaultValue) {
        long value = -1;
        String s = this.getStringProperty(key, null);
        try {
            value = Long.parseLong(s);
        } catch (Exception nfe) {
            value = defaultValue;
        }
        return value;
    }

    /**
   * Imposta una nella properties una coppia <code>(key, value)</code>, con <code>value</code> di tipo <code>long</code>.
   * @param String key
   * @param long value*/
    public void setLongProperty(String key, long value) {
        this.setStringProperty(key, "" + value);
    }

    /**
   * Il metodo restituisce il valore della property identificata dal parametro <code>key</code> nel formato <code>double</code>. Se la property non esiste, il metodo ritorna il parametro <code>defaultValue</code>.
   * @param String key
   * @param double defaultValue
   * @return double
   */
    public double getDoubleProperty(String key, double defaultValue) {
        double value = -1;
        String s = this.getStringProperty(key, null);
        try {
            value = Double.parseDouble(s);
        } catch (Exception nfe) {
            value = defaultValue;
        }
        return value;
    }

    /**
   * Imposta una nella properties una coppia <code>(key, value)</code>, con <code>value</code> di tipo <code>double</code>.
   * @param String key
   * @param double value
   */
    public void setDoubleProperty(String key, double value) {
        this.setStringProperty(key, "" + value);
    }

    /**
   * Il metodo restituisce il valore della property identificata dal parametro <code>key</code> nel formato <code>boolean</code>. Se la property non esiste, il metodo ritorna il parametro <code>defaultValue</code>.
   * @param String key
   * @param boolean defaultValue
   * @return boolean
   */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        boolean value = false;
        String s = this.getStringProperty(key, null);
        try {
            value = s.trim().toLowerCase().equals("true");
        } catch (Exception e) {
            value = defaultValue;
        }
        return value;
    }

    /**
   * Imposta una nella properties una coppia <code>(key, value)</code>, con <code>value</code> di tipo <code>boolean</code>.
   * @param String key
   * @param boolean value
   */
    public void setBooleanProperty(String key, boolean value) {
        this.setStringProperty(key, "" + value);
    }

    /**
   * Il metodo restituisce l'oggetto di tipo <code>java.util.Properties</code> nel quale sono contenute le properties.
   * @return Properties
   */
    protected Properties getProperties() {
        return this.prop;
    }

    protected abstract String getDefaultFileName();

    protected abstract String getFilenamePropertyName();

    protected abstract String getFileURL();
}
