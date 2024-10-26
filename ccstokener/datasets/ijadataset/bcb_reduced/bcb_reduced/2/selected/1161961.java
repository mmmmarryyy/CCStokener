package tjacobs.io;

import java.awt.Component;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import tjacobs.io.DataFetcher.FetcherListener;

/**
 * IOUtils class
 * 
 * Utilities for reducing the complexity of IO operations. Reading text / binary files & streams,
 * piping between streams, creating threadable fetchers.
 * 
 */
public class IOUtils {

    public static final int DEFAULT_BUFFER_SIZE = (int) Math.pow(2, 20);

    public static final int DEFAULT_WAIT_TIME = 30000;

    public static final int NO_WAIT_TIME = -1;

    public static final boolean ALWAYS_BACKUP = false;

    /**
	 * Will auto detect files in the following formats:
	 * UTF-16
	 * UTF-8
	 * ANSI (windows-1252)
	 * otherwise, assumes ASCII
	 * 
	 * NOTE: Existence of OTHER_SYMBOL characters (classified SO in Unicode) in UTF-8
	 * files will trick this method into thinking that its using ANSI encoding. If
	 * this functionality is not desired, use the longer version of this method
	 * 
	 * @param source
	 * @return
	 * @throws IOException
	 */
    public static String readFileAsString(File source) throws IOException {
        return readFileAsString(source, true);
    }

