package org.gvsig.remoteClient.utils;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.gvsig.remoteClient.wms.ICancellable;

/**
 * Clase con m�todos de utilidad en el protocolo WMS
 *
 * @authors Laura D�az, jaume dominguez faus
 */
public class Utilities {

    private static String characters;

    static boolean canceled;

    static final long latency = 500;

    /**
	 * Used to cancel a group of files
	 * <b>key</b>: Group id, <b>value</b>: Boolean (true if
	 * the group has to be canceled. Otherwise it is
	 * false)
	 */
    static Hashtable canceledGroup = new Hashtable();

    /**
	 * <b>key</b>: URL, <b>value</b>: path to the downloaded file.
	 */
    private static Hashtable downloadedFiles;

    static Exception downloadException;

    private static final String tempDirectoryPath = System.getProperty("java.io.tmpdir") + "/tmp-andami";

    private static Logger logger = Logger.getLogger(Utilities.class.getName());

    static {
        characters = "";
        for (int j = 32; j <= 127; j++) {
            characters += (char) j;
        }
        characters += "�������������������������������������������ǡ�����\n\r\f\t��";
    }

    /**
	 * Checks a File and tries to figure if the file is a text or a binary file.<br>
	 * Keep in mind that binary files are those that contains at least one
	 * non-printable character.
	 *
	 * @param file
	 * @return <b>true</b> when the file is <b>pretty problably</b> text,
	 * <b>false</b> if the file <b>is</b> binary.
	 */
    public static boolean isTextFile(File file) {
        return isTextFile(file, 1024);
    }

