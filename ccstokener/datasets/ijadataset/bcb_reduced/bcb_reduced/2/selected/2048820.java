package org.chessworks.common.javatools.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.chessworks.common.javatools.LogHelper;
import org.chessworks.common.javatools.io.codec.StreamDecoder;
import org.chessworks.common.javatools.io.codec.StreamEncoder;
import org.chessworks.common.javatools.io.codec.TextCodec;

public class FileHelper extends IOHelper {

    public static final String EOL = IOHelper.EOL;

    private static Logger logger = LogHelper.getLogger();

    private File file;

    private Charset encoding = IOHelper.UTF8;

    public FileHelper() {
    }

    public FileHelper(File file) {
        this.file = file;
        this.encoding = IOHelper.UTF8;
    }

    public FileHelper(File file, String encoding) {
        this.file = file;
        this.encoding = Charset.forName(encoding);
    }

    public FileHelper(File file, Charset encoding) {
        this.file = file;
        this.encoding = encoding;
    }

    public FileHelper(String fileName) {
        this.file = new File(fileName);
        this.encoding = IOHelper.UTF8;
    }

    public FileHelper(String fileName, String encoding) {
        this.file = new File(fileName);
        this.encoding = Charset.forName(encoding);
    }

    public FileHelper(String fileName, Charset encoding) {
        this.file = new File(fileName);
        this.encoding = encoding;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setFile(String fileName) {
        this.file = new File(fileName);
    }

    public void setCharset(Charset encoding) {
        this.encoding = encoding;
    }

    public void setCharSet(String encoding) {
        this.encoding = Charset.forName(encoding);
    }

    public void appendData() throws IOException {
        if (!isDirty()) return;
        FileOutputStream stream = null;
        BufferedOutputStream buf = null;
        DataOutputStream out = null;
        try {
            stream = new FileOutputStream(file, true);
            buf = new BufferedOutputStream(stream);
            out = new DataOutputStream(buf);
            doWrite(out);
        } catch (IOException e) {
            onException(e);
        } catch (RuntimeException e) {
            onException(e);
        } catch (Exception e) {
            onException(e);
        } finally {
            IOHelper.closeQuietly(stream);
            IOHelper.closeQuietly(out);
        }
        successfulWrite();
    }

    public void appendText() throws IOException {
        if (!isDirty()) return;
        FileOutputStream stream = null;
        OutputStreamWriter encoder = null;
        PrintWriter out = null;
        try {
            stream = new FileOutputStream(file, true);
            encoder = new OutputStreamWriter(stream, encoding);
            out = new PrintWriter(encoder);
            doWrite(out);
        } catch (IOException e) {
            onException(e);
        } catch (RuntimeException e) {
            onException(e);
        } catch (Exception e) {
            onException(e);
        } finally {
            IOHelper.closeQuietly(out);
            IOHelper.closeQuietly(encoder);
            IOHelper.closeQuietly(stream);
        }
        successfulWrite();
    }

    public void readData() throws IOException {
        FileInputStream stream = null;
        BufferedInputStream buf = null;
        DataInputStream in = null;
        try {
            stream = new FileInputStream(file);
            buf = new BufferedInputStream(stream);
            in = new DataInputStream(buf);
            doRead(in);
        } catch (IOException e) {
            onException(e);
        } catch (RuntimeException e) {
            onException(e);
        } catch (Exception e) {
            onException(e);
        } finally {
            IOHelper.closeQuietly(stream);
            IOHelper.closeQuietly(buf);
            IOHelper.closeQuietly(in);
        }
        successfulRead();
    }

    public void readText() throws IOException {
        FileInputStream stream = null;
        InputStreamReader decoder = null;
        BufferedReader in = null;
        try {
            stream = new FileInputStream(file);
            decoder = new InputStreamReader(stream, encoding);
            in = new BufferedReader(decoder);
            doRead(in);
        } catch (IOException e) {
            onException(e);
        } catch (RuntimeException e) {
            onException(e);
        } catch (Exception e) {
            onException(e);
        } finally {
            IOHelper.closeQuietly(in);
            IOHelper.closeQuietly(decoder);
            IOHelper.closeQuietly(stream);
        }
        successfulRead();
    }

    public void writeData() throws IOException {
        if (!isDirty()) return;
        checkWritableFile(file);
        FileOutputStream stream = null;
        BufferedOutputStream buf = null;
        DataOutputStream out = null;
        try {
            stream = new FileOutputStream(file);
            buf = new BufferedOutputStream(stream);
            out = new DataOutputStream(buf);
            doWrite(out);
            out.close();
        } catch (IOException e) {
            onException(e);
        } catch (RuntimeException e) {
            onException(e);
        } catch (Exception e) {
            onException(e);
        } finally {
            IOHelper.closeQuietly(out);
            IOHelper.closeQuietly(buf);
            IOHelper.closeQuietly(stream);
        }
        successfulWrite();
    }

    public void writeText() throws IOException {
        if (!isDirty()) return;
        checkWritableFile(file);
        FileOutputStream stream = null;
        OutputStreamWriter encoder = null;
        PrintWriter out = null;
        try {
            stream = new FileOutputStream(file);
            encoder = new OutputStreamWriter(stream, encoding);
            out = new PrintWriter(encoder);
            doWrite(out);
            out.close();
        } catch (IOException e) {
            onException(e);
        } catch (RuntimeException e) {
            onException(e);
        } catch (Exception e) {
            onException(e);
        } finally {
            IOHelper.closeQuietly(out);
            IOHelper.closeQuietly(stream);
        }
        successfulWrite();
    }

    protected boolean isDirty() {
        return true;
    }

    protected void successfulRead() {
    }

    protected void successfulWrite() {
    }

    protected void onException(Throwable e) throws IOException, InvalidDataException {
        if (e instanceof Error) throw (Error) e;
        if (e instanceof RuntimeException) throw (RuntimeException) e;
        if (e instanceof IOException) throw (IOException) e; else {
            String msg = "File: \"" + file + "\"";
            throw new InvalidDataException(msg, e);
        }
    }

    public String toString() {
        if (file != null) return file.toString(); else return super.toString();
    }

    public static <T> T readFile(String fileName, StreamDecoder<T> codec) throws FileNotFoundException, IOException {
        File file = new File(fileName);
        return readFile(file, codec);
    }

    public static <T> T readFile(File file, StreamDecoder<T> codec) throws FileNotFoundException, IOException {
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            T data = codec.decode(stream);
            return data;
        } finally {
            closeQuietly(stream);
        }
    }

