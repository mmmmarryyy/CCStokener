package vxmlsurfer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

public class FileManager {

    public static final int IN_MEMORY = 0;

    public static final int ON_DISK = 1;

    public static final boolean PREFETCHING_ON = true;

    public static final boolean PREFETCHING_OFF = false;

    private static boolean prefetch;

    private static int cacheLevel = 1;

    private static final String CACHEDIR = "cache/";

    private static HashMap<String, URIElement> cache = new HashMap<String, URIElement>();

    private static BufferedWriter log = null;

    static {
        flushCache();
        try {
            log = new BufferedWriter(new FileWriter(CACHEDIR + "log.txt"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static InputStream read(String URI) {
        boolean fetchFromServer = false;
        writeToLog("* Processor requesting read for " + URI);
        URIElement element = cache.get(URI);
        if (element == null) {
            writeToLog("Cache MISS -> " + URI);
            fetchFromServer = true;
        } else if (!element.isValid()) {
            writeToLog("Cache INVALID -> " + URI);
            deleteElement(element);
            fetchFromServer = true;
        }
        if (!fetchFromServer) {
            writeToLog("Cache HIT -> " + URI);
            if (element.isInmemory()) {
                writeToLog("... Read from memory cache");
                return element.getXMLStream();
            } else {
                writeToLog("... Read from disk cache");
                return readFromFile(CACHEDIR + element.getLocalAddress());
            }
        } else {
            writeToLog("... Read from actual location");
            return readFromServer(URI);
        }
    }

    private static InputStream readFromFile(String filename) {
        FileInputStream fin = null;
        try {
            File f = new File(filename);
            fin = new FileInputStream(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fin;
    }

    private static InputStream readFromServer(String filename) {
        InputStream is = null;
        try {
            if (filename.startsWith("http")) {
                URL url = new URL(filename);
                URLConnection uc = url.openConnection();
                is = uc.getInputStream();
            } else {
                is = (InputStream) (new FileInputStream(new File(filename)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return is;
    }

    public static void add(String uri, URIElement element, InputStream stream) {
        if (element.isInmemory()) {
            byte[] data = new byte[0];
            int ptr = 0;
            while (true) {
                byte[] buffer = new byte[100];
                int nob = 0;
                try {
                    nob = stream.read(buffer, ptr, 100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (nob > 0) {
                    ptr += nob;
                    byte[] temp = new byte[ptr];
                    int i = 0;
                    for (; i < data.length; i++) temp[i] = data[i];
                    for (int j = 0; j < nob; j++, i++) temp[i] = buffer[j];
                    data = temp;
                } else break;
            }
            element.setXMLStream(data);
        } else {
            String key = uri.substring(uri.lastIndexOf('/') + 1);
            element.setLocalAddress(key);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                BufferedWriter writer = new BufferedWriter(new FileWriter(CACHEDIR + key));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
                writer.close();
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        element.setEntryTime(System.currentTimeMillis());
        cache.put(uri, element);
    }

    public static int getCacheLevel() {
        return cacheLevel;
    }

    public static void setCacheLevel(int cacheLevel) {
        FileManager.cacheLevel = cacheLevel;
    }

    public static boolean isInMemory() {
        return cacheLevel == IN_MEMORY ? true : false;
    }

    public static boolean isPrefetch() {
        return prefetch;
    }

    public static void setPrefetch(boolean prefetch) {
        FileManager.prefetch = prefetch;
    }

    public static synchronized void flushCache() {
        cache = new HashMap<String, URIElement>();
        File dir = new File(CACHEDIR);
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) files[i].delete();
    }

    public static boolean isPresent(String url) {
        URIElement element = cache.get(url);
        if (element == null) return false; else {
            if (element.isValid()) return true; else {
                deleteElement(element);
                return false;
            }
        }
    }

    private static void deleteElement(URIElement element) {
        if (!element.isInmemory()) {
            try {
                new File(CACHEDIR + element.getLocalAddress()).delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cache.remove(element.getURI());
    }

    static void writeToLog(String message) {
        try {
            log.write(message);
            log.newLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void closeLog() {
        try {
            log.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
