package com.hp.hpl.jena.util;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import org.apache.commons.logging.LogFactory;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.WrappedIOException;
import com.hp.hpl.jena.JenaRuntime;

public class FileUtils {

    public static final String langXML = "RDF/XML";

    public static final String langXMLAbbrev = "RDF/XML-ABBREV";

    public static final String langNTriple = "N-TRIPLE";

    public static final String langN3 = "N3";

    public static final String langTurtle = "TURTLE";

    public static final String langBDB = "RDF/BDB";

    public static final String langSQL = "RDF/SQL";

    static Charset utf8 = null;

    static {
        try {
            utf8 = Charset.forName("utf-8");
        } catch (Throwable ex) {
            LogFactory.getLog(FileUtils.class).warn("Failed to get charset for UTF-8");
        }
    }

    /** Create a reader that uses UTF-8 encoding */
    public static Reader asUTF8(InputStream in) {
        if (JenaRuntime.runUnder(JenaRuntime.featureNoCharset)) return new InputStreamReader(in);
        return new InputStreamReader(in, utf8.newDecoder());
    }

    /** Create a buffered reader that uses UTF-8 encoding */
    public static BufferedReader asBufferedUTF8(InputStream in) {
        return new BufferedReader(asUTF8(in));
    }

    /** Create a writer that uses UTF-8 encoding */
    public static Writer asUTF8(OutputStream out) {
        if (JenaRuntime.runUnder(JenaRuntime.featureNoCharset)) return new OutputStreamWriter(out);
        return new OutputStreamWriter(out, utf8.newEncoder());
    }

    /** Create a print writer that uses UTF-8 encoding */
    public static PrintWriter asPrintWriterUTF8(OutputStream out) {
        return new PrintWriter(asUTF8(out));
    }

    /** Guess the language/type of model data. Updated by Chris, hived off the
     * model-suffix part to FileUtils as part of unifying it with similar code in FileGraph.
     * 
     * <ul>
     * <li> If the URI of the model starts jdbc: it is assumed to be an RDB model</li>
     * <li> If the URI ends ".rdf", it is assumed to be RDF/XML</li>
     * <li> If the URI end .nt, it is assumed to be N-Triples</li>
     * <li> If the URI end .bdb, it is assumed to be BerkeleyDB model [suppressed at present]</li>
     * </ul>
     * @param name    URL to base the guess on
     * @param otherwise Default guess
     * @return String   Guessed syntax - or the default supplied
     */
    public static String guessLang(String name, String otherwise) {
        if (name.startsWith("jdbc:") || name.startsWith("JDBC:")) return langSQL;
        String suffix = getFilenameExt(name);
        if (suffix.equals("n3")) return langN3;
        if (suffix.equals("nt")) return langNTriple;
        if (suffix.equals("ttl")) return langTurtle;
        if (suffix.equals("rdf")) return langXML;
        if (suffix.equals("owl")) return langXML;
        return otherwise;
    }

    /** Guess the language/type of model data
     * 
     * <ul>
     * <li> If the URI of the model starts jdbc: it is assumed to be an RDB model</li>
     * <li> If the URI ends .rdf, it is assumed to be RDF/XML</li>
     * <li> If the URI ends .n3, it is assumed to be N3</li>
     * <li> If the URI ends .nt, it is assumed to be N-Triples</li>
     * <li> If the URI ends .bdb, it is assumed to be BerkeleyDB model</li>
     * </ul>
     * @param urlStr    URL to base the guess on
     * @return String   Guessed syntax - default is RDF/XML
     */
    public static String guessLang(String urlStr) {
        return guessLang(urlStr, langXML);
    }

    /** Turn a file: URL or file name into a plain file name */
    public static String toFilename(String filenameOrURI) {
        if (!isFile(filenameOrURI)) return null;
        String fn = filenameOrURI;
        if (fn.startsWith("file:///")) {
            fn = fn.substring("file://".length());
            return fn;
        }
        if (fn.startsWith("file://localhost/")) return fn.substring("file://localhost".length());
        if (fn.startsWith("file:")) fn = fn.substring("file:".length());
        if (fn.indexOf("%") > -1) {
            try {
                fn = URLDecoder.decode(fn, "UTF-8");
            } catch (Exception ex) {
            }
        }
        return fn;
    }

    /** Check whether 'name' is possibly a file reference  
     * 
     * @param name
     * @return boolean False if clearly not a filename. 
     */
    public static boolean isFile(String name) {
        String scheme = getScheme(name);
        if (scheme == null) return true;
        if (scheme.equals("file")) return true;
        if (scheme.length() == 1) return true;
        return false;
    }

