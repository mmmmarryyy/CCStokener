package net.sf.j18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * J18n wrapper class for Java 1.6+.
 * 
 * <p>
 * This wrapper class only compiles at Java 1.6 or higher version, because it's
 * using the new ResourceBundle.Control utility class introduced by Java 1.6.
 * </p>
 * 
 * <p>
 * Java 1.5 has to use UTF8ResourceBundle, because 1.5 doesn't provide a
 * modifiable utility class.
 * </p>
 * 
 * <p>
 * 1.4 or lower is not supported. However, you can download the source of
 * <code>UTF8Properties</code> and modify it by yourself, or you can join this
 * project to help us develop Java 1.4 support.
 * </p>
 * 
 * Replace your previous call to
 * <code>
 *  ResouceBundle.getBundle(xxx);
 * </code>
 * with this UTF8-aware code:
 * <code>
 *  J18n.getBundle(xxx);
 * </code>
 * 
 * @author ilionyl
 */
public final class J18n {

    private static final Control control = new Control();

    /**
     * The factory method to get <code>ResourceBundle.Control</code>.
     * 
     * If you have your own customized ResourceBundle design and therefore
     * are not willing to use J18n directly, you can get the Control and 
     * then pass it to your <code>ResourceBundle.getBundle</code> method call.
     * @return the immutable object
     */
    public static ResourceBundle.Control getControl() {
        return control;
    }

    public static ResourceBundle getBundle(String baseName) {
        return ResourceBundle.getBundle(baseName, getControl());
    }

    public static ResourceBundle getBundle(String baseName, Locale locale) {
        return ResourceBundle.getBundle(baseName, locale, getControl());
    }

