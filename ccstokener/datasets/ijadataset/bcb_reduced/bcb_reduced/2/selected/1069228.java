package hu.csq.dyneta.misc;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 * This class simlifies the XML handling.
 *
 * Allows one statement loading and saving of objects.
 *
 * @author Tamás Cséri
 */
public class XMLHandler {

    XStream xstream;

    public XMLHandler(XStream xstream) {
        this.xstream = xstream;
    }

    /**
     * Tries to load an Object from the specified InputStream.
     * The InputStream will not be closed, it has to be closed manually.
     *
     * The input is assumed to be in UTF-8 charset.
     *
     * This function logs its errors.
     *
     * @param xstream
     * @param is
     * @return The read object if succeed, <tt>null</tt> otherwise.
     */
    public Object loadFromXML(InputStream is) {
        Object rv;
        try {
            rv = xstream.fromXML(new BufferedReader(new InputStreamReader(is, "UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(XMLHandler.class.getName()).fatal("Unsupported UTF-8 encoding", ex);
            rv = null;
        } catch (XStreamException ex) {
            Logger.getLogger(XMLHandler.class.getName()).error("Configuration file invalid", ex);
            rv = null;
        }
        return rv;
    }

    /**
     * Tries to load an Object from the specified File.
     * The file is opened and closed.
     *
     * The input is assumed to be in UTF-8 charset.
     *
     * @param xstream
     * @param is
     * @return
     */
    public Object loadFromXML(String fileName) {
        Logger.getLogger(XMLHandler.class.getName()).debug("Loading XML from file: " + fileName);
        FileInputStream is = null;
        Object rv = null;
        try {
            rv = loadFromXML(is = new FileInputStream(fileName));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(XMLHandler.class.getName()).error("Configuration file not found", ex);
            rv = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    Logger.getLogger(XMLHandler.class.getName()).error("Cannot close output stream of the URL", ex);
                }
            }
        }
        return rv;
    }

    /**
     * Tries to load an Object from the specified <tt>URL</tt>.
     * The <tt>URL</tt> is opened and closed before returning.
     *
     * The input is assumed to be in UTF-8 charset.
     * 
     * @param xstream
     * @param is
     * @return
     */
    public Object loadFromXML(URL url) {
        Logger.getLogger(XMLHandler.class.getName()).debug("Loading XML from URL: " + url.toString());
        InputStream is = null;
        Object rv = null;
        try {
            rv = loadFromXML(is = url.openStream());
        } catch (IOException ex) {
            Logger.getLogger(XMLHandler.class.getName()).error("Error when loading from URL: " + url.toString(), ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    Logger.getLogger(XMLHandler.class.getName()).error("Cannot close output stream of the URL", ex);
                }
            }
        }
        return rv;
    }

    /**
     * Saves the object to a file using XStream.
     * 
     * Saves the file using UTF-8 charset.
     *
     * 
     * @return true if succeed, false otherwise.
     */
    public boolean saveToXML(String fileName, Object objectToSave) {
        Logger.getLogger(XMLHandler.class.getName()).debug("Saving object to XML file: " + fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
            xstream.toXML(objectToSave, new OutputStreamWriter(fos, "UTF-8"));
            return true;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(XMLHandler.class.getName()).fatal("Unsupported UTF-8 encoding", ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(XMLHandler.class.getName()).error(null, ex);
        } catch (XStreamException ex) {
            Logger.getLogger(XMLHandler.class.getName()).error("XStream error", ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    Logger.getLogger(XMLHandler.class.getName()).error("Error when closing file.", ex);
                }
            }
        }
        return false;
    }
}
