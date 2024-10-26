package org.vramework.commons.io;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import org.vramework.commons.exceptions.VRuntimeException;
import org.vramework.commons.io.exceptions.IoErrors;
import org.vramework.commons.io.exceptions.IoException;
import org.vramework.commons.utils.VSert;

/**
 * Simple handling for small text resources such as text files or URL based resources. <br />
 * 
 * @author tmahring
 */
public class TextResources {

    /**
   * Loads the given text file into a String.
   * 
   * @param inputURL
   * @param charset
   * @return The file contents as String.
   */
    public static String loadResource(URL inputURL, Charset charset) {
        VSert.argNotNull("inputURL", inputURL);
        InputStream is = null;
        BufferedReader br = null;
        StringBuilder stringBuf = new StringBuilder(10000);
        final int READ_SIZE = 1024;
        try {
            URLConnection connection;
            if (inputURL.getProtocol().equalsIgnoreCase("HTTPS")) {
                connection = connect(inputURL);
                connection = inputURL.openConnection();
            } else {
                connection = inputURL.openConnection();
            }
            is = connection.getInputStream();
            br = new BufferedReader(new InputStreamReader(is, charset), 1024 * 10);
            char[] charBuf = new char[1024];
            char[] smallerBuf = null;
            int read = br.read(charBuf);
            while (read >= 0) {
                if (read < READ_SIZE) {
                    smallerBuf = new char[read];
                    System.arraycopy(charBuf, 0, smallerBuf, 0, read);
                    stringBuf.append(smallerBuf);
                } else {
                    stringBuf.append(charBuf);
                }
                read = br.read(charBuf);
            }
            return stringBuf.toString();
        } catch (FileNotFoundException e) {
            throw new IoException(IoErrors.FileNotFound, e.getMessage());
        } catch (Exception e) {
            throw new VRuntimeException(e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    protected static URLConnection connect(URL url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            return conn;
        } catch (Exception e) {
            throw new VRuntimeException(e);
        }
    }

    /**
   * Same as {@link #saveFile(String, File, Charset, boolean)} but converts passed filename to a file object.
   * 
   * @param content
   * @param fileName
   * @param charset
   * @param append
   */
    public static void saveFile(String content, String fileName, Charset charset, boolean append) {
        File file = new File(fileName);
        saveFile(content, file, charset, append);
    }

    /**
   * Saves the given content into a textfile. Note: Directories will be created automatically.
   * 
   * @param content
   * @param file
   * @param charset
   * @param append
   */
    public static void saveFile(String content, File file, Charset charset, boolean append) {
        OutputStream os = null;
        PrintWriter writer = null;
        OutputStreamWriter ow = null;
        try {
            File dir;
            try {
                String path = file.getParent();
                dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                file.createNewFile();
                os = new BufferedOutputStream(new FileOutputStream(file, append), 1024);
                ow = new OutputStreamWriter(os, charset);
                writer = new PrintWriter(ow);
                writer.write(content);
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
                throw new VRuntimeException(e);
            }
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                    writer = null;
                }
                if (ow != null) {
                    ow.close();
                    ow = null;
                }
                if (os != null) {
                    os.close();
                    os = null;
                }
            } catch (Exception ignored) {
                @SuppressWarnings("unused") int i = 0;
            }
        }
    }
}
