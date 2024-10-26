package net.sf.logdistiller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import junit.framework.TestCase;

/**
 * Base class to write custom LogEvent junit tests more easily.
 *
 * @since 1.0
 */
public class LogEventTestCase extends TestCase {

    protected final LogType logtype;

    protected LogType.Description description;

    protected LogEvent.Factory factory;

    protected LogEventTestCase(String logtype) {
        super(logtype);
        this.logtype = LogTypes.getLogType(logtype);
        this.description = this.logtype.newDescription(new HashMap<String, String>());
        this.description.setExtensions(new Attributes.Extension[0]);
    }

    /**
     * get a resource from the current testcase class and check it has been found
     *
     * @param name the resource to get
     * @return the corresponding resource, not <code>null</code>
     * @throws RuntimeException if resource not found
     * @see Class#getResource(String)
     */
    protected URL getResource(String name) {
        URL url = getClass().getResource(name);
        if (url == null) {
            throw new RuntimeException("resource '" + name + "' not found in package " + getClass().getPackage().getName());
        }
        return url;
    }

    /**
     * create a factory for a resource (read with platform encoding).
     */
    protected LogEvent.Factory newFactory(String name) throws IOException {
        URL url = getResource(name);
        String source = getClass().getPackage().getName().replace('.', '/') + '/' + name;
        return description.newFactory(new InputStreamReader(url.openStream()), source);
    }

    private static final String HEADER = "====== log event ======";

    /**
     * Dump every LogEvent found in the factory to the given PrintWriter.
     */
    public static int dump(LogEvent.Factory factory, PrintWriter out) throws IOException, ParseException {
        LogEvent le;
        int count = 0;
        while ((le = factory.nextEvent()) != null) {
            out.println(HEADER);
            le.dump(out);
            count++;
        }
        return count;
    }

    /**
     * Dump every LogEvent found in the factory to the given File using platform encoding.
     */
    public static int dump(LogEvent.Factory factory, File file) throws IOException, ParseException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(file));
            return dump(factory, out);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Read the next LogEvent dump.
     */
    public static String readDump(BufferedReader reader) throws IOException {
        StringWriter buffer = new StringWriter();
        PrintWriter out = new PrintWriter(buffer);
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        if (!HEADER.equals(line)) {
            throw new IllegalStateException("expected '" + HEADER + "' but got '" + line + "'");
        }
        while (!startsWith(reader, HEADER) && ((line = reader.readLine()) != null)) {
            out.println(line);
        }
        out.close();
        return buffer.toString();
    }

    /**
     * Checks if the reader content starts with given content, without consuming any character.
     */
    private static boolean startsWith(BufferedReader reader, String content) throws IOException {
        reader.mark(content.length());
        for (int i = 0; i < content.length(); i++) {
            if (reader.read() != content.charAt(i)) {
                reader.reset();
                return false;
            }
        }
        reader.reset();
        return true;
    }

    public void checkDump(LogEvent.Factory factory, BufferedReader reader) throws IOException, ParseException {
        LogEvent le = null;
        while ((le = factory.nextEvent()) != null) {
            String dump = readDump(reader);
            System.out.println(dump);
            assertEquals(dump, le.dump());
        }
        assertEquals("not any log event available, but dump still has content", null, readDump(reader));
    }

    public void checkDump(LogEvent.Factory factory, String name) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getResource(name).openStream()));
        checkDump(factory, reader);
    }
}