    /**
	 * Will auto detect files in the following formats:
	 * UTF-16
	 * UTF-8
	 * ANSI (windows-1252)
	 * otherwise, assumes ASCII
	 * @param source
	 * @param detectANSIByOtherSymbol if this parameter is true, any UTF-8 file will
	 * be interpreted as an ANSI file. Likewise, it is assumed that characters in this range
	 * are the indicators of an ANSI file.  
	 * @return
	 * @throws IOException
	 */
    public static String readFileAsString(File source, boolean detectANSIByOtherSymbolChar) throws IOException {
        final DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(source)));
        final byte[] buffer = new byte[(int) source.length()];
        dis.readFully(buffer);
        dis.close();
        String encoding = GetEncoding(buffer);
        String str = new String(buffer, encoding);
        if (encoding.toUpperCase().equals("UTF-8") && detectANSIByOtherSymbolChar) {
            for (int i = 0; i < str.length(); i++) {
                if (Character.getType(str.charAt(i)) == Character.OTHER_SYMBOL) {
                    str = new String(buffer, "windows-1252");
                    break;
                }
            }
        }
        return str;
    }

    /** Detect UTF-16 and UTF-8 */
    public static String GetEncoding(byte[] buffer) {
        if (buffer[0] == -1 && (buffer[1] == -2)) return "UTF-16";
        if (buffer[0] == -2 && (buffer[1] == -1)) return "UTF-16";
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] >= 128 || buffer[i] < 0) return "UTF-8";
        }
        return "US-ASCII";
    }

    public static String readFileAsString(File source, String encoding) throws IOException {
        final DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(source)));
        final byte[] buffer = new byte[(int) source.length()];
        dis.readFully(buffer);
        dis.close();
        return new String(buffer, encoding);
    }

    /**
	 * Will auto detect files in the following formats:
	 * UTF-16
	 * UTF-8
	 * otherwise, assumes ASCII
	 * @param f
	 * @return
	 * @throws IOException
	 */
    public static String loadTextFile(File f) throws IOException {
        return readFileAsString(f);
    }

    public static String loadTextFile(PushbackInputStream in, long length) throws IOException {
        boolean UTF = isUTF16(in);
        char data[] = new char[!UTF ? (int) length : ((int) length) / 2 - 1];
        BufferedReader br = new BufferedReader(UTF ? new InputStreamReader(in) : new InputStreamReader(in, "UTF-16"));
        br.read(data);
        return new String(data);
    }

    /**
	 * Old way. Preferred way now is to use loadTextFile(PushBackInputStream)
	 * @deprecated
	 * @param f
	 * @param UTF16
	 * @return
	 * @throws IOException
	 */
    public static String loadTextFile(File f, boolean UTF16) throws IOException {
        BufferedReader br = !UTF16 ? new BufferedReader(new FileReader(f)) : new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-16"));
        int length = (int) (!UTF16 ? f.length() : f.length() / 2);
        char data[] = new char[!UTF16 ? (int) length : ((int) length) / 2 - 1];
        int got = 0;
        do {
            got += br.read(data, got, data.length - got);
        } while (got < data.length);
        return new String(data);
    }

    public static DataFetcher loadData(InputStream in) {
        byte buf[] = new byte[DEFAULT_BUFFER_SIZE];
        return loadData(in, buf);
    }

    public static DataFetcher loadData(InputStream in, byte buf[]) {
        return loadData(in, buf, TimeOut.DEFAULT_WAIT_TIME);
    }

    public static DataFetcher loadData(InputStream in, byte buf[], int waitTime) {
        return new DataFetcher(in, buf, waitTime);
    }

    public static DataFetcher loadData(InputStream in, int initBufLength, int waitTime) {
        return loadData(in, new byte[initBufLength], waitTime);
    }

    public static DataFetcher loadData(File f) throws FileNotFoundException {
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        long len = f.length();
        if (len > Integer.MAX_VALUE) {
            throw new RuntimeException("File size exceeds maximum size for a byte buffer");
        }
        return loadData(in, (int) len, TimeOut.NO_TIMEOUT);
    }

    public static DataFetcher loadData(URL url) throws IOException {
        return loadData(url.openConnection());
    }

    public static DataFetcher loadData(URLConnection conn) throws IOException {
        int size = conn.getContentLength();
        if (size < 0) return loadData(conn.getInputStream(), 2000, DEFAULT_WAIT_TIME);
        return loadData(conn.getInputStream(), size, DEFAULT_WAIT_TIME);
    }

    public static void fetch(FetcherListener listener, InputStream stream) {
        DataFetcher fetcher = loadData(stream);
        runFetcher(fetcher, listener, true);
    }

    public static void fetch(FetcherListener listener, File f) throws FileNotFoundException {
        fetch(listener, new FileInputStream(f));
    }

    public static void fetch(FetcherListener listener, URL stream) throws IOException {
        fetch(listener, stream.openConnection());
    }

    public static void fetch(FetcherListener listener, URLConnection stream) throws IOException {
        fetch(listener, stream.getInputStream());
    }

    public static void fetch(FetcherListener listener, Socket s) throws IOException {
        fetch(listener, s.getInputStream());
    }

    public static void runFetcher(DataFetcher fetcher, FetcherListener fl, boolean asyncronous) {
        fetcher.addFetcherListener(fl);
        if (asyncronous) {
            new Thread(fetcher).start();
        } else {
            fetcher.run();
        }
    }

    /** Creates a DataFetcher from the input stream, runs w/ a new thread
	 * @param in
	 * @param fl
	 * @return
	 */
    public static DataFetcher loadDataASync(InputStream in, FetcherListener fl) {
        DataFetcher fetcher = loadData(in);
        fetcher.addFetcherListener(fl);
        new Thread(fetcher).start();
        return fetcher;
    }

    /**
	 * Note: There is no guarentee that this method will
	 * ever return. For instance, if you call loadAll on
	 * an open socket connection it won't return until the
	 * socket has closed
	 */
    public static String loadAllString(InputStream in) throws UnsupportedEncodingException {
        DataFetcher fetcher = loadData(in);
        fetcher.run();
        byte buffer[] = fetcher.buf;
        buffer = trimBuf(buffer, fetcher.got);
        String encoding = GetEncoding(buffer);
        return new String(buffer, encoding);
    }

    /**
	 * Note: There is no guarantee that this method will
	 * ever return. For instance, if you call loadAll on
	 * an open socket connection it won't return until the
	 * socket has closed
	 */
    public static byte[] loadAll(InputStream in) {
        DataFetcher fetcher = loadData(in);
        try {
            return fetcher.readCompletely();
        } catch (PartialReadException pre) {
            pre.printStackTrace();
            return fetcher.buf;
        }
    }

    public static void copyBufs(byte src[], byte target[]) {
        int length = Math.min(src.length, target.length);
        for (int i = 0; i < length; i++) {
            target[i] = src[i];
        }
    }

    /**
	 * Not threaded by default. If you need this to run
	 * in a separate thread, create a new thread or runnable class
	 * @param in
	 * @param out
	 */
    public static void pipe(InputStream in, OutputStream out) {
        pipe(in, out, TimeOut.NO_TIMEOUT);
    }

    /**
	 * Not threaded by default. If you need this to run
	 * in a separate thread, create a new thread or runnable class
	 * @param in
	 * @param out
	 */
    public static void pipe(InputStream in, final OutputStream out, int timeout) {
        pipe(timeout, false, in, out);
    }

    /**
	 * Not threaded by default. If you need this to run
	 * in a separate thread, create a new thread or runnable class
	 * @param in
	 * @param out
	 */
    public static IOException pipe(int timeout, final boolean closeWhenDone, final InputStream in, final OutputStream... out) {
        return pipe(timeout, closeWhenDone, false, in, out);
    }

    /**
	 * Not threaded by default. If you need this to run
	 * in a separate thread, create a new thread or runnable class
	 * @param in
	 * @param out
	 */
    public static IOException pipe(int timeout, final boolean closeWhenDone, boolean syncOnOutputStream, final InputStream in, final OutputStream... out) {
        DataFetcher info = new DataFetcher(in, new byte[DEFAULT_BUFFER_SIZE], timeout);
        PipeFetcher pf = new PipeFetcher(closeWhenDone, syncOnOutputStream, in, out);
        info.addFetcherListener(pf);
        info.run();
        return pf.ex;
    }

    private static class PipeFetcher implements FetcherListener {

        OutputStream out[];

        InputStream in;

        public IOException ex;

        boolean closeWhenDone;

        boolean syncOnOutputStream;

        public PipeFetcher(boolean closeWhenDone, boolean syncOnOutputStream, InputStream in, OutputStream... out) {
            this.in = in;
            this.out = out;
            this.closeWhenDone = closeWhenDone;
            this.syncOnOutputStream = syncOnOutputStream;
        }

        public void fetchedMore(byte[] buf, int start, int end) {
            for (OutputStream out : this.out) {
                try {
                    if (syncOnOutputStream) {
                        synchronized (out) {
                            out.write(buf, start, end - start);
                            out.flush();
                        }
                    } else {
                        out.write(buf, start, end - start);
                    }
                } catch (IOException iox) {
                    ex = iox;
                    try {
                        in.close();
                        out.close();
                    } catch (IOException iox2) {
                        iox2.printStackTrace();
                    }
                }
            }
        }

        public void fetchedAll(byte[] buf) {
            if (closeWhenDone) {
                for (OutputStream out : this.out) {
                    try {
                        out.close();
                    } catch (IOException iox) {
                        iox.printStackTrace();
                    }
                }
            }
        }
    }

    public static byte[] expandBuf(byte array[]) {
        return expandBuf(array, array.length * 2);
    }

    public static byte[] expandBuf(byte array[], int newlength) {
        byte newbuf[] = new byte[newlength];
        copyBufs(array, newbuf);
        return newbuf;
    }

    public static byte[] trimBuf(byte[] array, int size) {
        byte[] newbuf = new byte[size];
        for (int i = 0; i < size; i++) {
            newbuf[i] = array[i];
        }
        return newbuf;
    }

    /**
	 * @see getFileOutputStream(File, boolean)
	 */
    public static OutputStream getFileOutputStream(File file) throws IOException {
        return getFileOutputStream(file, true);
    }

    /**
	 * Convienience method for opening a FileOutputStream w/wo a buffer
	 * 
	 * makes sure that the file directory exists so this should always succeed.
	 */
    public static OutputStream getFileOutputStream(File file, boolean buffered) throws IOException {
        if (!file.exists() && !file.isDirectory()) {
            confirmDirectoryExists(file.getParentFile());
        }
        if (file.exists()) {
            if (ALWAYS_BACKUP) {
                file.renameTo(new File(file.getAbsolutePath() + "~"));
            } else {
                file.delete();
            }
        }
        file.createNewFile();
        OutputStream out = new FileOutputStream(file);
        if (buffered) {
            out = new BufferedOutputStream(out);
        }
        return out;
    }

    /**
	 * Confirms that a directory exists and makes it if it doesn't
	 */
    public static void confirmDirectoryExists(File dir) {
        if (!dir.exists()) {
            confirmDirectoryExists(dir.getParentFile());
            dir.mkdir();
        }
        if (!dir.isDirectory()) {
            confirmDirectoryExists(dir.getParentFile());
        }
    }

    public static OutputStream getFileOutputStream(String name) throws IOException {
        return getFileOutputStream(name, true);
    }

    public static PrintStream getFilePrintStream(String file) throws IOException {
        return new PrintStream(getFileOutputStream(file));
    }

    public static PrintStream getFilePrintStream(File file) throws IOException {
        return new PrintStream(getFileOutputStream(file));
    }

    public static OutputStream getFileOutputStream(String name, boolean buffered) throws IOException {
        return getFileOutputStream(new File(name), buffered);
    }

    /**
	 * @param f if f is a directory it returns the absolue path to f otherwise it returns the absolute path to the directory f is in
	 */
    public static String getDirectory(File f) {
        if (f.isDirectory()) {
            return f.getAbsolutePath();
        } else {
            return f.getParentFile().getAbsolutePath();
        }
    }

    /**
	 * Get the file without the extension.
	 * @see getFileNoExtension(String);
	 */
    public static String getFilenameNoExtension(File f) {
        return getFilenameNoExtension(f.getName());
    }

    /**
	 * Gets the file name without the extension
	 * returns the whole file name if no '.' is found<br>
	 * otherwise returns whatever's before the last .
	 */
    public static String getFilenameNoExtension(String s) {
        int idx = s.indexOf('.');
        if (idx == -1) {
            return s;
        } else {
            return s.substring(0, idx);
        }
    }

    /**
	 * gets the file extension
	 * if a '.' character is found it returns what's after the last .
	 * if not, it returns the empty string
	 */
    public static String getFileExtension(String s) {
        int idx = s.lastIndexOf('.');
        if (idx == -1) {
            return "";
        } else {
            return s.substring(idx + 1);
        }
    }

    /**
	 * @see getFileExtension(String)
	 */
    public static String getFileExtension(File f) {
        return getFileExtension(f.getName());
    }

    /**
	 * Delete everything in a directory. Recursively deletes all sub-directories
	 */
    public static void deleteDirectory(File f, Component parent) {
        if (!f.isDirectory()) {
            throw new RuntimeException("File " + f.getAbsolutePath() + " is not a directory!");
        }
        int val = JOptionPane.showConfirmDialog(parent, "Confirm Delete " + f.getAbsolutePath(), "Confirm Delete " + f.getAbsolutePath(), JOptionPane.OK_CANCEL_OPTION);
        if (val == JOptionPane.OK_OPTION) {
            deleteAllFiles(f);
        }
    }

    private static void deleteAllFiles(File f) {
        if (!f.isDirectory()) {
            f.delete();
        } else {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].equals(f) || files[i].equals(f.getParent())) {
                    continue;
                }
                deleteAllFiles(files[i]);
            }
            f.delete();
        }
    }

    /**
	 * static utility method for copying a file to another location
	 */
    public static void copyFile(File src, File newParent) throws FileNotFoundException {
        if (!src.exists()) {
            return;
        }
        if (!newParent.exists()) {
            newParent.mkdirs();
        }
        if (newParent.isDirectory()) {
            File newFile = new File(newParent, src.getName());
            if (src.isDirectory()) {
                newFile.mkdir();
                File children[] = src.listFiles();
                for (int i = 0; i < children.length; i++) {
                    copyFile(children[i], newFile);
                }
            } else {
                DataFetcher info = loadData(new FileInputStream(src));
                final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newFile));
                info.addFetcherListener(new FetcherListener() {

                    public void fetchedMore(byte[] buf, int start, int end) {
                        try {
                            out.write(buf, start, end - start);
                        } catch (IOException iox) {
                            iox.printStackTrace();
                        }
                    }

                    public void fetchedAll(byte buf[]) {
                    }
                });
            }
        }
    }

    /**
	 * @deprecated use the Find class
	 */
    public static File find(Pattern p, File start) {
        return recursiveFind(start, null, p, true);
    }

    /**
	 * @deprecated use the Find class
	 */
    public static File find(Pattern p) {
        return find(p, new File("."));
    }

    /**
	 * @deprecated use the Find class
	 */
    private static File recursiveFind(File current, File cameFrom, Pattern p, boolean startDescending) {
        Matcher m = p.matcher(current.getName());
        if (m.matches()) {
            return current;
        }
        if (startDescending) {
            File value = descend(current, cameFrom, p, startDescending);
            if (value != null) return value;
            return ascend(current, cameFrom, p, startDescending);
        } else {
            File value = ascend(current, cameFrom, p, startDescending);
            if (value != null) return value;
            return descend(current, cameFrom, p, startDescending);
        }
    }

    /**
	 * @deprecated use the Find class
	 */
    private static File ascend(File current, File cameFrom, Pattern p, boolean startDescending) {
        File par = current.getParentFile();
        if (par == null) {
            return null;
        }
        par = par.getAbsoluteFile();
        if (par.equals(cameFrom)) {
            return null;
        }
        return recursiveFind(par, current, p, false);
    }

    /**
	 * @deprecated use the Find class
	 */
    private static File descend(File current, File cameFrom, Pattern p, boolean startDescending) {
        File files[] = current.listFiles();
        if (files == null) {
            return null;
        }
        for (int i = 0; i < files.length; i++) {
            File child = files[i];
            if (child.equals(cameFrom)) {
                continue;
            }
            File f = recursiveFind(child, current, p, true);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    /**
	 * Utility for saving a string to a file. Rather than have to set
	 * up all the writers etc and trap exceptions, just returns a boolean
	 * if it worked
	 * <p>
	 * @see saveData(File, byte[])
	 */
    public static boolean saveString(File f, String s) {
        return saveData(f, s.getBytes());
    }

    /**
	 * Utility for saving a string to a file. Rather than have to set
	 * up all the writers etc and trap exceptions, just returns a boolean
	 * if it worked
	 */
    public static boolean saveData(File f, byte[] data) {
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
            out.write(data);
            out.close();
        } catch (IOException iox) {
            iox.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * Way to save a stream to a file. Not multithreaded.
	 * Will block until the save is done
	 * 
	 * @param f
	 * @param in
	 * @return
	 */
    public static boolean saveData(File f, InputStream in) throws FileNotFoundException {
        return pipe(TimeOut.NO_TIMEOUT, true, in, new FileOutputStream(f)) == null;
    }

    /**
	 * From http://www.exampledepot.com/egs/java.io/WriteToUTF8.html
	 * @param f
	 * @param aString
	 * @param encoding
	 */
    public static void saveStringEncoded(File f, String aString, String encoding) {
        try {
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(f), encoding);
            out.write(aString);
            out.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @deprecated use Find
	 */
    public static List<File> findAllFiles(Pattern p) {
        ArrayList<File> l = new ArrayList<File>();
        File start = new File("C:\\");
        return recursiveFindAll(start, null, p, true, l, 0);
    }

    /**
	 * @deprecated use Find
	 */
    private static List<File> recursiveFindAll(File current, File cameFrom, Pattern p, boolean startDescending, ArrayList<File> list, int level) {
        System.out.println("Scanning: " + current);
        Matcher m = p.matcher(current.getName());
        if (current.getName().equals("C:\\")) {
            System.out.println("root");
            try {
                System.in.read();
            } catch (IOException iox) {
            }
        }
        if (m.matches()) {
            list.add(current);
            System.out.println("Adding " + current);
        }
        if (startDescending) {
            descendAll(current, cameFrom, p, startDescending, list, level + 1);
        } else {
            descendAll(current, cameFrom, p, startDescending, list, level + 1);
        }
        return list;
    }

    /**
	 * @deprecated use Find
	 */
    @SuppressWarnings("unused")
    private static List<File> ascendAll(File current, File cameFrom, Pattern p, boolean startDescending, ArrayList<File> list, int level) {
        File par = current.getParentFile();
        if (par == null) {
            return list;
        }
        par = par.getAbsoluteFile();
        if (par.equals(cameFrom)) {
            return list;
        }
        recursiveFindAll(par, current, p, false, list, level);
        return list;
    }

    /**
	 * @deprecated use Find
	 */
    private static File descendAll(File current, File cameFrom, Pattern p, boolean startDescending, ArrayList<File> list, int level) {
        File files[] = current.listFiles();
        if (files == null) {
            return null;
        }
        for (int i = 0; i < files.length; i++) {
            File child = files[i];
            if (child.equals(cameFrom)) {
                continue;
            }
            recursiveFindAll(child, current, p, true, list, level);
        }
        return null;
    }

    public File getUniqueName(File f) {
        return getUniqueName(f, new MessageFormat("~{0,number,integer}"));
    }

    public String getNameWOExtension(File f, boolean useAbsolute) {
        int idx = f.getName().indexOf(".");
        return (idx == -1) ? (useAbsolute ? f.getAbsolutePath() : f.getName()) : (useAbsolute ? f.getAbsolutePath() : f.getName()).substring(0, (useAbsolute ? f.getAbsolutePath().lastIndexOf(".") : f.getName().lastIndexOf(".")));
    }

    public String getFileType(File f, boolean includeDot) {
        int idx = f.getName().lastIndexOf(".");
        return idx == -1 ? "" : f.getName().substring(idx + (includeDot ? 0 : 1));
    }

    public File getUniqueName(File f, MessageFormat format) {
        String base = getNameWOExtension(f, true);
        String extension = getFileType(f, true);
        int count = 0;
        while (f.exists()) {
            count++;
            f = new File(base + format.format(new Object[] { Integer.valueOf(count) }) + extension);
        }
        return f;
    }

    public static boolean isUTF16(File f) throws IOException {
        FileInputStream in = null;
        try {
            if (!f.exists() || f.length() < 2) return false;
            in = new FileInputStream(f);
            byte b = (byte) in.read();
            if (!(b == -1)) return false;
            b = (byte) in.read();
            return b == -2;
        } finally {
            if (in != null) in.close();
        }
    }

    public static boolean isUTF16(PushbackInputStream in) {
        boolean got1 = false, got2 = false;
        byte b = 0, b2 = 0;
        try {
            b = (byte) in.read();
            got1 = true;
            if (!(b == -1 || b == -2)) {
                return false;
            }
            b2 = (byte) in.read();
            got2 = true;
            return b == -1 ? b2 == -2 : b2 == -1;
        } catch (IOException iox) {
            iox.printStackTrace();
        } finally {
            try {
                if (got1) {
                    in.unread(b);
                }
                if (got2) {
                    in.unread(b2);
                }
            } catch (IOException iox) {
                iox.printStackTrace();
            }
        }
        return false;
    }

    public void saveJTableToCSV(JTable table, File f) throws IOException {
        if (table == null || f == null) return;
        TableModel model = table.getModel();
        if (model == null) return;
        int rows = model.getRowCount();
        int columns = model.getColumnCount();
        PrintWriter pw = new PrintWriter(new FileWriter(f));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                Object o = model.getValueAt(i, j);
                pw.print(o.toString());
                if (j != columns - 1) {
                    pw.print(",");
                }
            }
            pw.println();
        }
    }

    public static String getPathRelativeTo(File path, File relativeTo) {
        return getPathRelativeTo(path.getAbsolutePath(), relativeTo.getAbsolutePath());
    }

    public static String getPathRelativeTo(String path, String relativeTo) {
        if (!path.startsWith(relativeTo)) return null;
        path = path.substring(relativeTo.length());
        if (path.startsWith(File.separator)) path = path.substring(File.separator.length());
        return path;
    }

    public static PrintStream sStdOut;

    public static PrintStream sStdErr;

    public static File LogToFile() {
        DateFormat df = new SimpleDateFormat("MMddyy-hhmmss");
        Main m = Main.getSingleton();
        String name = null;
        if (m != null) {
            if (m instanceof App) {
                name = ((App) m).getProjectName();
            } else name = m.getClass().getSimpleName();
        }
        String lognm = (name != null ? name : "");
        File dir = new File(System.getProperty("user.home"));
        File[] files = dir.listFiles();
        File[] logFiles = new File[4];
        int count = 0;
        for (File fi : files) {
            if (fi.getName().startsWith(lognm) && fi.getName().endsWith(".log")) {
                if (count < 4) {
                    logFiles[count++] = fi;
                } else {
                    long mod = fi.lastModified();
                    for (int i = 0; i < logFiles.length; i++) {
                        if (logFiles[i].lastModified() < mod) {
                            mod = logFiles[i].lastModified();
                            File tmp = logFiles[i];
                            logFiles[i] = fi;
                            fi = tmp;
                        }
                    }
                    fi.delete();
                }
            }
        }
        File f = new File(dir, lognm + df.format(new Date()) + ".log");
        return LogToFile(f);
    }

    public static File LogToFile(File f) {
        sStdOut = System.out;
        sStdErr = System.err;
        try {
            final FileOutputStream fos = new FileOutputStream(f);
            OutputStream pos_out = new OutputStream() {

                public void write(int b) {
                    try {
                        sStdOut.write(b);
                        fos.write(b);
                    } catch (IOException iox) {
                        iox.printStackTrace();
                    }
                }

                public void write(byte[] b) {
                    try {
                        sStdOut.write(b);
                        fos.write(b);
                    } catch (IOException iox) {
                        iox.printStackTrace();
                    }
                }

                public void write(byte[] b, int off, int len) {
                    try {
                        sStdOut.write(b, off, len);
                        fos.write(b, off, len);
                    } catch (IOException iox) {
                        iox.printStackTrace();
                    }
                }
            };
            OutputStream pos_err = new OutputStream() {

                public void write(int b) {
                    try {
                        sStdErr.write(b);
                        fos.write(b);
                    } catch (IOException iox) {
                        iox.printStackTrace();
                    }
                }

                public void write(byte[] b) {
                    try {
                        sStdErr.write(b);
                        fos.write(b);
                    } catch (IOException iox) {
                        iox.printStackTrace();
                    }
                }

                public void write(byte[] b, int off, int len) {
                    try {
                        sStdErr.write(b, off, len);
                        fos.write(b, off, len);
                    } catch (IOException iox) {
                        iox.printStackTrace();
                    }
                }
            };
            PrintStream ps_out = new PrintStream(pos_out);
            PrintStream ps_err = new PrintStream(pos_err);
            System.setErr(ps_err);
            System.setOut(ps_out);
        } catch (IOException iox) {
            iox.printStackTrace();
        }
        return f;
    }

    /**
	 * @deprecated doesn't work
	 * @param shortcut
	 * @param was
	 * @param to
	 * @return
	 */
    public static byte[] redirectWindowsShortcut(byte[] shortcut, String was, String to) {
        byte[] strdata1 = was.getBytes();
        byte[] strdata2 = was.toUpperCase().getBytes();
        byte[] to_bytes = to.getBytes();
        byte[] output = new byte[shortcut.length + (to_bytes.length - strdata1.length)];
        int idx = 0;
        for (int i = 0; i < shortcut.length; i++) {
            if (shortcut[i] == strdata1[idx] || shortcut[i] == strdata2[idx]) {
                idx++;
            } else {
                for (int j = i - idx; j <= i; j++) {
                    output[j] = shortcut[j];
                }
                idx = 0;
            }
            if (idx == strdata1.length) {
                int start = i - idx;
                System.out.println("found at: " + start);
                int len = to_bytes.length;
                for (int j = 0; j < len; j++) {
                    output[start + j] = to_bytes[j];
                }
                i++;
                for (int j = 0; j < strdata1.length - i; j++) {
                    output[start + len + j] = shortcut[i + j];
                }
                return output;
            }
        }
        return null;
    }
}
