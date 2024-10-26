package javax.activation;

import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.activation.registries.MimeTypeFile;
import com.sun.activation.registries.LogSupport;

/**
 * This class extends FileTypeMap and provides data typing of files
 * via their file extension. It uses the <code>.mime.types</code> format. <p>
 *
 * <b>MIME types file search order:</b><p>
 * The MimetypesFileTypeMap looks in various places in the user's
 * system for MIME types file entries. When requests are made
 * to search for MIME types in the MimetypesFileTypeMap, it searches  
 * MIME types files in the following order:
 * <p>
 * <ol>
 * <li> Programmatically added entries to the MimetypesFileTypeMap instance.
 * <li> The file <code>.mime.types</code> in the user's home directory.
 * <li> The file &lt;<i>java.home</i>&gt;<code>/lib/mime.types</code>.
 * <li> The file or resources named <code>META-INF/mime.types</code>.
 * <li> The file or resource named <code>META-INF/mimetypes.default</code>
 * (usually found only in the <code>activation.jar</code> file).
 * </ol>
 * <p>
 * <b>MIME types file format:</b><p>
 *
 * <code>
 * # comments begin with a '#'<br>
 * # the format is &lt;mime type> &lt;space separated file extensions><br>
 * # for example:<br>
 * text/plain    txt text TXT<br>
 * # this would map file.txt, file.text, and file.TXT to<br>
 * # the mime type "text/plain"<br>
 * </code>
 *
 * @author Bart Calder
 * @author Bill Shannon
 */
public class MimetypesFileTypeMap extends FileTypeMap {

    private static MimeTypeFile defDB = null;

    private MimeTypeFile[] DB;

    private static final int PROG = 0;

    private static String defaultType = "application/octet-stream";

    /**
     * The default constructor.
     */
    public MimetypesFileTypeMap() {
        Vector dbv = new Vector(5);
        MimeTypeFile mf = null;
        dbv.addElement(null);
        if (defDB != null) dbv.addElement(defDB);
        DB = new MimeTypeFile[dbv.size()];
        dbv.copyInto(DB);
    }

    /**
     * Load from the named resource.
     */
    private MimeTypeFile loadResource(String name) {
        InputStream clis = null;
        try {
            clis = SecuritySupport.getResourceAsStream(this.getClass(), name);
            if (clis != null) {
                MimeTypeFile mf = new MimeTypeFile(clis);
                if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: successfully " + "loaded mime types file: " + name);
                return mf;
            } else {
                if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: not loading " + "mime types file: " + name);
            }
        } catch (IOException e) {
            if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: can't load " + name, e);
        } catch (SecurityException sex) {
            if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: can't load " + name, sex);
        } finally {
            try {
                if (clis != null) clis.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
     * Load all of the named resource.
     */
    private void loadAllResources(Vector v, String name) {
        boolean anyLoaded = false;
        try {
            URL[] urls;
            ClassLoader cld = null;
            cld = SecuritySupport.getContextClassLoader();
            if (cld == null) cld = this.getClass().getClassLoader();
            if (cld != null) urls = SecuritySupport.getResources(cld, name); else urls = SecuritySupport.getSystemResources(name);
            if (urls != null) {
                if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: getResources");
                for (int i = 0; i < urls.length; i++) {
                    URL url = urls[i];
                    InputStream clis = null;
                    if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: URL " + url);
                    try {
                        clis = SecuritySupport.openStream(url);
                        if (clis != null) {
                            v.addElement(new MimeTypeFile(clis));
                            anyLoaded = true;
                            if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: " + "successfully loaded " + "mime types from URL: " + url);
                        } else {
                            if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: " + "not loading " + "mime types from URL: " + url);
                        }
                    } catch (IOException ioex) {
                        if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: can't load " + url, ioex);
                    } catch (SecurityException sex) {
                        if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: can't load " + url, sex);
                    } finally {
                        try {
                            if (clis != null) clis.close();
                        } catch (IOException cex) {
                        }
                    }
                }
            }
        } catch (Exception ex) {
            if (LogSupport.isLoggable()) LogSupport.log("MimetypesFileTypeMap: can't load " + name, ex);
        }
        if (!anyLoaded) {
            LogSupport.log("MimetypesFileTypeMap: !anyLoaded");
            MimeTypeFile mf = loadResource("/" + name);
            if (mf != null) v.addElement(mf);
        }
    }

    /**
     * Load the named file.
     */
    private MimeTypeFile loadFile(String name) {
        MimeTypeFile mtf = null;
        try {
            mtf = new MimeTypeFile(name);
        } catch (IOException e) {
        }
        return mtf;
    }

    /**
     * Construct a MimetypesFileTypeMap with programmatic entries
     * added from the named file.
     *
     * @param mimeTypeFileName	the file name
     */
    public MimetypesFileTypeMap(String mimeTypeFileName) throws IOException {
        this();
        DB[PROG] = new MimeTypeFile(mimeTypeFileName);
    }

    /**
     * Construct a MimetypesFileTypeMap with programmatic entries
     * added from the InputStream.
     *
     * @param is	the input stream to read from
     */
    public MimetypesFileTypeMap(InputStream is) {
        this();
        try {
            DB[PROG] = new MimeTypeFile(is);
        } catch (IOException ex) {
        }
    }

    /**
     * Prepend the MIME type values to the registry.
     *
     * @param mime_types A .mime.types formatted string of entries.
     */
    public synchronized void addMimeTypes(String mime_types) {
        if (DB[PROG] == null) DB[PROG] = new MimeTypeFile();
        DB[PROG].appendToRegistry(mime_types);
    }

    /**
     * Return the MIME type of the file object.
     * The implementation in this class calls
     * <code>getContentType(f.getName())</code>.
     *
     * @param f	the file
     * @return	the file's MIME type
     */
    public String getContentType(File f) {
        return this.getContentType(f.getName());
    }

    /**
     * Return the MIME type based on the specified file name.
     * The MIME type entries are searched as described above under
     * <i>MIME types file search order</i>.
     * If no entry is found, the type "application/octet-stream" is returned.
     *
     * @param filename	the file name
     * @return		the file's MIME type
     */
    public synchronized String getContentType(String filename) {
        int dot_pos = filename.lastIndexOf(".");
        if (dot_pos < 0) return defaultType;
        String file_ext = filename.substring(dot_pos + 1);
        if (file_ext.length() == 0) return defaultType;
        for (int i = 0; i < DB.length; i++) {
            if (DB[i] == null) continue;
            String result = DB[i].getMIMETypeString(file_ext);
            if (result != null) return result;
        }
        return defaultType;
    }
}
