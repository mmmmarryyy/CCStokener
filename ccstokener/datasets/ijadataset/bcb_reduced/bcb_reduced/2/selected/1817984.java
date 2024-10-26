package org.merlotxml.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.merlotxml.merlot.MerlotDebug;

/**
 * File utilities
 *
 * @author	Rick Boykin
 */
public class FileUtil {

    private FileUtil() {
    }

    /**
	 * Utility method to copy a file from one directory to another
	 */
    public static void copyFile(String fileName, String fromDir, String toDir) throws IOException {
        copyFile(new File(fromDir + File.separator + fileName), new File(toDir + File.separator + fileName));
    }

    /**
	 * Utility method to copy a file from one directory to another
	 */
    public static void copyFile(File from, File to) throws IOException {
        if (!from.canRead()) {
            throw new IOException("Cannot read file '" + from + "'.");
        }
        if (to.exists() && (!to.canWrite())) {
            throw new IOException("Cannot write to file '" + to + "'.");
        }
        FileInputStream fis = new FileInputStream(from);
        FileOutputStream fos = new FileOutputStream(to);
        byte[] buf = new byte[1024];
        int bytesLeft;
        while ((bytesLeft = fis.available()) > 0) {
            if (bytesLeft >= buf.length) {
                fis.read(buf);
                fos.write(buf);
            } else {
                byte[] smallBuf = new byte[bytesLeft];
                fis.read(smallBuf);
                fos.write(smallBuf);
            }
        }
        fos.close();
        fis.close();
    }

    public static InputStream getInputStream(File file, Class c) throws FileNotFoundException {
        InputStream rtn;
        String s;
        if (file != null) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                s = file.toString();
                int i = s.indexOf(File.separator);
                if (i >= 0) {
                    s = s.substring(i);
                    s = s.replace('\\', '/');
                    if ((rtn = c.getResourceAsStream(s)) != null) {
                        return rtn;
                    }
                }
                throw e;
            }
        }
        return null;
    }

    public static File extractFile(String srcFile, String toDir, String destFilename, Class cls) {
        URL url = cls.getResource(srcFile);
        URLConnection connection = null;
        try {
            connection = url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        File dir = new File(toDir);
        if (!dir.exists()) dir.mkdirs();
        File cache = new File(toDir, destFilename);
        return extractFile(connection, cache);
    }

    public static File extractFile(URLConnection connection, File cacheFile) {
        try {
            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            File outFile = File.createTempFile(cacheFile.getName(), ".tmp");
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
            int i;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
            out.flush();
            out.close();
            in.close();
            FileUtil.copyFile(outFile, cacheFile);
            outFile.delete();
            return cacheFile;
        } catch (IOException ex) {
            MerlotDebug.exception(ex);
        }
        return null;
    }
}
