package com.muddyhorse.cynch;

import java.awt.Frame;
import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import com.muddyhorse.cynch.manifest.*;

/**
 *
 */
public class UpdateUtils implements Constants {

    private static final int BUFFER_SIZE = 10000;

    public static final RemoteFileInfo DUMMY_REMOTE_INFO = new RemoteFileInfo("");

    private static Frame mainframe;

    private static Config mainConfig;

    public static String getStringFromClasspath(Class<?> base, String filename) {
        try {
            InputStream is = base.getResourceAsStream(filename);
            if (is != null) {
                int available = is.available();
                byte buffer[] = new byte[available];
                is.read(buffer);
                is.close();
                String tmp = new String(buffer);
                return tmp;
            } else {
                return "";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /** Read the contents of a webpage at the given address */
    public static String getStringFromURL(String urlString) throws MalformedURLException {
        return getStringFromURL(new URL(urlString));
    }

    /** Read the contents of a webpage indicated by a URL */
    public static String getStringFromURL(URL url) {
        try {
            if (url == null) {
                return null;
            }
            URLConnection conn = url.openConnection();
            conn.connect();
            if (conn instanceof HttpURLConnection) {
                int status = ((HttpURLConnection) conn).getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    return null;
                }
            }
            int available = conn.getContentLength();
            InputStream is = conn.getInputStream();
            byte buffer[] = new byte[available];
            int pos = 0;
            while (available > 0) {
                int amount = is.read(buffer, pos, available);
                available -= amount;
                pos += amount;
            }
            is.close();
            String tmp = new String(buffer);
            return tmp;
        } catch (Exception ex) {
            System.out.println(ex);
            return "";
        }
    }

    /** Read the contents of a file */
    public static String getStringFromFile(String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            int available = fis.available();
            byte buffer[] = new byte[available];
            fis.read(buffer);
            fis.close();
            String tmp = new String(buffer);
            return tmp;
        } catch (Exception ex) {
            return "";
        }
    }

    /** Erase all C style comments in the string prior to parsing it */
    public static String eraseComments(String input) {
        int count = input.length();
        StringBuffer sb = new StringBuffer(count);
        boolean isCComment = false;
        boolean isString = false;
        for (int i = 0; i < count; i++) {
            char c = input.charAt(i);
            if (c == '\"') {
                isString = !isString;
            }
            if (!isString && c == '/' && input.charAt(i + 1) == '*') {
                isCComment = true;
                i++;
            }
            if (!isCComment) {
                sb.append(c);
            }
            if (!isString && c == '*' && input.charAt(i + 1) == '/') {
                isCComment = false;
                i++;
            }
        }
        return sb.toString();
    }

    private static int offsetToEOL(String data, int pos) {
        int i = 0, count = data.length();
        for (; pos + i < count && !Character.isISOControl(data.charAt(pos + i)); i++) {
        }
        return i;
    }

    private static int offsetToNextLine(String data, int pos) {
        int i = 0, count = data.length();
        for (; pos + i < count && !Character.isISOControl(data.charAt(pos + i)); i++) {
        }
        for (; pos + i < count && Character.isISOControl(data.charAt(pos + i)); i++) {
        }
        return i;
    }

    /** Split the file, line by line, into key/value pairs */
    public static Map<String, String> stringToHashtable(String file) {
        if (file == null) {
            return null;
        }
        String sansComments = eraseComments(file);
        int pos = 0, count = sansComments.length();
        Map<String, String> retval = new HashMap<String, String>();
        while (pos < count) {
            int eol = offsetToEOL(sansComments, pos);
            int next = offsetToNextLine(sansComments, pos);
            if (eol > 0) {
                String line = sansComments.substring(pos, pos + eol);
                StringTokenizer st = new StringTokenizer(line, "=");
                String key = st.nextToken();
                String value = "";
                if (st.hasMoreTokens()) {
                    value = st.nextToken();
                }
                retval.put(key, value);
            }
            pos += next;
        }
        return retval;
    }

    /** Write a hashtable to file */
    public static void writeHashtable(String filename, Map<String, String> hash) {
        try {
            FileWriter fw = new FileWriter(filename);
            Set<Entry<String, String>> entries = hash.entrySet();
            for (Entry<String, String> entry : entries) {
                String key = entry.getKey();
                String value = entry.getValue();
                String str = key + "=" + value;
                fw.write(str);
                fw.write("\r\n");
            }
            fw.close();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /** Compare local and remote manifests and return a hashtable
     *  of operations to perform.
     */
    public static Map<String, Operation> compareManifests(LocalManifest localManifest, RemoteManifest remoteManifest) {
        SortedMap<String, LocalFileInfo> localFIs = localManifest.getAllFileInfo();
        TreeMap<String, RemoteFileInfo> remoteFIs = new TreeMap<String, RemoteFileInfo>(remoteManifest.getAllFileInfo());
        Map<String, Operation> rv = new TreeMap<String, Operation>();
        for (LocalFileInfo lfi : localFIs.values()) {
            String key = lfi.getFileID();
            RemoteFileInfo rfi = remoteFIs.get(key);
            if (rfi != null) {
                remoteFIs.remove(key);
            }
            Operation op = new Operation(key);
            op.setLocal(lfi);
            op.setRemote(rfi);
            if (rfi == null) {
                op.setOperation(OperationType.delete);
            } else {
                BigDecimal rVersion = rfi.getVersion();
                BigDecimal lVersion = lfi.getVersion();
                if (rVersion == null) {
                    throw new IllegalStateException("Remote version information not found");
                }
                if (lVersion == null) {
                    throw new IllegalStateException("Local version information not found");
                }
                if (rVersion.compareTo(lVersion) > 0) {
                    op.setOperation(OperationType.update);
                    LocalFileInfo localInfo = rfi.getLocalInfo(localManifest.getBase());
                    op.setLocal(localInfo);
                } else {
                    op.setOperation(OperationType.nothing);
                }
            }
            rv.put(key, op);
        }
        Iterator<Entry<String, RemoteFileInfo>> remoteEntryItor = remoteFIs.entrySet().iterator();
        while (remoteEntryItor.hasNext()) {
            Entry<String, RemoteFileInfo> remoteEntry = remoteEntryItor.next();
            String key = remoteEntry.getKey();
            RemoteFileInfo rfi = remoteEntry.getValue();
            Operation op = new Operation(key);
            op.setRemote(rfi);
            LocalFileInfo localInfo = rfi.getLocalInfo(localManifest.getBase());
            op.setLocal(localInfo);
            op.setOperation(OperationType.download);
            rv.put(key, op);
        }
        return rv;
    }

    /**
     * @param ids list of IDs to work with.  If null, all IDs will be
     *   accepted.
     * @return an array of Vectors.  An operation is placed in the vector
     *   at the offset corresponding to its operation kind.  All of these
     *   vectors are guaranteed to be non-null;
     */
    public static Map<OperationType, List<Operation>> sortOperationsByOp(Map<String, Operation> ops, List<String> ids) {
        Map<OperationType, List<Operation>> vs = new HashMap<OperationType, List<Operation>>();
        for (OperationType op : OperationType.values()) {
            vs.put(op, new ArrayList<Operation>());
        }
        Set<Entry<String, Operation>> entries = ops.entrySet();
        for (Entry<String, Operation> entry : entries) {
            String k = entry.getKey();
            if (ids == null || ids.contains(k)) {
                Operation op = entry.getValue();
                OperationType olt = op.getOperation();
                vs.get(olt).add(op);
            }
        }
        return vs;
    }

    /**
     * @param ids list of IDs to work with.  If null, all IDs will be
     *   accepted.
     * @return a list of 3 lists of operations.  An operation is placed in the list
     *   at the offset corresponding to its operation kind.  All of these
     *   lists are guaranteed to be non-null;
     */
    public static Map<DownloadType, List<Operation>> sortOperationsByType(Map<String, Operation> ops, List<String> ids) {
        Map<DownloadType, List<Operation>> vs = new HashMap<DownloadType, List<Operation>>();
        vs.put(DownloadType.required, new ArrayList<Operation>());
        vs.put(DownloadType.critical, new ArrayList<Operation>());
        vs.put(DownloadType.optional, new ArrayList<Operation>());
        vs.put(null, new ArrayList<Operation>());
        Set<Entry<String, Operation>> entries = ops.entrySet();
        for (Entry<String, Operation> entry : entries) {
            String k = entry.getKey();
            if (ids == null || ids.contains(k)) {
                Operation op = entry.getValue();
                RemoteFileInfo rmt = op.getRemote();
                DownloadType dlt;
                if (rmt != null) {
                    dlt = rmt.getDownloadType();
                } else {
                    dlt = null;
                }
                vs.get(dlt).add(op);
            }
        }
        return vs;
    }

    /**
     * Note that this method removes operations from the config as
     *  they are successfully completed.
     * @param cfg the hashtable to write the results to.  If null, no
     *   record of the transaction is kept.
     * @param type the type of operation to perform. If the type is TYPE_ALL,
     *  all types will be performed.
     * @return the count of errors that occurred in performing the
     *   various operations.
     */
    public static int performOperations(Config cfg, DownloadType type, List<Operation> ops, ProgressListener l) {
        int errorCnt = 0;
        for (Operation op : ops) {
            DownloadType downloadType;
            RemoteFileInfo remote = op.getRemote();
            if (remote != null) {
                downloadType = remote.getDownloadType();
            } else {
                downloadType = null;
            }
            if (type == DownloadType.all || type == downloadType) {
                if (!performOperation(cfg, op, l)) {
                    ++errorCnt;
                } else {
                    cfg.getOperations().remove(op.getFileID());
                }
            }
        }
        return errorCnt;
    }

    /**
     * @param cfg the DUConfig containing context information
     * @return true if the operation was successful.
     */
    public static boolean performOperation(Config cfg, Operation op, ProgressListener l) {
        System.out.println("uu.pO: starting...");
        try {
            long dlSize = 0;
            l.starting(op);
            RemoteFileInfo rfi = op.getRemote();
            URL u;
            if (rfi != null) {
                u = rfi.getPath();
            } else {
                u = null;
                switch(op.getOperation()) {
                    case download:
                    case update:
                        throw new IllegalArgumentException("null remote file info for operation");
                    default:
                }
                rfi = DUMMY_REMOTE_INFO;
            }
            LocalFileInfo lfi = op.getLocal();
            if (lfi == null) {
                throw new IllegalStateException("Operation missing file info; unable to process");
            }
            final File n = lfi.getPath();
            switch(op.getOperation()) {
                case download:
                case update:
                    {
                        String tempPrefix = cfg.getAppShortName();
                        String tempSuffix = "_Cynch.tmp";
                        insurePathToFileExists(n);
                        File tempf = File.createTempFile(tempPrefix, tempSuffix, n.getParentFile());
                        dlSize = getFileFromURL(u, tempf, l);
                        if (dlSize > 0 && verifyFile(tempf)) {
                            postProcessDownload(n, tempf, rfi.getAction());
                        } else {
                            throw new IllegalStateException("(" + op.getOperation() + ") File verification problem: " + tempf);
                        }
                    }
                    break;
                case delete:
                    {
                        if (!n.delete()) {
                            throw new IllegalStateException("File deletion problem: " + n);
                        }
                        cfg.getLocalManifest().remove(op.getFileID());
                        cfg.getLocalManifest().save();
                    }
                    break;
                case nothing:
                    {
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown operation type: " + op.getOperation());
            }
            op.getRemote().setSize(dlSize);
            if (op.getOperation() != OperationType.nothing) {
                cfg.getLocalManifest().getAllFileInfo().put(lfi.getFileID(), lfi);
                cfg.getLocalManifest().save();
            }
            l.finished(op, true);
            return true;
        } catch (Exception ex) {
            System.out.println("uu:" + ex);
            ex.printStackTrace();
            l.finished(op, false);
            return false;
        }
    }

    private static void postProcessDownload(final File targetFile, File tempFile, PostDownloadActionType action) throws ZipException, IOException {
        File backf = new File(targetFile.getPath() + ".bak");
        boolean b = false;
        if (backf.exists()) {
            if (backf.isDirectory()) {
                b = true;
            } else {
                b = backf.delete();
            }
            if (!b) {
                throw new IllegalStateException("(Update)File delete problem: " + backf);
            }
        }
        if (targetFile.exists() && !targetFile.isDirectory()) {
            b = targetFile.renameTo(backf);
            if (!b) {
                throw new IllegalStateException("(Update)File Rename problem: " + targetFile);
            }
        }
        switch(action) {
            case nothing:
                {
                    b = tempFile.renameTo(targetFile);
                    if (!b) {
                        throw new IllegalStateException("(Update)File Rename problem: " + tempFile);
                    }
                }
                break;
            case unzip:
                {
                    unzipFileToLocation(tempFile, targetFile);
                    b = tempFile.delete();
                    if (!b) {
                        throw new IllegalStateException("(unzip)File delete problem: " + tempFile);
                    }
                }
                break;
            default:
                break;
        }
    }

    private static void unzipFileToLocation(File tempFile, File targetFile) throws ZipException, IOException {
        ZipFile zf = new ZipFile(tempFile);
        Enumeration<? extends ZipEntry> enumer = zf.entries();
        while (enumer.hasMoreElements()) {
            ZipEntry ze = enumer.nextElement();
            if (!ze.isDirectory()) {
                File newf = new File(targetFile, ze.getName());
                insurePathToFileExists(newf);
                OutputStream zout = new BufferedOutputStream(new FileOutputStream(newf));
                InputStream zin = zf.getInputStream(ze);
                connectStreams(zin, zout);
            }
        }
        zf.close();
    }

    public static void connectStreams(InputStream in, OutputStream out) throws IOException {
        byte readBuffer[] = new byte[BUFFER_SIZE];
        int amount = 0;
        while (amount >= 0) {
            amount = in.read(readBuffer, 0, BUFFER_SIZE);
            if (amount == -1) {
                break;
            }
            out.write(readBuffer, 0, amount);
        }
        in.close();
        out.close();
    }

    /** Counts the total download size (in bytes) of downloads and
     *  updates.  Operations that are deletes or "nothing"s do not
     *  add to the total.
     * @param type the type of operation to total
     * @param ids the list of file IDs to consider.  If null,
     *   consider all operations.
     * @return the size of download and update operations, or -1 if
     *   an error occurred.
     */
    public static long countDownloadSize(Config cfg, DownloadType type, List<String> ids) {
        long total = 0;
        try {
            Collection<Operation> ops = cfg.getOperations().values();
            for (Operation op : ops) {
                RemoteFileInfo rmt = op.getRemote();
                if (rmt != null) {
                    if (type == DownloadType.all || type == rmt.getDownloadType()) {
                        if (ids == null || ids.contains(op.getFileID())) {
                            if (op.getOperation() == OperationType.update || op.getOperation() == OperationType.download) {
                                total += rmt.getSize();
                            }
                        }
                    }
                }
            }
            return total;
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    /** Performs in the order: New downloads, Update files, Delete files.
     * Note that this method removes operations from the config as
     *  they are successfully completed.
     * @param cfg the hashtable to write the results to.  If null, no
     *  record of the transaction is kept.
     * @param type the type of operation to perform. If the type is TYPE_ALL,
     *  all types will be performed.
     * @param ids list of IDs to work with.  If null, all IDs will be
     *  accepted.
     * @return the number of errors that occurred while performing the
     *  operations, or -1 if the operation was interrupted (by, for example,
     *  user intervention).
     */
    public static int performAllOperations(Config cfg, DownloadType type, List<String> ids, ProgressListener l) {
        int errorCnt;
        Map<OperationType, List<Operation>> opSets = sortOperationsByOp(cfg.getOperations(), ids);
        errorCnt = performOperations(cfg, type, opSets.get(OperationType.download), l);
        errorCnt += performOperations(cfg, type, opSets.get(OperationType.update), l);
        errorCnt += performOperations(cfg, type, opSets.get(OperationType.delete), l);
        return errorCnt;
    }

    /**
     * @return the number of bytes downloaded, -1 if error occurred
     */
    public static int getFileFromURL(URL rmt, File local, ProgressListener l) throws InterruptedException {
        InputStream is = null;
        OutputStream fs = null;
        try {
            if (rmt == null) {
                return -1;
            }
            URLConnection conn = rmt.openConnection();
            conn.connect();
            if (conn instanceof HttpURLConnection) {
                int status = ((HttpURLConnection) conn).getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    return -1;
                }
            }
            final int available = conn.getContentLength();
            is = conn.getInputStream();
            fs = new BufferedOutputStream(new FileOutputStream(local), BUFFER_SIZE);
            byte buffer[] = new byte[BUFFER_SIZE];
            int left = available;
            while (left > 0) {
                if (l != null) {
                    l.progress(local.getName(), "Downloading", available - left, available);
                }
                int amount = is.read(buffer, 0, BUFFER_SIZE);
                if (amount == -1) {
                    break;
                }
                left -= amount;
                fs.write(buffer, 0, amount);
            }
            System.out.println("uu.gFFURL: downloaded " + (available - left) + " of " + available);
            System.out.println("uu.gFFURL: from " + rmt + " to " + local);
            return available - left;
        } catch (InterruptedException ix) {
            throw ix;
        } catch (Exception ex) {
            System.out.println(ex);
            return -1;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (fs != null) {
                    fs.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * @return the number of bytes downloaded, -1 if error occurred
     */
    public static int getFileFromClasspath(Class<?> base, String rmt, File local) {
        InputStream is = null;
        OutputStream fs = null;
        try {
            if (rmt == null || local == null) {
                return -1;
            }
            is = base.getResourceAsStream(rmt);
            final int available = is.available();
            fs = new BufferedOutputStream(new FileOutputStream(local), BUFFER_SIZE);
            byte buffer[] = new byte[BUFFER_SIZE];
            int left = available;
            while (left > 0) {
                int amount = is.read(buffer, 0, BUFFER_SIZE);
                if (amount == -1) {
                    break;
                }
                left -= amount;
                fs.write(buffer, 0, amount);
            }
            System.out.println("uu.gFFC: downloaded " + (available - left) + " of " + available);
            System.out.println("uu.gFFC: from " + rmt + " to " + local);
            return available - left;
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (fs != null) {
                    fs.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * @return the number of bytes saved, -1 if error occurred
     */
    public static int writeStringToFile(String data, File local) {
        PrintWriter fs = null;
        try {
            if (data == null || local == null) {
                return -1;
            }
            insurePathToFileExists(local);
            fs = new PrintWriter(new BufferedOutputStream(new FileOutputStream(local), BUFFER_SIZE));
            fs.print(data);
            fs.flush();
            return data.length();
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        } finally {
            try {
                if (fs != null) {
                    fs.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static boolean startApplication(Config cfg) {
        return startApplication(cfg.get(INI_EXEC_NAME), cfg.get(INI_EXEC_ARGS), cfg.get(INI_LOCAL_BASE));
    }

    public static boolean startApplication(String execName, String args, String startDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(execName, startDir, args);
            pb.directory(new File(startDir));
            Process p = pb.start();
            int rc = p.waitFor();
            System.out.println("rc is " + rc);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /** This method insures that a path exists to allow creation
     *  of the specified file.
     */
    public static void insurePathToFileExists(String filename) {
        insurePathToFileExists(new File(filename));
    }

    /** This method insures that a path exists to allow creation
     *  of the specified file.
     */
    public static void insurePathToFileExists(File f) {
        File p = f.getParentFile();
        if (!p.exists()) {
            if (!p.mkdirs()) {
                throw new IllegalStateException("Unable to make directory " + p.getPath());
            }
        }
    }

    public static void setMainFrame(Frame f) {
        mainframe = f;
    }

    public static Frame getMainFrame() {
        return mainframe;
    }

    private static boolean verifyFile(File f) {
        return true;
    }

    public static Config getMainConfig() {
        return mainConfig;
    }

    public static void setMainConfig(Config cfg) {
        mainConfig = cfg;
    }
}
