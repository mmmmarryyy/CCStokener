package uk.co.massycat.appreviewsfinder;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author ben
 */
public class Utilities {

    static final String kWindows = "Windows";

    static final String kMacOsX = "Mac OS X";

    public static boolean isMacOSX() {
        boolean is_os_x = false;
        String os_name = System.getProperty("os.name");
        is_os_x = os_name.compareToIgnoreCase(kMacOsX) == 0;
        return is_os_x;
    }

    public static boolean isWindowsOS() {
        boolean windows = false;
        String os_name = System.getProperty("os.name");
        if (os_name.length() >= kWindows.length()) {
            String sub_os = os_name.substring(0, kWindows.length());
            if (sub_os.equals(kWindows)) {
                windows = true;
            }
        }
        return windows;
    }

    public static boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            String[] children = directory.list();
            for (int i = 0; i < children.length; i++) {
                boolean sub_good = Utilities.deleteDirectory(new File(directory, children[i]));
                if (!sub_good) {
                    return false;
                }
            }
        }
        return directory.delete();
    }

    public static String connectAndGetResponse(String url_str, String itunes_code) {
        String xml_string = null;
        try {
            URL url = new URL(url_str);
            HttpURLConnection http_conn = (HttpURLConnection) url.openConnection();
            http_conn.setRequestProperty("X-Apple-Store-Front", itunes_code);
            http_conn.setRequestProperty("User-Agent", "iTunes-iPhone/2.2 (2)");
            http_conn.connect();
            if (http_conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                int content_length = http_conn.getContentLength();
                InputStream input_stream = http_conn.getInputStream();
                int alloc_length = content_length;
                int read_total = 0;
                if (content_length < 0) {
                    alloc_length = 512 * 1024;
                }
                byte[] read_buffer = new byte[alloc_length];
                try {
                    while (read_total < read_buffer.length) {
                        int read = input_stream.read(read_buffer, read_total, read_buffer.length - read_total);
                        if (read > 0) {
                            read_total += read;
                            if (content_length < 0) {
                            }
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                }
                if (read_total > 0) {
                    xml_string = new String(read_buffer, 0, read_total, "UTF-8");
                }
            }
        } catch (Exception e) {
            System.err.println("What went wrong? : " + e);
        }
        return xml_string;
    }

    public class DirsOnlyFileFilter implements FileFilter {

        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }

    private DirsOnlyFileFilter mDirsOnlyFileFilter = new DirsOnlyFileFilter();

    private static Utilities sUtilities = null;

    public static DirsOnlyFileFilter getDirsOnlyFileFilter() {
        if (sUtilities == null) {
            sUtilities = new Utilities();
        }
        return sUtilities.mDirsOnlyFileFilter;
    }

    public static String makeStartTag(String tag) {
        return "<" + tag + ">";
    }

    public static String makeEndTag(String tag) {
        return "</" + tag + ">";
    }

    public static String makeElement(String tag, String value) {
        return makeStartTag(tag) + value + makeEndTag(tag);
    }
}
