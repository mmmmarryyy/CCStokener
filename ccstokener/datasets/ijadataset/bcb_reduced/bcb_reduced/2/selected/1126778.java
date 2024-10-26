package uk.org.beton.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Holds a static table that associates certain file extensions with 'interesting' files.
 *
 * @author Rick Beton
 */
public final class MimeTypes {

    private static final Resources RES = new Resources("Mime.");

    private static final MimeTypes SINGLETON = new MimeTypes();

    private final Map<String, String> extnMap = new HashMap<String, String>();

    private final Set<String> canParse = new HashSet<String>();

    public static final String APPLICATION_XHTML_XML = "application/xhtml+xml";

    public static final String TEXT_HTML = "text/html";

    public static final String TEXT_CSS = "text/css";

    private MimeTypes() {
        try {
            final URL url = RES.getURL("types");
            final InputStream is = url.openStream();
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            while (line != null) {
                line = line.trim();
                final int p = line.indexOf('#');
                if (p >= 0) {
                    line = line.substring(0, p).trim();
                }
                if (line.length() > 0) {
                    final StringTokenizer st = new StringTokenizer(line, " \t");
                    if (st.countTokens() > 1) {
                        final String mime = st.nextToken();
                        while (st.hasMoreTokens()) {
                            extnMap.put(st.nextToken(), mime);
                        }
                    }
                }
                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        canParse.add(TEXT_HTML);
        canParse.add(TEXT_CSS);
    }

    public static String getType(String extn) {
        return SINGLETON.extnMap.get(extn);
    }

    public static boolean isParseableType(String extn) {
        final String mime = SINGLETON.extnMap.get(extn);
        if (mime == null) {
            return true;
        }
        final boolean can = SINGLETON.canParse.contains(mime);
        return can;
    }
}
