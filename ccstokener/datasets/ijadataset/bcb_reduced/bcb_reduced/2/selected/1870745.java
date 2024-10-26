package org.jnetpcap.util.resolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import org.jnetpcap.packet.format.JFormatter;
import org.jnetpcap.util.JLogger;
import org.jnetpcap.util.config.JConfig;

/**
 * A resolver that resolves the first 3 bytes of a MAC address to a manufacturer
 * code. The resolver loads jNetPcap supplied compressed oui database of
 * manufacturer codes and caches that information. The resolver can also
 * download over the internet, if requested, a raw IEEE OUI database of
 * manufacturer code, parse it and produce a cache file for future use.
 * 
 * @author Mark Bednarczyk
 * @author Sly Technologies, Inc.
 */
public class IEEEOuiPrefixResolver extends AbstractResolver {

    /**
	 * Default URI path to IEEE raw oui database of manufacturer codes. The URI is
	 * {@value #IEEE_OUI_DATABASE_PATH}.
	 */
    public static final String IEEE_OUI_DATABASE_PATH = "http://standards.ieee.org/regauth/oui/oui.txt";

    private static final String RESOURCE_COMPRESSED_OUI_DATABASE = "oui.txt";

    private static final String PROPERTY_OUI_DB_URL = "resolver.OUI_PREFIX.db.url";

    private static final String PROPERTY_OUI_DB_DOWNLOAD = "resolver.OUI_PREFIX.db.download";

    private static final String DEFAULT_OUI_DB_DOWNLOAD = "false";

    private boolean initialized = false;

    /**
	 * Creates an uninitalized Oui prefix resolver. The resolver is "late"
	 * initialized when its first called on to do work.
	 * 
	 * @param type
	 */
    public IEEEOuiPrefixResolver() {
        super(JLogger.getLogger(IEEEOuiPrefixResolver.class), "OUI_PREFIX");
    }

    /**
	 * Initializes the resolver by first checking if there are any cached entries,
	 * if none, it reads the compressed oui database supplied with jNetPcap in the
	 * resource directory {@value #RESOURCE_COMPRESSED_OUI_DATABASE}.
	 */
    @Override
    public void initializeIfNeeded() {
        if (initialized == false && hasCacheFile() == false) {
            initialized = true;
            setCacheCapacity(13000);
            super.initializeIfNeeded();
            setPositiveTimeout(INFINITE_TIMEOUT);
            setNegativeTimeout(0);
            try {
                URL url = JConfig.getResourceURL(RESOURCE_COMPRESSED_OUI_DATABASE);
                if (url != null) {
                    logger.fine("loading compressed database file from " + url.toString());
                    readOuisFromCompressedIEEEDb(RESOURCE_COMPRESSED_OUI_DATABASE);
                    return;
                }
                boolean download = Boolean.parseBoolean(JConfig.getProperty(PROPERTY_OUI_DB_DOWNLOAD, DEFAULT_OUI_DB_DOWNLOAD));
                String u = JConfig.getProperty(PROPERTY_OUI_DB_URL);
                if (u != null && download) {
                    url = new URL(u);
                    logger.fine("loading remote database " + url.toString());
                    loadCache(url);
                    return;
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "error while reading database", e);
            }
        } else {
            super.initializeIfNeeded();
        }
    }

