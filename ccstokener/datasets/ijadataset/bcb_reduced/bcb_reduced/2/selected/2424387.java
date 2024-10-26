package org.java.plugin.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java.plugin.util.IoUtil;

/**
 * @version $Id$
 */
final class Util {

    private static Log log = LogFactory.getLog(Util.class);

    private static File tempFolder;

    private static boolean tempFolderInitialized = false;

    static File getTempFolder() throws IOException {
        if (tempFolder != null) {
            return tempFolderInitialized ? tempFolder : null;
        }
        synchronized (Util.class) {
            tempFolder = new File(System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + ".jpf-tool-cache");
            log.debug("libraries cache folder is " + tempFolder);
            File lockFile = new File(tempFolder, "lock");
            if (lockFile.exists()) {
                throw new IOException("can't initialize temporary folder " + tempFolder + " as lock file indicates that it is " + "owned by another JPF instance");
            }
            if (tempFolder.exists()) {
                IoUtil.emptyFolder(tempFolder);
            } else {
                tempFolder.mkdirs();
            }
            if (!lockFile.createNewFile()) {
                throw new IOException("can\'t create lock file in JPF " + "tool temporary folder " + tempFolder);
            }
            lockFile.deleteOnExit();
            tempFolder.deleteOnExit();
            tempFolderInitialized = true;
        }
        return tempFolder;
    }

    static byte[] readUrlContent(final URL url) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        InputStream urlStrm = url.openStream();
        try {
            IoUtil.copyStream(urlStrm, result, 256);
        } finally {
            urlStrm.close();
        }
        return result.toByteArray();
    }

    private Util() {
    }
}
