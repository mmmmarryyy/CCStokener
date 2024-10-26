package oxygen.util;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A collection of utility functions
 * @author ugorji
 */
public class OxygenUtils {

    private static final SimpleDateFormat uniqueRunIdFmt = new SimpleDateFormat("_H_mm_ss_S");

    private static final Random uniqueRunIdRand = new Random();

    private static String hostid = null;

    private static Log log0 = null;

    private static boolean windows = false;

    private static ThreadGroup topLevelTG = null;

    private static boolean CDEBUG_IS_SYSTEM_OUT = false;

    static {
        windows = (System.getProperty("os.name").indexOf("Windows") != -1);
    }

    public static boolean isWindows() {
        return windows;
    }

    public static InputStream getInputStream(URL url) throws Exception {
        URLConnection con = url.openConnection();
        con.setAllowUserInteraction(false);
        con.connect();
        InputStream is = con.getInputStream();
        return is;
    }

    public static String getURLContents(URL url) throws Exception {
        URLConnection con = url.openConnection();
        HttpURLConnection hcon = null;
        if (con != null && con instanceof HttpURLConnection) {
            hcon = (HttpURLConnection) con;
        }
        con.setAllowUserInteraction(false);
        con.connect();
        InputStream is = con.getInputStream();
        StringBuffer contents = new StringBuffer();
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(is);
            int len = -1;
            char[] ca = new char[1024];
            while ((len = isr.read(ca, 0, 1024)) != -1) {
                contents.append(ca, 0, len);
            }
            return contents.toString();
        } finally {
            CloseUtils.close(isr);
            if (hcon != null) hcon.disconnect();
        }
    }

    public static String getFileContents(File f) throws Exception {
        return getTextContents(new FileReader(f), true);
    }

    public static String getTextContents(Reader fr, boolean closeWhenDone) throws Exception {
        StringWriter bos = new StringWriter();
        char[] buffer = new char[1024];
        int readCount = 0;
        while ((readCount = fr.read(buffer)) > 0) {
            bos.write(buffer, 0, readCount);
        }
        if (closeWhenDone) {
            fr.close();
        }
        return bos.toString();
    }

    public static void writeFileContents(File f, String s) throws Exception {
        writeTextContents(new FileWriter(f), s, true);
    }

    public static void writeTextContents(Writer fw, String s, boolean closeWhenDone) throws Exception {
        fw.write(s, 0, s.length());
        if (closeWhenDone) {
            fw.close();
        }
    }

    public static void copyFile(File from, File to) throws Exception {
        if (from.equals(to)) {
            return;
        }
        FileInputStream fis = new FileInputStream(from);
        FileOutputStream fos = new FileOutputStream(to);
        copyStreams(fis, fos, true);
    }

    public static void copyStreams(InputStream fis, OutputStream fos, boolean closeWhenDone) throws Exception {
        byte[] b = new byte[1024];
        int num = 0;
        while ((num = fis.read(b)) != -1) {
            fos.write(b, 0, num);
        }
        fos.flush();
        if (closeWhenDone) {
            fis.close();
            fos.close();
        }
    }

    public static void copyStreams(Reader fis, Writer fos, boolean closeWhenDone) throws Exception {
        char[] b = new char[1024];
        int num = 0;
        while ((num = fis.read(b)) != -1) {
            fos.write(b, 0, num);
        }
        fos.flush();
        if (closeWhenDone) {
            CloseUtils.close(fis);
            CloseUtils.close(fos);
        }
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Throwable exc) {
        }
    }

    private static String getHostId() throws Exception {
        if (hostid == null) {
            hostid = InetAddress.getLocalHost().getHostName();
            int indexOfDot = hostid.indexOf(".");
            if (indexOfDot != -1) hostid = hostid.substring(0, indexOfDot);
            int maxHostIdLength = 8;
            if (hostid.length() > maxHostIdLength) hostid = hostid.substring(0, maxHostIdLength);
        }
        return hostid;
    }

    /** 
   * Gets a unique id, constructed from a substring of the hostname,
   * the current timestamp and a random number.
   */
    public static synchronized String getUniqueID() throws Exception {
        getHostId();
        String time = uniqueRunIdFmt.format(new Date());
        StringBuffer importnum = new StringBuffer();
        importnum.append(hostid);
        importnum.append(time);
        importnum.append("_R");
        importnum.append(uniqueRunIdRand.nextInt(99999));
        return importnum.toString();
    }

    public static synchronized int getUniqueIntID(int max) throws Exception {
        return uniqueRunIdRand.nextInt(max);
    }

    /**
   * Get the relative path of dir2, compared to dir1
   */
    public static String getRelativePath(File dir1, File dir2) {
        String dir11 = dir1.getPath();
        String dir22 = dir2.getPath();
        int indx = dir11.length();
        String relPath = dir22.substring(indx);
        String fileSep = System.getProperty("file.separator");
        if (!("/").equals(fileSep)) {
            relPath = StringUtils.replaceInString(relPath, fileSep, "/");
        }
        if (relPath.startsWith("/")) {
            relPath = relPath.substring(1);
        }
        if (relPath.endsWith("/")) {
            relPath = relPath.substring(0, relPath.length() - 1);
        }
        return relPath;
    }

    public static void listFiles(File dir, int maxDepth, Collection bucket) {
        listFiles(bucket, dir, null, 0, maxDepth);
    }

    public static void listFiles(File dir, int maxDepth, Collection bucket, FileFilter ff) {
        listFiles(bucket, dir, ff, 0, maxDepth);
    }

    private static void listFiles(Collection list, File vf, FileFilter vff, int currDepth, int maxDepth) {
        currDepth++;
        if (currDepth > maxDepth) {
            return;
        }
        if (vf.exists() && vf.isDirectory()) {
            File[] files = vf.listFiles();
            for (int i = 0; i < files.length; i++) {
                File vpf = files[i];
                if (vff == null || vff.accept(vpf)) {
                    list.add(vpf);
                }
                if (files[i].isDirectory()) {
                    listFiles(list, vpf, vff, currDepth, maxDepth);
                }
            }
        }
    }

    public static void deleteFile(File f) {
        deleteFile(f, true);
    }

    public static void deleteFile(File f, boolean deleteThisFileAlso) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteFile(files[i], true);
            }
        }
        if (deleteThisFileAlso && f.exists() && !f.delete()) {
            throw new RuntimeException("Unable to delete file: " + f);
        }
    }

    public static void mkdirs(File f) {
        if (f.exists()) {
            if (f.isFile()) {
                throw new RuntimeException("A regular file already exists for this pathname: " + f);
            }
        } else if (!f.mkdirs()) {
            throw new RuntimeException("Unable to create directory: " + f);
        }
    }

    public static void extract(ZipInputStream zis, File dir, List filesToExtract) throws Exception {
        ZipEntry ze = null;
        OxygenUtils.mkdirs(dir);
        byte[] b = new byte[1024];
        int numread = -1;
        while ((ze = zis.getNextEntry()) != null) {
            String zename = ze.getName();
            if (filesToExtract != null && !filesToExtract.contains(zename)) {
                continue;
            }
            File f = new File(dir, zename);
            if (ze.isDirectory()) {
                OxygenUtils.mkdirs(f);
            } else {
                OxygenUtils.mkdirs(f.getParentFile());
                FileOutputStream fos = new FileOutputStream(f);
                while ((numread = zis.read(b, 0, b.length)) != -1) {
                    fos.write(b, 0, numread);
                }
                CloseUtils.close(fos);
            }
        }
    }

    public static Hashtable propsToTable(Properties p, Hashtable ht) throws Exception {
        if (ht == null) {
            ht = new Hashtable();
        }
        for (Enumeration enum0 = p.propertyNames(); enum0.hasMoreElements(); ) {
            String key = (String) enum0.nextElement();
            String value = p.getProperty(key);
            if (value != null) {
                ht.put(key, value);
            }
        }
        return ht;
    }

    public static List toList(Enumeration enum0) {
        List l = new ArrayList();
        while (enum0.hasMoreElements()) {
            l.add(enum0.nextElement());
        }
        return l;
    }

    /**
   * This will create an int flag with only that position set
   */
    public static int makeFlag(int position) {
        return (1 << position);
    }

    public static boolean isFlagSet(int flags, int _flag) {
        return ((flags & _flag) == _flag);
    }

    public static int setFlag(int flags, int _flag) {
        flags = flags | _flag;
        return flags;
    }

    public static int clearFlag(int flags, int _flag) {
        flags = flags & ~_flag;
        return flags;
    }

    public static int getFlagPosition(int _flag) {
        for (int i = 0; i < 32; i++) {
            if (_flag == makeFlag(i)) {
                return i;
            }
        }
        return -1;
    }

    public static void extractProps(Properties p, Properties p2, String pfx, boolean removePrefix) {
        int pfxlen = pfx.length();
        for (Enumeration enum0 = p.propertyNames(); enum0.hasMoreElements(); ) {
            String key = (String) enum0.nextElement();
            if (key.startsWith(pfx)) {
                String v = p.getProperty(key);
                if (removePrefix) {
                    key = key.substring(pfxlen);
                }
                p2.setProperty(key, StringUtils.replacePropertyReferencesInString(v, p));
            }
        }
    }

    public static void extractProps(Properties p, Properties p2, Pattern keyPatternMatch) {
        for (Enumeration enum0 = p.propertyNames(); enum0.hasMoreElements(); ) {
            String key = (String) enum0.nextElement();
            if (keyPatternMatch.matcher(key).matches()) {
                String v = p.getProperty(key);
                p2.setProperty(key, StringUtils.replacePropertyReferencesInString(v, p));
            }
        }
    }

    public static Locale stringToLocale(String s) {
        if (s == null || s.trim().length() == 0) {
            return null;
        }
        s = s.replace('-', '_');
        String[] sa = StringUtils.split(s, "_");
        Locale locale = null;
        if (sa.length == 1 && sa[0].trim().length() > 0) {
            locale = new Locale(sa[0]);
        } else if (sa.length == 2) {
            locale = new Locale(sa[0], sa[1].toUpperCase());
        } else if (sa.length == 3) {
            locale = new Locale(sa[0], sa[1].toUpperCase(), sa[2]);
        }
        return locale;
    }

    /**
   * if prefix is null, do not check for prefix
   * if prefix is not null, and removePrefix = true, after matching, remove the prefix
   * if findAllResources, use getResources (and not getResource)
   */
    public static Properties extractPropertiesFromResources(String resourceLocations, boolean findAllResources, String prefix, boolean removePrefix) throws Exception {
        Properties p = new Properties();
        StringTokenizer stz = new StringTokenizer(resourceLocations, " ,");
        List urls = new ArrayList();
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        while (stz.hasMoreTokens()) {
            String tok = stz.nextToken();
            if (findAllResources) {
                for (Enumeration enum0 = cloader.getResources(tok); enum0.hasMoreElements(); ) {
                    urls.add((URL) enum0.nextElement());
                }
            } else {
                urls.add(cloader.getResource(tok));
            }
        }
        for (Iterator itr = urls.iterator(); itr.hasNext(); ) {
            URL url = (URL) itr.next();
            InputStream is = getInputStream(url);
            try {
                p.load(is);
            } finally {
                CloseUtils.close(is);
            }
        }
        Properties p2 = p;
        if (prefix != null) {
            p2 = new Properties();
            extractProps(p, p2, prefix, removePrefix);
        }
        return p2;
    }

    public static Properties changeKeysToLowerCase(Properties p1) {
        Properties p2 = new Properties();
        for (Enumeration enum0 = p1.propertyNames(); enum0.hasMoreElements(); ) {
            String s = (String) enum0.nextElement();
            String v = p1.getProperty(s);
            p2.setProperty(s.toLowerCase(), v);
        }
        return p2;
    }

    public static InputStream getDisconnectedInputStream(InputStream is) throws Exception {
        final File f = File.createTempFile("oxy", null);
        copyStreams(is, new FileOutputStream(f), true);
        FilterInputStream fis = new FilterInputStream(new FileInputStream(f)) {

            public void close() throws IOException {
                super.close();
                f.delete();
            }
        };
        return fis;
    }

    public static InputStream searchForResourceAsStream(String s, Class c) throws Exception {
        InputStream is = null;
        File f = new File(s);
        if (f.exists()) {
            is = new FileInputStream(f);
        } else {
            ClassLoader cl = c.getClassLoader();
            is = cl.getResourceAsStream(s);
            if (is == null) {
                cl = Thread.currentThread().getContextClassLoader();
                is = cl.getResourceAsStream(s);
            }
        }
        return is;
    }

    public static Class getClass(String s) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = OxygenUtils.class.getClassLoader();
        }
        Class c = Class.forName(s, true, cl);
        return c;
    }

    public static ThreadGroup topLevelThreadGroup() {
        if (topLevelTG == null) {
            ThreadGroup tltg = topLevelThreadGroup(Thread.currentThread().getThreadGroup());
            topLevelTG = new ThreadGroup(tltg, "oxy") {

                public void uncaughtException(Thread t, Throwable e) {
                    OxygenUtils.error("Uncaught Exception from Thread: " + t, e);
                }
            };
        }
        return topLevelTG;
    }

    public static void cdebug(Object o) {
        if (CDEBUG_IS_SYSTEM_OUT) {
            System.out.println(o);
        } else {
            debug(o);
        }
    }

    public static void debug(Object o) {
        if (o != null && o instanceof Throwable) {
            log().debug("-NO MESSAGE-", (Throwable) o);
        } else {
            log().debug(String.valueOf(o));
        }
    }

    public static void info(Object o) {
        if (o != null && o instanceof Throwable) {
            log().info("-NO MESSAGE-", (Throwable) o);
        } else {
            log().info(String.valueOf(o));
        }
    }

    public static void error(Object o) {
        if (o != null && o instanceof Throwable) {
            error((String) null, (Throwable) o);
        } else {
            error(String.valueOf(o), (Throwable) null);
        }
    }

    public static void error(String message, Throwable thr) {
        message = ((message == null) ? "-NO MESSAGE-" : message);
        if (thr == null) {
            log().error(message);
        } else {
            log().error(message, thr);
        }
    }

    public static void reverseArray(Object[] arr) {
        int size = arr.length;
        for (int i = 0, mid = size >> 1, j = size - 1; i < mid; i++, j--) {
            Object tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    public static void waitForSocketAccept(String host, int port, long timeoutms, long sleeptime) throws Exception {
        log().debug("Attempting connection to " + host + " at port " + port + " with timeout of " + timeoutms + "ms");
        long start = System.currentTimeMillis();
        int retry = 0;
        long elapsed = 0;
        Socket socket = null;
        try {
            while (true) {
                try {
                    socket = new Socket(host, port);
                    elapsed = System.currentTimeMillis() - start;
                    log().debug("successfully got connection after " + (elapsed / 1000) + " seconds");
                    break;
                } catch (Exception e) {
                    elapsed = System.currentTimeMillis() - start;
                    if (elapsed > timeoutms) {
                        log().debug("Timeout Period of " + (timeoutms / 1000) + " seconds elapsed - could not get connection");
                        throw new Exception("TimedOut after " + (timeoutms / 1000) + " seconds - Could not connect to Server");
                    }
                    retry++;
                    try {
                        Thread.sleep(sleeptime);
                    } catch (InterruptedException ie) {
                    }
                }
            }
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (Exception se) {
            }
        }
    }

    private static Log log() {
        if (log0 == null) {
            log0 = LogFactory.getLog(OxygenConstants.LOGGER_BASE_PREFIX);
        }
        return log0;
    }

    private static ThreadGroup topLevelThreadGroup(ThreadGroup tg0) {
        ThreadGroup tg = tg0.getParent();
        if (tg == null) {
            return tg0;
        } else {
            return topLevelThreadGroup(tg);
        }
    }
}