    /**
	 * Download IEEE supplied OUI.txt database of manufacturer prefixes and codes.
	 * The file is downloaded using the protocol specified in the URL, parsed and
	 * cached indefinately. The machine making the URL connection must have
	 * internet connection available as well as neccessary security permissions
	 * form JRE in order to make the connection.
	 * <p>
	 * 
	 * @param url
	 *          The url of the IEEE resource to load. If the url is null, the
	 *          default uri is attempted {@value #IEEE_OUI_DATABASE_PATH}.
	 * @return number of entries cached
	 * @exception IOException
	 *              any IO errors
	 */
    @Override
    public int loadCache(URL url) throws IOException {
        if (url == null) {
            url = new URL(IEEE_OUI_DATABASE_PATH);
        }
        return readOuisFromRawIEEEDb(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    private int readOuisFromCompressedIEEEDb(BufferedReader in) throws IOException {
        int count = 0;
        try {
            String s;
            while ((s = in.readLine()) != null) {
                String[] c = s.split(":", 2);
                if (c.length < 2) {
                    continue;
                }
                Long i = Long.parseLong(c[0], 16);
                super.addToCache(i, c[1]);
                count++;
            }
        } finally {
            in.close();
        }
        return count;
    }

    private boolean readOuisFromCompressedIEEEDb(String f) throws FileNotFoundException, IOException {
        File file = new File(f);
        if (file.canRead()) {
            readOuisFromCompressedIEEEDb(new BufferedReader(new FileReader(file)));
            return true;
        }
        InputStream in = JFormatter.class.getClassLoader().getResourceAsStream("resources/" + f);
        if (in == null) {
            return false;
        }
        readOuisFromCompressedIEEEDb(new BufferedReader(new InputStreamReader(in)));
        return true;
    }

    private int readOuisFromRawIEEEDb(BufferedReader in) throws IOException {
        int count = 0;
        try {
            String s;
            while ((s = in.readLine()) != null) {
                if (s.contains("(base 16)")) {
                    String[] c = s.split("\t\t");
                    if (c.length < 2) {
                        continue;
                    }
                    String id = c[0].split(" ")[0];
                    long i = Long.parseLong(id, 16);
                    String[] a = c[1].split(" ");
                    if (a.length > 1) {
                        String p = a[0];
                        if (p.length() <= 3 || p.length() == 2 && p.charAt(1) == '.') {
                            p += a[1];
                        }
                        p = p.replace('.', '_');
                        p = p.replace('-', '_');
                        p = p.replace('\t', ' ').trim();
                        p = p.replace(',', ' ').trim();
                        if (p.endsWith("_") || p.endsWith("-")) {
                            p = p.substring(0, p.length() - 1);
                        }
                        p = transform(p, a);
                        super.addToCache(i, p);
                        count++;
                    }
                }
            }
        } finally {
            in.close();
        }
        return count;
    }

    private String transform(String str, String[] a) {
        int i = 1;
        while (true) {
            String more = (a.length > 1) ? a[i] : null;
            String after = transform(str, more);
            if (after == str) {
                break;
            }
            str = after;
        }
        return str;
    }

    private String transform(String str, String more) {
        str = transform(str, more, "Graphic", "Graph");
        str = transform(str, more, "Electronic", "Elect");
        str = transform(str, more, "Application", "App");
        str = transform(str, more, "Incorporated", "Inc");
        str = transform(str, more, "Corporation", "Corp");
        str = transform(str, more, "Company", "Co");
        str = transform(str, more, "Technologies", "Tech");
        str = transform(str, more, "Technology", "Tech");
        str = transform(str, more, "Communication", "Com");
        str = transform(str, more, "Network", "Net");
        str = transform(str, more, "System", "Sys");
        str = transform(str, more, "Information", "Info");
        str = transform(str, more, "Industries", "Ind");
        str = transform(str, more, "Industrial", "Ind");
        str = transform(str, more, "Industry", "Ind");
        str = transform(str, more, "Laboratories", "Lab");
        str = transform(str, more, "Laboratory", "Ind");
        str = transform(str, more, "Enterprises", "Ent");
        str = transform(str, more, "Computer", "Cp");
        str = transform(str, more, "Manufacturing", "Mfg");
        str = transform(str, more, "Resources", "Res");
        str = transform(str, more, "Resource", "Res");
        str = transform(str, more, "Limited", "Ltd");
        str = transform(str, more, "International", "Int");
        str = transform(str, more, "Presentation", "Pres");
        str = transform(str, more, "Equipment", "Eq");
        str = transform(str, more, "Peripheral", "Pr");
        str = transform(str, more, "Interactive", "Int");
        return str;
    }

    /**
	 * Transform any reference to specific terms with abbrieviations. The method
	 * also makes the sigular form plural and checks both lower and upper case
	 * versions.
	 * 
	 * @param str
	 *          string to be transformed
	 * @param singular
	 *          term to look for in sigular form
	 * @param abbr
	 *          abbreviation to substitute in place
	 * @return new string
	 */
    private String transform(String str, String more, final String singular, final String abbr) {
        final String plural = singular + "s";
        str = str.replace(plural.toUpperCase(), abbr);
        str = str.replace(plural.toLowerCase(), abbr);
        str = str.replace(plural, abbr);
        str = str.replace(singular.toUpperCase(), abbr);
        str = str.replace(singular.toLowerCase(), abbr);
        str = str.replace(singular, abbr);
        if (str.equals(abbr)) {
            if (more != null) {
                str += more;
            }
        }
        return str;
    }

    /**
	 * Resolves the supplied address to a human readable name.
	 * 
	 * @return resolved name or null if not resolved
	 */
    @Override
    public String resolveToName(byte[] address, long hash) {
        return null;
    }

    /**
	 * Generates a special hashcode for first 3 bytes of the address that is
	 * unique for every address.
	 */
    @Override
    public long toHashCode(byte[] address) {
        return ((address[2] < 0) ? address[2] + 256 : address[2]) | ((address[1] < 0) ? address[1] + 256 : address[1]) << 8 | ((address[0] < 0) ? address[0] + 256 : address[0]) << 16;
    }

    @Override
    protected String resolveToName(long number, long hash) {
        throw new UnsupportedOperationException("this resolver only resolves addresses in byte[] form");
    }
}