    /** Check whether a name is an absolute URI (has a scheme name)
     * 
     * @param name
     * @return boolean True if there is a scheme name 
     */
    public static boolean isURI(String name) {
        return (getScheme(name) != null);
    }

    public static String getScheme(String uri) {
        for (int i = 0; i < uri.length(); i++) {
            char ch = uri.charAt(i);
            if (ch == ':') return uri.substring(0, i);
            if (!isASCIILetter(ch)) break;
        }
        return null;
    }

    private static boolean isASCIILetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    /**
     * Get the directory part of a filename
     * @param filename
     * @return Directory name
     */
    public static String getDirname(String filename) {
        File f = new File(filename);
        return f.getParent();
    }

    /** Get the basename of a filename
     * 
     * @param filename
     * @return Base filename.
     */
    public static String getBasename(String filename) {
        File f = new File(filename);
        return f.getName();
    }

    /**
     Get the suffix part of a file name or a URL in file-like format.
     */
    public static String getFilenameExt(String filename) {
        int iSlash = filename.lastIndexOf('/');
        int iBack = filename.lastIndexOf('\\');
        int iExt = filename.lastIndexOf('.');
        if (iBack > iSlash) iSlash = iBack;
        return iExt > iSlash ? filename.substring(iExt + 1).toLowerCase() : "";
    }

    /**
     create a temporary file that will be deleted on exit, and do something
     sensible with any IO exceptions - namely, throw them up wrapped in
     a JenaException.
     
     @param prefix the prefix for File.createTempFile
     @param suffix the suffix for File.createTempFile
     @return the temporary File
     */
    public static File tempFileName(String prefix, String suffix) {
        File result = new File(getTempDirectory(), prefix + randomNumber() + suffix);
        if (result.exists()) return tempFileName(prefix, suffix);
        result.deleteOnExit();
        return result;
    }

    /**
     Answer a File naming a freshly-created directory in the temporary directory. This
     directory should be deleted on exit.
     TODO handle threading issues, mkdir failure, and better cleanup
     
     @param prefix the prefix for the directory name
     @return a File naming the new directory
     */
    public static File getScratchDirectory(String prefix) {
        File result = new File(getTempDirectory(), prefix + randomNumber());
        if (result.exists()) return getScratchDirectory(prefix);
        if (result.mkdir() == false) throw new JenaException("mkdir failed on " + result);
        result.deleteOnExit();
        return result;
    }

    public static String getTempDirectory() {
        return JenaRuntime.getSystemProperty("java.io.tmpdir");
    }

    private static int counter = 0;

    private static int randomNumber() {
        return ++counter;
    }

    /**
     Answer a BufferedReader than reads from the named resource file as
     UTF-8, possibly throwing WrappedIOExceptions.
     */
    public static BufferedReader openResourceFile(String filename) {
        try {
            InputStream is = FileUtils.openResourceFileAsStream(filename);
            return new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
    }

    /**
     * Open an resource file for reading.
     */
    public static InputStream openResourceFileAsStream(String filename) throws FileNotFoundException {
        InputStream is = ClassLoader.getSystemResourceAsStream(filename);
        if (is == null) {
            is = FileUtils.class.getResourceAsStream("/" + filename);
            if (is == null) {
                is = FileUtils.class.getResourceAsStream(filename);
                if (is == null) {
                    is = new FileInputStream(filename);
                }
            }
        }
        return is;
    }

    public static BufferedReader readerFromURL(String urlStr) {
        try {
            return asBufferedUTF8(new URL(urlStr).openStream());
        } catch (java.net.MalformedURLException e) {
            try {
                return asBufferedUTF8(new FileInputStream(urlStr));
            } catch (FileNotFoundException f) {
                throw new WrappedIOException(f);
            }
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
    }

    /** Read a whole file as UTF-8
     * @param filename
     * @return String
     * @throws IOException
     */
    public static String readWholeFileAsUTF8(String filename) throws IOException {
        InputStream in = new FileInputStream(filename);
        return readWholeFileAsUTF8(in);
    }

    /** Read a whole stream as UTF-8
     * 
     * @param in    InputStream to be read
     * @return      String
     * @throws IOException
     */
    public static String readWholeFileAsUTF8(InputStream in) throws IOException {
        Reader r = new BufferedReader(asUTF8(in), 1024);
        return readWholeFileAsUTF8(r);
    }

    private static String readWholeFileAsUTF8(Reader r) throws IOException {
        StringWriter sw = new StringWriter(1024);
        char buff[] = new char[1024];
        while (r.ready()) {
            int l = r.read(buff);
            if (l <= 0) break;
            sw.write(buff, 0, l);
        }
        r.close();
        sw.close();
        return sw.toString();
    }
}