    /**
	 * Checks a File and tries to figure if the file is a text or a binary file.<br>
	 * Keep in mind that binary files are those that contains at least one
	 * non-printable character.
	 *
	 * @param file
	 * @param byteAmount, number of bytes to check.
	 * @return <b>true</b> when the file is <b>pretty problably</b> text,
	 * <b>false</b> if the file <b>is</b> binary.
	 */
    public static boolean isTextFile(File file, int byteAmount) {
        int umbral = byteAmount;
        try {
            FileReader fr = new FileReader(file);
            for (int i = 0; i < umbral; i++) {
                int c = fr.read();
                if (c == -1) {
                    return true;
                }
                char ch = (char) c;
                if (characters.indexOf(ch) == -1) {
                    return false;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
	 * Checks a byte array and tells if it contains only text or contains
	 * any binary data.
	 *
	 * @param file
	 * @return <b>true</b> when the data is <b>only</b> text, <b>false</b> otherwise.
	 * @deprecated
	 */
    public static boolean isTextData(byte[] data) {
        char[] charData = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            charData[i] = (char) data[i];
        }
        for (int i = 0; i < data.length; i++) {
            int c = charData[i];
            if (c == -1) {
                return true;
            }
            char ch = (char) c;
            if (characters.indexOf(ch) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
	 * Copia el contenido de un InputStream en un OutputStream
	 *
	 * @param in InputStream
	 * @param out OutputStream
	 */
    public static void serializar(InputStream in, OutputStream out) {
        byte[] buffer = new byte[102400];
        int n;
        try {
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Elimina del xml la declaraci�n del DTD
	 *
	 * @param bytes bytes del fichero XML de respuesta a getCapabilities
	 * @param startTag Tag raiz del xml respuesta a getCapabilities
	 *
	 * @return bytes del fichero XML sin la declaraci�n del DTD
	 */
    public static byte[] eliminarDTD(byte[] bytes, String startTag) {
        String text = new String(bytes);
        int index1 = text.indexOf("?>") + 2;
        int index2;
        try {
            index2 = findBeginIndex(bytes, startTag);
        } catch (NoSuchObjectException e) {
            return bytes;
        }
        byte[] buffer = new byte[bytes.length - (index2 - index1)];
        System.arraycopy(bytes, 0, buffer, 0, index1);
        System.arraycopy(bytes, index2, buffer, index1, bytes.length - index2);
        return buffer;
    }

    /**
	 * Obtiene el �ndice del comienzo del xml
	 *
	 * @param bytes bytes del fichero XML en el que se busca
	 * @param tagRaiz Tag raiz del xml respuesta a getCapabilities
	 *
	 * @return �ndice donde empieza el tag raiz
	 *
	 * @throws NoSuchObjectException Si no se encuentra el tag
	 */
    private static int findBeginIndex(byte[] bytes, String tagRaiz) throws NoSuchObjectException {
        try {
            int nodo = 0;
            int ret = -1;
            int i = 0;
            while (true) {
                switch(nodo) {
                    case 0:
                        if (bytes[i] == '<') {
                            ret = i;
                            nodo = 1;
                        }
                        break;
                    case 1:
                        if (bytes[i] == ' ') {
                        } else if (bytes[i] == tagRaiz.charAt(0)) {
                            nodo = 2;
                        } else {
                            nodo = 0;
                        }
                        break;
                    case 2:
                        String aux = new String(bytes, i, 18);
                        if (aux.equalsIgnoreCase(tagRaiz.substring(1))) {
                            return ret;
                        }
                        nodo = 0;
                        break;
                }
                i++;
            }
        } catch (Exception e) {
            throw new NoSuchObjectException("No se pudo parsear el xml");
        }
    }

    /**
	 * Converts the contents of a Vector to a comma separated list
	 *
	 * */
    public static String Vector2CS(Vector v) {
        String str = new String();
        if (v != null) {
            int i;
            for (i = 0; i < v.size(); i++) {
                str = str + v.elementAt(i);
                if (i < v.size() - 1) str = str + ",";
            }
        }
        return str;
    }

    public static boolean isValidVersion(String version) {
        if (version.trim().length() == 5) {
            if ((version.charAt(1) == '.') && (version.charAt(3) == '.')) {
                char x = version.charAt(0);
                char y = version.charAt(2);
                char z = version.charAt(4);
                if ((Character.isDigit(x)) && (Character.isDigit(y)) && (Character.isDigit(z))) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
	 * Crea un fichero temporal con un nombre concreto y unos datos pasados por
	 * par�metro.
	 * @param fileName Nombre de fichero
	 * @param data datos a guardar en el fichero
	 */
    public static void createTemp(String fileName, String data) throws IOException {
        File f = new File(fileName);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        dos.writeBytes(data);
        dos.close();
        f.deleteOnExit();
    }

    /**
	 * Checks if a String is a number or not
	 *
	 * @param String, s
	 * @return boolean, true if s is a number
	 */
    public static boolean isNumber(String s) {
        try {
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
	 * Parses the String containing different items [character] separated and
	 * creates a vector with them.
	 * @param str String contains item1[c]item2[c]item3...
	 * @param c is the string value for separating the items
	 * @return Vector containing all the items
	 */
    public static Vector createVector(String str, String c) {
        StringTokenizer tokens = new StringTokenizer(str, c);
        Vector v = new Vector();
        try {
            while (tokens.hasMoreTokens()) {
                v.addElement(tokens.nextToken());
            }
            return v;
        } catch (Exception e) {
            return new Vector();
        }
    }

    /**
	 * @param dimensions
	 * @return
	 */
    public static String Vector2URLParamString(Vector v) {
        if (v == null) return "";
        String s = "";
        for (int i = 0; i < v.size(); i++) {
            s += v.get(i);
            if (i < v.size() - 1) s += "&";
        }
        return s;
    }

    /**
	 * Return the content of a file that has been created 
	 * from a URL using the HTTP POST protocol
	 * @param url
	 * The URL
	 * @param data
	 * The data to send on the query
	 * @return
	 * File containing this URL's content or null if no file was found.
	 */
    private static File getPreviousDownloadedURL(URL url, String data) {
        if (data == null) return getPreviousDownloaded(url);
        return getPreviousDownloaded(url + data);
    }

    /**
	 * Returns the content of a URL as a file from the file system.<br>
	 * <p>
	 * If the URL has been already downloaded in this session and notified
	 * to the system using the static <b>Utilities.addDownloadedURL(URL)</b>
	 * method, it can be restored faster from the file system avoiding to
	 * download it again.
	 * </p>
	 * @param url
	 * @return File containing this URL's content or null if no file was found.
	 */
    private static File getPreviousDownloaded(Object object) {
        File f = null;
        if (downloadedFiles != null && downloadedFiles.containsKey(object)) {
            String filePath = (String) downloadedFiles.get(object);
            f = new File(filePath);
            if (!f.exists()) return null;
        }
        return f;
    }

    /**
	 * Adds an URL to the table of downloaded files for further uses. If the URL
	 * already exists in the table its filePath value is updated to the new one and
	 * the old file itself is removed from the file system.
	 *
	 * @param url
	 * @param filePath
	 */
    static void addDownloadedURL(URL url, String filePath) {
        if (downloadedFiles == null) downloadedFiles = new Hashtable();
        String fileName = (String) downloadedFiles.put(url, filePath);
    }

    /** 
	 * Computes the correct temporal filename based on an input name.
	 * 
	 * @param name
	 * @return Computed temporal filename
	 */
    private static String calculateFileName(String name) {
        int index = name.lastIndexOf(".");
        if (index > 0) {
            return tempDirectoryPath + File.separator + name.substring(0, index) + System.currentTimeMillis() + name.substring(index, name.length());
        }
        return tempDirectoryPath + File.separator + name + System.currentTimeMillis();
    }

    /**
	 * Downloads an URL into a temporary file that is removed the next time the
	 * tempFileManager class is called, which means the next time gvSIG is launched.
	 *
	 * @param url
	 * @param name
	 * @return
	 * @throws IOException
	 * @throws ServerErrorResponseException
	 * @throws ConnectException
	 * @throws UnknownHostException
	 */
    public static synchronized File downloadFile(URL url, String name, ICancellable cancel) throws IOException, ConnectException, UnknownHostException {
        return downloadFile(url, null, name, cancel);
    }

    /**
	 * Downloads a URL using the HTTP Post protocol
	 * @param url
	 * The server URL
	 * @param data
	 * The data to send in the request
	 * @param name
	 * A common name for all the retrieved files
	 * @param cancel
	 * Used to cancel the downloads
	 * @return
	 * The retrieved file
	 * @throws IOException
	 * @throws ConnectException
	 * @throws UnknownHostException
	 */
    public static synchronized File downloadFile(URL url, String data, String name, ICancellable cancel) throws IOException, ConnectException, UnknownHostException {
        File f = null;
        if ((f = getPreviousDownloadedURL(url, data)) == null) {
            File tempDirectory = new File(tempDirectoryPath);
            if (!tempDirectory.exists()) tempDirectory.mkdir();
            f = new File(calculateFileName(name));
            if (cancel == null) {
                cancel = new ICancellable() {

                    public boolean isCanceled() {
                        return false;
                    }

                    public Object getID() {
                        return Utilities.class.getName();
                    }
                };
            }
            Monitor monitorObj = new Monitor(cancel);
            Thread downloader = new Thread(new Downloader(url, data, f, cancel.getID()));
            Thread monitor = new Thread(monitorObj);
            monitor.start();
            downloader.start();
            while (!getCanceled(cancel.getID()) && downloader.isAlive()) {
                try {
                    Thread.sleep(latency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                monitorObj.setFinish(true);
                monitor.join();
                downloader.join();
            } catch (InterruptedException e1) {
                logger.warning(e1.getMessage());
            }
            downloader = null;
            monitor = null;
            if (getCanceled(cancel.getID())) return null;
            if (Utilities.downloadException != null) {
                Exception e = Utilities.downloadException;
                if (e instanceof FileNotFoundException) throw (IOException) e; else if (e instanceof IOException) throw (IOException) e; else if (e instanceof ConnectException) throw (ConnectException) e; else if (e instanceof UnknownHostException) throw (UnknownHostException) e;
            }
        } else {
            logger.info(url.toString() + " cached at '" + f.getAbsolutePath() + "'");
        }
        return f;
    }

    /**
	 * Try if a group of downloads has been canceled
	 * @param groupId
	 * Group id
	 * @return
	 * If the group has been canceled
	 */
    protected static boolean getCanceled(Object groupId) {
        Object obj = canceledGroup.get(groupId);
        if (obj != null) {
            return ((Boolean) obj).booleanValue();
        }
        return false;
    }

    /**
	 * Cancel a group of downloads
	 * @param groupId
	 * Group id
	 * @param isCanceled
	 * if the group has to be canceled
	 */
    protected static void setCanceled(Object groupId, boolean isCanceled) {
        if (groupId == null) {
            groupId = Utilities.class.getName();
        }
        canceledGroup.put(groupId, new Boolean(isCanceled));
    }

    /**
	 * Cleans every temporal file previously downloaded.
	 */
    public static void cleanUpTempFiles() {
        try {
            File tempDirectory = new File(tempDirectoryPath);
            File[] files = tempDirectory.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) deleteDirectory(files[i]);
                    files[i].delete();
                }
            }
            tempDirectory.delete();
        } catch (Exception e) {
        }
    }

    /**
	 * Recursive directory delete.
	 * @param f
	 */
    private static void deleteDirectory(File f) {
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) deleteDirectory(files[i]);
            files[i].delete();
        }
    }

    /**
	 * Remove an URL from the system cache. The file will remain in the file
	 * system for further eventual uses.
	 * @param request
	 */
    public static void removeURL(URL url) {
        if (downloadedFiles != null && downloadedFiles.containsKey(url)) downloadedFiles.remove(url);
    }

    /**
	 * Remove an URL from the system cache. The file will remain in the file
	 * system for further eventual uses.
	 * @param request
	 */
    public static void removeURL(Object url) {
        if (downloadedFiles != null && downloadedFiles.containsKey(url)) downloadedFiles.remove(url);
    }
}

final class Monitor implements Runnable {

    ICancellable c;

    private volatile boolean finish = false;

    public Monitor(ICancellable cancel) {
        Utilities.setCanceled(cancel.getID(), false);
        this.c = cancel;
    }

    public void run() {
        while (!c.isCanceled() && !getFinish()) {
            try {
                Thread.sleep(Utilities.latency);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (c.isCanceled()) Utilities.setCanceled(c.getID(), true);
    }

    public synchronized void setFinish(boolean value) {
        finish = value;
    }

    public synchronized boolean getFinish() {
        return finish;
    }
}

final class Downloader implements Runnable {

    private URL url;

    private File dstFile;

    private Object groupID = null;

    private String data = null;

    private static Logger logger = Logger.getLogger(Downloader.class.getName());

    public Downloader(URL url, File dstFile, Object groupID) {
        this.url = url;
        this.dstFile = dstFile;
        this.groupID = groupID;
        Utilities.downloadException = null;
    }

    public Downloader(URL url, String data, File dstFile, Object groupID) {
        this.url = url;
        this.data = data;
        this.dstFile = dstFile;
        this.groupID = groupID;
        Utilities.downloadException = null;
    }

    public void run() {
        logger.info("downloading '" + url.toString() + "' to: " + dstFile.getAbsolutePath());
        Preferences prefs = Preferences.userRoot().node("gvsig.downloader");
        int timeout = prefs.getInt("timeout", 60000);
        DataOutputStream dos;
        try {
            DataInputStream is;
            OutputStreamWriter os = null;
            HttpURLConnection connection = null;
            if (url.getProtocol().equals("https")) {
                disableHttsValidation();
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            if (data != null) {
                connection.setRequestProperty("SOAPAction", "post");
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
                os = new OutputStreamWriter(connection.getOutputStream());
                os.write(data);
                os.flush();
                is = new DataInputStream(connection.getInputStream());
            } else {
                is = new DataInputStream(url.openStream());
            }
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dstFile)));
            byte[] buffer = new byte[1024 * 4];
            long readed = 0;
            for (int i = is.read(buffer); !Utilities.getCanceled(groupID) && i > 0; i = is.read(buffer)) {
                dos.write(buffer, 0, i);
                readed += i;
            }
            if (os != null) {
                os.close();
            }
            dos.close();
            is.close();
            is = null;
            dos = null;
            if (Utilities.getCanceled(groupID)) {
                logger.warning("[RemoteServices] '" + url + "' CANCELED.");
                dstFile.delete();
                dstFile = null;
            } else {
                Utilities.addDownloadedURL(url, dstFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utilities.downloadException = e;
        }
    }

    /**
	 * This method disables the Https certificate validation.
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
    private void disableHttsValidation() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
}
