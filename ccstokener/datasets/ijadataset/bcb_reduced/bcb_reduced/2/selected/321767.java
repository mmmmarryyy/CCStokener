package ch.photoindex.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Tools to work with I/O.
 * 
 * @author Lukas Blunschi
 * 
 */
public class IOTools {

    /**
	 * Pipe one stream into another. no close operations are applied to the
	 * streams.
	 * 
	 * @param in
	 *            read from this.
	 * @param out
	 *            write to this.
	 * @return number of bytes piped or -1 if an exception occured.
	 */
    public static long pipe(InputStream in, OutputStream out) {
        try {
            long numBytes = 0;
            byte[] buffer = new byte[4096];
            int num = -1;
            while ((num = in.read(buffer)) > 0) {
                numBytes += num;
                out.write(buffer, 0, num);
            }
            return numBytes;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
	 * Download given URL and store result in specified file.
	 * 
	 * @param urlStr
	 * @param destFile
	 * @return null if all went well, the error message otherwise.
	 */
    public static String download(String urlStr, File destFile) {
        String result = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            URL url = new URL(urlStr);
            in = new BufferedInputStream(url.openStream());
            out = new BufferedOutputStream(new FileOutputStream(destFile), 1024);
            byte[] buffer = new byte[1024];
            int num = -1;
            while ((num = in.read(buffer)) > 0) {
                out.write(buffer, 0, num);
            }
        } catch (Exception e) {
            result = e.getMessage();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
