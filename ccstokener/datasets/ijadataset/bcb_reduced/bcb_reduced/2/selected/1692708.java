package se.sics.cooja.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;

/**
 * Some utility methods for generating hex dumps.
 *
 * @author Niclas Finne, Fredrik Osterlind
 */
public class StringUtils {

    private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private StringUtils() {
    }

    public static String toHex(byte[] data) {
        char[] buf = new char[data.length * 2];
        for (int i = 0, j = 0, n = data.length; i < n; i++, j += 2) {
            buf[j] = HEX[(data[i] >> 4) & 0xf];
            buf[j + 1] = HEX[data[i] & 0xf];
        }
        return new String(buf);
    }

    public static String toHex(byte[] data, int bytesPerGroup) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, n = data.length; i < n; i++) {
            if ((i % bytesPerGroup) == 0 && i > 0) {
                sb.append(' ');
            }
            sb.append(HEX[(data[i] >> 4) & 0xf]);
            sb.append(HEX[data[i] & 0xf]);
        }
        return sb.toString();
    }

    public static String hexDump(byte[] data) {
        return hexDump(data, 5, 4);
    }

    public static String hexDump(byte[] data, int groupsPerLine, int bytesPerGroup) {
        if (bytesPerGroup <= 0) {
            throw new IllegalArgumentException("0 bytes per group");
        }
        if (groupsPerLine <= 0) {
            groupsPerLine = 1;
        }
        final int bytesPerLine = groupsPerLine * bytesPerGroup;
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < data.length; j += bytesPerLine) {
            int n = data.length - j;
            if (n > bytesPerLine) {
                n = bytesPerLine;
            }
            for (int i = 0; i < bytesPerLine; i++) {
                if ((i % bytesPerGroup) == 0 && i > 0) {
                    sb.append(' ');
                }
                if (i < n) {
                    sb.append(HEX[(data[j + i] >> 4) & 0xf]);
                    sb.append(HEX[data[j + i] & 0xf]);
                } else {
                    sb.append("  ");
                }
            }
            sb.append("  ");
            for (int i = 0; i < n; i++) {
                if (data[j + i] > 32) {
                    sb.append((char) (data[j + i] & 0xff));
                } else {
                    sb.append('.');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static String loadFromURL(URL url) {
        if (url == null) {
            return null;
        }
        try {
            InputStreamReader reader = new InputStreamReader(url.openStream());
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) > 0) {
                sb.append(buf, 0, read);
            }
            ;
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public static String loadFromFile(File scriptFile) {
        if (scriptFile == null) {
            return null;
        }
        try {
            FileReader reader = new FileReader(scriptFile);
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) > 0) {
                sb.append(buf, 0, read);
            }
            ;
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean saveToFile(File file, String text) {
        try {
            PrintWriter outStream = new PrintWriter(new FileWriter(file));
            outStream.print(text);
            outStream.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
