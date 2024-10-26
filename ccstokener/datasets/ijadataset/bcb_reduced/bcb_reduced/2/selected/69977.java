package net.noderunner.exml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is useful for resolving system literals by filename or URL name.
 */
public class DefaultResolver implements SystemLiteralResolver {

    private List<String> paths;

    /**
	 * Constructs a new <code>DefaultResolver</code>.
	 * By default, attempts to load a system literal
	 * by assuming it is already a fully-qualified name.
	 *
	 * @see #addSearchPath
	 */
    public DefaultResolver() {
        paths = new ArrayList<String>();
    }

    /**
	 * Adds the specified filesystem directory or URL to 
	 * the search path.
	 * Example possible values to use:
	 * <pre>
	 * /usr/local/dtd
	 * http://example.net/dtd
	 * https://secure.example.com/dtd
	 * C:\DTD
	 * </pre>
	 */
    public void addSearchPath(String parentDirectory) {
        if (parentDirectory == null) throw new IllegalArgumentException("Cannot add null search path");
        paths.add(parentDirectory);
    }

    private Reader openURL(String fn) throws IOException {
        URL url = new URL(fn);
        URLConnection con = url.openConnection();
        con.setDoInput(true);
        return new InputStreamReader(con.getInputStream());
    }

    private Reader openLocalFile(String fn) throws IOException {
        FileInputStream fis = new FileInputStream(fn);
        InputStreamReader isr = new InputStreamReader(fis);
        return isr;
    }

    /**
	 * Resolves a literal first by URL name, then by filename, then by
	 * searching the search path.
	 * Returns a reader used to read the resolved literal.
	 */
    public Reader resolve(String systemLiteral) throws IOException {
        if (systemLiteral == null) throw new IllegalArgumentException("Cannot resolve null systemLiteral");
        String path = null;
        Iterator<String> i = paths.iterator();
        while (true) {
            try {
                String fn = systemLiteral;
                if (path != null) fn = path + '/' + systemLiteral;
                return openURL(fn);
            } catch (MalformedURLException ex) {
            } catch (IOException ioe) {
                if (!i.hasNext()) {
                    throw ioe;
                }
            }
            try {
                String fn = systemLiteral;
                if (path != null) fn = path + File.separator + systemLiteral;
                return openLocalFile(fn);
            } catch (FileNotFoundException fnf) {
                if (!i.hasNext()) {
                    throw fnf;
                }
            }
            path = i.next();
        }
    }
}
