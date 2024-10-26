package se.kth.cid.component.xml;

import se.kth.cid.component.*;
import se.kth.cid.component.local.*;
import se.kth.cid.util.*;
import se.kth.cid.identity.*;
import se.kth.cid.neuron.*;
import se.kth.cid.neuron.local.*;
import se.kth.cid.conceptmap.*;
import se.kth.cid.conceptmap.local.*;
import se.kth.cid.component.xml.*;
import se.kth.cid.xml.*;
import java.io.*;
import java.util.*;

/** This is the FormatHandler that are used with XML documents (text/xml).
 *
 *  @author Mikael Nilsson
 *  @version $Revision: 401 $
 */
public class XmlFormatHandler implements FormatHandler {

    /** Takes care of multiple ftp-connections for you.*/
    FtpHandler ftpHandler;

    /** The version of the Component DTD we generate and parse.
   */
    public static final String COMPONENT_DTD_VERSION = "1.0";

    /** The class doing the actual loading.
   */
    XmlComponentIO componentIO;

    /** Constructs an XmlFormatLoader.
   */
    public XmlFormatHandler() {
        componentIO = new XmlComponentIO();
        ftpHandler = new FtpHandler();
    }

    public void setComponentStore(ComponentStore store) {
    }

    public MIMEType getMIMEType() {
        return MIMEType.XML;
    }

    public boolean canHandleURI(URI uri) {
        return uri.toString().indexOf('#') == -1;
    }

    public Container loadContainer(URI uri, URI origuri) throws ComponentException {
        throw new ComponentException("No XML-continers supported.");
    }

    public Component loadComponent(URI uri, URI origuri) throws ComponentException {
        try {
            Component comp = null;
            InputStream is = null;
            java.net.URL url = null;
            try {
                url = uri.getJavaURL();
            } catch (java.net.MalformedURLException e) {
                throw new ComponentException("Invalid URL " + uri + " for component " + origuri + ":\n " + e.getMessage());
            }
            try {
                if (url.getProtocol().equals("ftp")) is = ftpHandler.getInputStream(url); else {
                    java.net.URLConnection conn = url.openConnection();
                    conn.connect();
                    is = conn.getInputStream();
                }
            } catch (IOException e) {
                if (is != null) is.close();
                throw new ComponentException("IO error loading URL " + url + " for component " + origuri + ":\n " + e.getMessage());
            }
            try {
                comp = componentIO.loadComponent(origuri, uri, is, isSavable(uri));
            } catch (ComponentException e) {
                if (is != null) is.close();
                throw new ComponentException("Error loading component " + origuri + " from " + url + ":\n " + e.getMessage());
            }
            is.close();
            return comp;
        } catch (IOException ioe) {
            Tracer.debug("didn't manage to close inputstream....");
            return null;
        }
    }

    File getFile(URI uri) {
        if (uri instanceof FileURL) return ((FileURL) uri).getJavaFile();
        return null;
    }

    OutputStream getOutputStream(URI uri) throws IOException {
        File f = getFile(uri);
        if (f != null) return new FileOutputStream(f);
        java.net.URL url = uri.getJavaURL();
        if (url.getProtocol().equalsIgnoreCase("ftp")) return ftpHandler.getOutputStream(url);
        return null;
    }

    public boolean isSavable(URI uri) {
        File f = getFile(uri);
        if (f != null) {
            try {
                if (f.isFile()) return f.canWrite(); else if (!f.exists()) return (new File(f.getParent())).canWrite();
            } catch (SecurityException se) {
                return false;
            }
        }
        java.net.URL url = null;
        try {
            url = uri.getJavaURL();
        } catch (java.net.MalformedURLException mue) {
            return false;
        }
        if (url.getProtocol().equalsIgnoreCase("ftp")) try {
            ftpHandler.isWritable(url);
            return true;
        } catch (IOException io) {
        }
        return false;
    }

    public void checkCreateComponent(URI uri) throws ComponentException {
        File f = getFile(uri);
        try {
            if (f != null) {
                File d = new File(f.getParent());
                if (d.isDirectory()) {
                    if (!(f.exists()) && new File(f.getParent()).canWrite()) return;
                    if (f.exists()) throw new ComponentException("file already exists.");
                    d = new File(f.getParent());
                    if (!d.canWrite()) throw new ComponentException("Cant write file since it's containing directory is write protected.");
                } else {
                    if (d.exists()) throw new ComponentException("Intended continaing directoy isn't a directory, it's a file.");
                    d = new File(d.getParent());
                    while (d != null && !d.isDirectory()) {
                        if (d.exists()) throw new ComponentException("The subpath " + d.toString() + ", is a file.\n" + "Hence you can't create the neccessary path for this file.");
                        d = new File(d.getParent());
                    }
                    if (d == null) throw new ComponentException("Invalid basepath.");
                    if (!d.canWrite()) throw new ComponentException("Can't create path since " + d.toString() + " isn't writable."); else throw new FilePathComponentException("Path not valid, missing directorys.", f);
                }
            }
        } catch (SecurityException se) {
            throw new ComponentException("Cannot create file, \n" + "securityexception:" + se.getMessage());
        }
        java.net.URL url;
        try {
            url = uri.getJavaURL();
        } catch (java.net.MalformedURLException mue) {
            throw new ComponentException("MalformedURLException: " + mue.getMessage());
        }
        if (url.getProtocol().equalsIgnoreCase("ftp")) {
            try {
                ftpHandler.isWritable(url);
            } catch (IOException io) {
                throw new ComponentException("Component with uri" + uri.toString() + " won't be savable:\n" + io.getMessage());
            }
            if (!ftpHandler.isValidPath(url)) {
                List[] list = ftpHandler.tellValidPath(url);
                if (list != null) throw new FtpPathComponentException("Path not valid!", ftpHandler, list, url);
            }
            try {
                ftpHandler.getInputStream(url).close();
                throw new ComponentException("Something (maybee a component) with uri" + uri.toString() + " already exists!");
            } catch (IOException io) {
            }
            return;
        }
        throw new ComponentException("Cannot create other than file: or ftp:user:password@host/filepath components");
    }

    public Component createComponent(URI uri, URI realURI, String type, Object extras) throws ComponentException {
        checkCreateComponent(uri);
        if (type == COMPONENT) return new LocalComponent(realURI, uri, MIMEType.XML); else if (type == NEURON) return new LocalNeuron(realURI, uri, MIMEType.XML, (URI) extras); else if (type == NEURONTYPE) return new LocalNeuronType(realURI, uri, MIMEType.XML); else if (type == CONCEPTMAP) return new LocalConceptMap(realURI, uri, MIMEType.XML); else throw new ComponentException("Unknown Component type: '" + type + "'");
    }

    public void saveComponent(URI uri, Component comp) throws ComponentException {
        try {
            OutputStream os = getOutputStream(uri);
            componentIO.printComponent(comp, os);
            os.close();
        } catch (IOException e) {
            throw new ComponentException("IO error saving component: " + uri + ":\n " + e.getMessage());
        }
    }
}