    public static <T> T readFileQuietly(String fileName, StreamDecoder<T> codec) {
        File file = new File(fileName);
        return readFileQuietly(file, codec);
    }

    public static <T> T readFileQuietly(File file, StreamDecoder<T> codec) {
        Exception ex;
        try {
            T content = readFile(file, codec);
            return content;
        } catch (IOException e) {
            ex = e;
        }
        String msg1 = "Unable to read file: " + file;
        String msg2 = "Exception when reading file.";
        logger.log(Level.INFO, msg1);
        logger.log(Level.FINE, msg2, ex);
        return null;
    }

    public static <T> T readFileMandatory(String fileName, StreamDecoder<T> codec) {
        File file = new File(fileName);
        return readFileQuietly(file, codec);
    }

    public static <T> T readFileMandatory(File file, StreamDecoder<T> codec) {
        Exception ex;
        try {
            T content = readFile(file, codec);
            if (content == null) {
                throw new InvalidDataException("No data returned.");
            }
            return content;
        } catch (IOException e) {
            ex = e;
        }
        String msg = "Unable to read file: " + file.getName();
        logger.log(Level.SEVERE, msg, ex);
        AssertionError ae = new AssertionError(msg);
        ae.initCause(ex);
        throw ae;
    }

    public static <T> void writeFile(File file, StreamEncoder<T> codec, T data) throws FileNotFoundException, IOException {
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            codec.encode(stream, data);
        } finally {
            closeQuietly(stream);
        }
    }

    public static <T> void writeFile(String fileName, StreamEncoder<T> codec, T data) throws FileNotFoundException, IOException {
        File file = new File(fileName);
        writeFile(file, codec, data);
    }

    public static <T> void writeFile(File file, StreamEncoder<T> codec, T data, boolean append) throws FileNotFoundException, IOException {
        if (append) {
            appendToFile(file, codec, data);
        } else {
            writeFile(file, codec, data);
        }
    }

    public static <T> void writeFile(String fileName, StreamEncoder<T> codec, T data, boolean append) throws FileNotFoundException, IOException {
        File file = new File(fileName);
        if (append) {
            appendToFile(file, codec, data);
        } else {
            writeFile(file, codec, data);
        }
    }

    public static <T> void appendToFile(File file, StreamEncoder<T> codec, T data) throws FileNotFoundException, IOException {
        OutputStream stream = null;
        try {
            final boolean append = true;
            stream = new FileOutputStream(file, append);
            codec.encode(stream, data);
        } finally {
            closeQuietly(stream);
        }
    }

    public static <T> void appendToFile(String fileName, StreamEncoder<T> codec, T data) throws FileNotFoundException, IOException {
        File file = new File(fileName);
        appendToFile(file, codec, data);
    }

    /**
	 * Utility method to ensure the filename doesn't have any dangerous characters. This includes characters which may not be permitted by the file
	 * system and characters which could be used to access other directories.
	 *
	 * This method is rather conservative. File names may only have A-Z, a-z, 0-9, -, and _. A '.' is also allowed, but '..' is not.
	 *
	 * @return true if the name is shown to be non-dangerous.
	 */
    public static boolean isSafeFileName(String name) {
        if (name == null) return false;
        if (name.length() == 0) return false;
        boolean safeChars = name.matches("[a-zA-Z0-9-_.]*");
        boolean doubleDot = (name.indexOf("..") >= 0);
        boolean hidden = name.startsWith(".");
        boolean root = name.startsWith("/");
        boolean legal = safeChars & !doubleDot & !hidden & !root;
        return legal;
    }

    public static void checkWritableFile(File file) throws FileNotFoundException {
        if (!file.exists()) {
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                throw new FileNotFoundException("No parent directory for file \"" + file + "\"");
            } else if (!parentDir.canWrite()) {
                throw new FileNotFoundException("Directory is not writable for \"" + file + "\"");
            }
        } else if (file.isDirectory()) {
            throw new FileNotFoundException("Not a file: \"" + file + "\"");
        } else if (!file.canWrite()) {
            throw new FileNotFoundException("Read only: \"" + file + "\"");
        }
    }

    public static void checkWritableDirectory(File dir) throws FileNotFoundException {
        if (!dir.exists()) {
            throw new FileNotFoundException("Does not exist: \"" + dir + "\"");
        } else if (dir.isFile()) {
            throw new FileNotFoundException("Not a directory: \"" + dir + "\"");
        } else if (!dir.canWrite()) {
            throw new FileNotFoundException("Read only: \"" + dir + "\"");
        }
    }

    /**
	 * Opens a text file located somewhere in the classpath (ie inside the JAR file).
	 *
	 * @param path
	 *            The path to the file, relative to the classpath or JAR.
	 * @return A BufferedReader for reading the text file.
	 */
    public static BufferedReader openInternalTextFile(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream is = cl.getResourceAsStream(path);
        InputStreamReader r = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(r);
        return br;
    }

    /**
	 * Opens a binary file located somewhere in the classpath (ie inside the JAR file).
	 *
	 * @param path
	 *            The path to the file, relative to the classpath or JAR.
	 * @return A BufferedInputStream for reading the binary file.
	 */
    public static BufferedInputStream openInternalBinaryFile(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream is = cl.getResourceAsStream(path);
        BufferedInputStream in = new BufferedInputStream(is);
        return in;
    }

    /**
	 * Loads a properties file located somewhere in the classpath (ie inside the JAR file).
	 *
	 * @param path
	 *            The path to the file, relative to the classpath or JAR.
	 * @param defaults
	 *            A properties file containing default settings to be used in the event the provided file does not deliver.
	 * @return The properties file. If the file is not found, a new properties object initialized with the defaults is returned. If no defaults are
	 *         provided, null is returned.
	 */
    public static Properties loadInternalPropertiesFile(String path, Properties defaults) {
        Properties props = new Properties(defaults);
        InputStream in = null;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            in = cl.getResourceAsStream(path);
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            if (defaults == null) return null;
        } finally {
            closeQuietly(in);
        }
        return props;
    }

    /**
	 * Opens a text file located on the filesystem relative to the current directory. This does NOT search inside the JAR file.
	 *
	 * @param path
	 *            The path to the file, relative to the current directory when the program is run. The path may be absolute or relative.
	 * @return A BufferedReader for reading the text file.
	 */
    public static BufferedReader openExternalTextFile(String path) throws java.io.IOException {
        InputStreamReader r = new FileReader(path);
        BufferedReader br = new BufferedReader(r);
        return br;
    }

    /**
	 * Opens a binary file located on the filesystem relative to the current directory. This does NOT search inside the JAR file.
	 *
	 * @param path
	 *            The path to the file, relative to the current directory when the program is run. The path may be absolute or relative.
	 * @return A BufferedInputStream for reading the binary file.
	 */
    public static BufferedInputStream openExternalBinaryFile(String path) throws java.io.IOException {
        InputStream is = new FileInputStream(path);
        BufferedInputStream in = new BufferedInputStream(is);
        return in;
    }

    /**
	 * Loads a properties file located on the filesystem. This does NOT search inside the JAR file.
	 *
	 * @param path
	 *            The path to the file, relative to the current directory when the program is run. The path may be absolute or relative.
	 * @param defaults
	 *            A properties file containing default settings to be used in the event the provided file does not deliver.
	 * @return The properties file. If the file is not found, a new properties object initialized with the defaults is returned. If no defaults are
	 *         provided, null is returned.
	 */
    public static Properties loadExternalPropertiesFile(String path, Properties defaults) {
        Properties props = new Properties(defaults);
        InputStream in = null;
        try {
            in = new FileInputStream(path);
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            if (defaults == null) return null;
        } finally {
            closeQuietly(in);
        }
        return props;
    }

    public static void closeQuietly(Closeable stream) {
        Logger logger = LogHelper.getCallerLogger();
        IOHelper.closeQuietly(stream, logger);
    }

    public static boolean addToFile(String txt, String fileName) {
        return writeFile(txt, fileName, true);
    }

    public static int countLines(String fileName) {
        int x = 0;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(fileName));
            while (true) {
                String s = in.readLine();
                if (s == null) break;
                x++;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to read file \"{0}\".  Returning 0.", fileName);
            return 0;
        } finally {
            closeQuietly(in);
        }
        return x;
    }

    public static String listFile(String fileName, String cr) {
        StringBuffer s = new StringBuffer("");
        List<String> result = readFile(fileName);
        if (result != null) {
            for (String line : result) {
                s.append(line);
                s.append(cr);
            }
        }
        return s.toString();
    }

    public static String listFile(URL url, String cr) {
        StringBuffer s = new StringBuffer("");
        List<String> result = readFile(url);
        if (result != null) {
            for (String line : result) {
                s.append(line);
                s.append(cr);
            }
        }
        return s.toString();
    }

    public static boolean makeFile(String txt, String fileName) {
        return writeFile(txt, fileName, false);
    }

    public static int searchFile(String txt, String fileName) {
        if (txt == null) return -1;
        txt = txt.trim();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(fileName));
            int lineNum = 0;
            while (true) {
                String s = in.readLine();
                if (s == null) break;
                s = s.trim();
                if (s.equalsIgnoreCase(txt)) {
                    return lineNum;
                }
                lineNum++;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to read file \"{0}\".  Returning 0.", fileName);
        } finally {
            closeQuietly(in);
        }
        return -1;
    }

    public static List<String> readFile(String fileName) {
        List<String> result = new ArrayList<String>();
        String s = null;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(fileName));
            do {
                s = in.readLine();
                if (s != null) result.add(s);
            } while (s != null);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to read file \"{0}\".  Returning null.", fileName);
            return null;
        } finally {
            closeQuietly(in);
        }
        return result;
    }

    public static List<String> readFile(URL url) {
        List<String> result = new ArrayList<String>();
        String s = null;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            do {
                s = in.readLine();
                if (s != null) result.add(s);
            } while (s != null);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to read \"{0}\".  Returning null.", url);
            return null;
        } finally {
            closeQuietly(in);
        }
        return result;
    }

    public static boolean writeFile(String txt, String fileName, boolean append) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(fileName);
            BufferedOutputStream buf = new BufferedOutputStream(out);
            TextCodec.UTF8.encode(buf, txt);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to write file: " + fileName, e);
            return false;
        } finally {
            closeQuietly(out);
        }
        return true;
    }
}
