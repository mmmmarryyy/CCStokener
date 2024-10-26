package httptest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;

/**
 *  A simple Jave program to test RSP pages when served from real web
 *  servers.
 */
public class HTTPTest {

    public static void main(String[] args) throws MalformedURLException, IOException {
        String baseurl = args[0];
        int atpos = baseurl.indexOf('@');
        int tests = 0, failed = 0;
        String[] files = new File("examples").list();
        for (int i = 0; i < files.length; i++) if (files[i].endsWith(".expected")) {
            String basename = files[i].substring(0, files[i].length() - ".expected".length());
            File expected = new File("examples", files[i]);
            String[] lines = readStream(new FileInputStream(expected));
            String url = baseurl.substring(0, atpos) + basename + baseurl.substring(atpos + 1);
            System.out.print(basename + " ... ");
            if (!(new HTTPTest(new URL(url), lines)).test()) {
                failed++;
                System.out.println("FAILED");
            } else System.out.println("OK");
            tests++;
        }
        if (failed == 0) System.out.println("SUCCESS (" + tests + " tests)"); else {
            System.out.println(failed + " FAILURES (" + tests + " tests)");
            System.exit(1);
        }
    }

    static String[] readStream(InputStream stream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        ArrayList lines = new ArrayList();
        String line;
        while ((line = in.readLine()) != null) lines.add(line);
        String[] l = (String[]) lines.toArray(new String[0]);
        if (l.length > 0 && l[l.length - 1].trim().length() == 0) {
            String[] s = new String[l.length - 1];
            System.arraycopy(l, 0, s, 0, l.length - 1);
            l = s;
        }
        return l;
    }

    private URL url;

    private String[] expected;

    public HTTPTest(URL url, String[] expected) {
        this.url = url;
        this.expected = expected;
    }

    public boolean test() {
        String[] lines = null;
        try {
            lines = readStream(url.openStream());
        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
        if (expected.length != lines.length) {
            System.err.println(url + ":\n  line count mismatch - " + lines.length + ", expected " + expected.length);
            return false;
        }
        for (int i = 0; i < expected.length; i++) if (!lines[i].equals(expected[i])) {
            System.err.println(url + ": mismatch on line " + (i + 1) + ":-");
            System.err.println("  " + expected[i]);
            System.err.println("  " + lines[i]);
            return false;
        }
        return true;
    }
}