    public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader) {
        return ResourceBundle.getBundle(baseName, locale, loader, getControl());
    }

    private static class Control extends ResourceBundle.Control {

        @Override
        public List<String> getFormats(String baseName) {
            if (baseName == null) throw new NullPointerException();
            return Arrays.asList("utf8");
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IOException {
            if (baseName == null || locale == null || format == null || loader == null) throw new NullPointerException();
            ResourceBundle bundle = null;
            if (format.equals("utf8")) {
                String bundleName = toBundleName(baseName, locale);
                String resourceName = toResourceName(bundleName, "properties");
                InputStream stream = null;
                if (reload) {
                    URL url = loader.getResource(resourceName);
                    if (url != null) {
                        URLConnection connection = url.openConnection();
                        if (connection != null) {
                            connection.setUseCaches(false);
                            stream = connection.getInputStream();
                        }
                    }
                } else {
                    stream = loader.getResourceAsStream(resourceName);
                }
                if (stream != null) {
                    BufferedReader bReader = new BufferedReader(new InputStreamReader(new BomStream(stream), "UTF8"));
                    bundle = new UTF8ResourceBundle(bReader);
                    bReader.close();
                }
            }
            return bundle;
        }
    }

    private static class UTF8ResourceBundle extends ResourceBundle {

        private UTF8Properties props;

        public UTF8ResourceBundle(BufferedReader bReader) throws IOException {
            props = new UTF8Properties();
            props.loadUTF8(bReader);
        }

        protected Object handleGetObject(String key) {
            return props.getProperty(key);
        }

        @SuppressWarnings("unchecked")
        public Enumeration<String> getKeys() {
            return (Enumeration<String>) props.propertyNames();
        }
    }

    private static class BomStream extends InputStream {

        private boolean firstByte = true;

        private InputStream inStream;

        private static final int[] BOM = { 239, 187, 191 };

        public BomStream(InputStream inStream) {
            this.inStream = inStream;
        }

        public int read() throws IOException {
            int c = -1;
            if (firstByte) {
                firstByte = false;
                inStream.mark(3);
                int c1, c2, c3;
                c1 = inStream.read();
                c2 = inStream.read();
                c3 = inStream.read();
                if (!(c1 == BOM[0]) || !(c2 == BOM[1]) || !(c3 == BOM[2])) {
                    inStream.reset();
                }
            }
            c = inStream.read();
            return c;
        }
    }

    private static class UTF8Properties extends java.util.Properties {

        public UTF8Properties() {
        }

        public synchronized void loadUTF8(BufferedReader bReader) throws IOException {
            char[] convtBuf = new char[1024];
            LineReader lr = new LineReader(bReader);
            int limit;
            int keyLen;
            int valueStart;
            char c;
            boolean hasSep;
            boolean precedingBackslash;
            while ((limit = lr.readLine()) >= 0) {
                c = 0;
                keyLen = 0;
                valueStart = limit;
                hasSep = false;
                precedingBackslash = false;
                while (keyLen < limit) {
                    c = lr.lineBuf[keyLen];
                    if ((c == '=' || c == ':') && !precedingBackslash) {
                        valueStart = keyLen + 1;
                        hasSep = true;
                        break;
                    } else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash) {
                        valueStart = keyLen + 1;
                        break;
                    }
                    if (c == '\\') {
                        precedingBackslash = !precedingBackslash;
                    } else {
                        precedingBackslash = false;
                    }
                    keyLen++;
                }
                while (valueStart < limit) {
                    c = lr.lineBuf[valueStart];
                    if (c != ' ' && c != '\t' && c != '\f') {
                        if (!hasSep && (c == '=' || c == ':')) {
                            hasSep = true;
                        } else {
                            break;
                        }
                    }
                    valueStart++;
                }
                String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
                String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
                put(key, value);
            }
        }

        class LineReader {

            public LineReader(BufferedReader bReader) {
                this.bReader = bReader;
            }

            BufferedReader bReader;

            InputStream iStream;

            char[] lineBuf = new char[1024];

            int readTemp() throws IOException {
                String line = bReader.readLine();
                if (line == null) return -1;
                if (line.trim().equals("") || line.trim().charAt(0) == '#') return 0; else {
                    String newLine = line.trim();
                    int len = newLine.length();
                    newLine.getChars(0, len, lineBuf, 0);
                    return len;
                }
            }

            int readLine() throws IOException {
                int len;
                while ((len = readTemp()) == 0) {
                }
                if (len == -1) return -1;
                return len;
            }
        }

        private String loadConvert(char[] in, int off, int len, char[] convtBuf) {
            if (convtBuf.length < len) {
                int newLen = len * 2;
                if (newLen < 0) {
                    newLen = Integer.MAX_VALUE;
                }
                convtBuf = new char[newLen];
            }
            char aChar;
            char[] out = convtBuf;
            int outLen = 0;
            int end = off + len;
            while (off < end) {
                aChar = in[off++];
                if (aChar == '\\') {
                    aChar = in[off++];
                    if (aChar == 'u') {
                        int value = 0;
                        for (int i = 0; i < 4; i++) {
                            aChar = in[off++];
                            switch(aChar) {
                                case '0':
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                case '8':
                                case '9':
                                    value = (value << 4) + aChar - '0';
                                    break;
                                case 'a':
                                case 'b':
                                case 'c':
                                case 'd':
                                case 'e':
                                case 'f':
                                    value = (value << 4) + 10 + aChar - 'a';
                                    break;
                                case 'A':
                                case 'B':
                                case 'C':
                                case 'D':
                                case 'E':
                                case 'F':
                                    value = (value << 4) + 10 + aChar - 'A';
                                    break;
                                default:
                                    throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                            }
                        }
                        out[outLen++] = (char) value;
                    } else {
                        if (aChar == 't') aChar = '\t'; else if (aChar == 'r') aChar = '\r'; else if (aChar == 'n') aChar = '\n'; else if (aChar == 'f') aChar = '\f';
                        out[outLen++] = aChar;
                    }
                } else {
                    out[outLen++] = (char) aChar;
                }
            }
            return new String(out, 0, outLen);
        }
    }
}
