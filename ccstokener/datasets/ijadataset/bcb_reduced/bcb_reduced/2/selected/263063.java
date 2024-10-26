package xdoclet.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import xdoclet.XDocletMessages;

/**
 * A utility class for handling common filing operations. It also caches loaded files.
 *
 * @author    Ara Abrahamian (ara_e@email.com)
 * @author    Hani Suleiman (hani@formicary.net)
 * @created   Aug 5, 2001
 * @version   $Revision: 1.14 $
 * @todo      Deal with Translator.getString()'s exception better (throw it so the build stops?) in
 *      getFileContent(String)
 */
public final class FileManager {

    private static final int BUFFER_SIZE = 10240;

    private static final Map urlCache = new HashMap();

    /**
     * Gets the URLContent attribute of the FileManager class
     *
     * @param url  Describe what the parameter does
     * @return     The URLContent value
     */
    public static synchronized String getURLContent(URL url) {
        Log log = LogUtil.getLog(FileManager.class, "getURLContent");
        if (url == null) {
            throw new IllegalArgumentException("url shouldn't be null!");
        }
        String content = (String) urlCache.get(url);
        if (content != null) {
            return content;
        }
        try {
            InputStream is = null;
            if ("file".equals(url.getProtocol())) {
                is = new java.io.FileInputStream(url.getFile());
            } else {
                is = url.openStream();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
            pump(is, baos);
            content = new String(baos.toByteArray());
            urlCache.put(url, content);
            return content;
        } catch (FileNotFoundException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(Translator.getString(XDocletMessages.class, XDocletUtilMessages.EXCEPTION_READING_MERGE_FILE, new String[] { e.toString() }));
            return null;
        }
    }

    /**
     * Describe what the method does
     *
     * @param url              Describe what the parameter does
     * @param destination      Describe what the parameter does
     * @exception IOException  Describe the exception
     */
    public static synchronized void writeURLContent(URL url, File destination) throws IOException {
        FileOutputStream fos = new FileOutputStream(destination);
        pump(url.openStream(), fos);
        fos.flush();
        fos.close();
    }

    /**
     * Describe what the method does
     *
     * @param is               Describe what the parameter does
     * @param os               Describe what the parameter does
     * @exception IOException  Describe the exception
     */
    private static void pump(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int lengthRead;
        while ((lengthRead = is.read(buffer)) >= 0) {
            os.write(buffer, 0, lengthRead);
        }
    }
}
