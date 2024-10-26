package nlp.lang.he.morph.erel.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

/**
 * contains static utilities for creating streams that can map either to a local
 * File or to a remote URL each utility takes an Object argument called
 * "thepath": If "thepath" is a String -- it is understood as the directory of
 * "thename" in the local file system. If "thepath" is a URL -- it is understood
 * as the context in which to parse "thename". Otherwise -- a ClassCastException
 * is thrown.
 */
public class LocalOrRemote {

    /** each opening of a local or remote file will be logged to "log_stream" */
    public static PrintStream log_stream = System.err;

    private static InputStream openInput(String thedirectory, String thename) throws FileNotFoundException {
        File thefile = new File(thedirectory, thename);
        String the_absolute_path = thefile.getAbsolutePath();
        log_stream.println("reading local file " + the_absolute_path);
        return new FileInputStream(thefile);
    }

    private static InputStream openInput(URL thecontext, String thename) throws IOException {
        if (thecontext.getFile().indexOf('/') < 0) throw new java.net.MalformedURLException(thecontext.toString());
        URL theurl = new URL(thecontext, thename);
        String the_absolute_path = theurl.toString();
        log_stream.println("reading remote file " + the_absolute_path);
        return theurl.openConnection().getInputStream();
    }

    public static InputStream getInputStream(Object thepath, String thename) throws IOException {
        if (thepath instanceof String) return openInput((String) thepath, thename); else if (thepath instanceof URL) return openInput((URL) thepath, thename); else throw new ClassCastException("the path is of class " + thepath.getClass());
    }

    public static Reader getReader(Object thepath, String thename) throws IOException {
        return new InputStreamReader(getInputStream(thepath, thename));
    }

    public static ObjectReader getObjectReader(Object thepath, String thename) throws IOException {
        return new ObjectReader(getReader(thepath, thename));
    }

    public static CollectionReader getCollectionReader(Object thepath, String thename) throws IOException {
        return new CollectionReader(getReader(thepath, thename));
    }

    private static OutputStream openOutput(String thedirectory, String thename) throws IOException {
        File thefile = new File(thedirectory, thename);
        String the_absolute_path = thefile.getAbsolutePath();
        log_stream.println("writing local file " + the_absolute_path);
        return new FileOutputStream(thefile);
    }

    private static OutputStream openOutput(URL thecontext, String thename) throws IOException {
        if (thecontext.getFile().indexOf('/') < 0) throw new java.net.MalformedURLException(thecontext.toString());
        URL theurl = new URL(thecontext, thename);
        String the_absolute_path = theurl.toString();
        log_stream.println("writing remote file " + the_absolute_path);
        return theurl.openConnection().getOutputStream();
    }

    public static OutputStream getOutputStream(Object thepath, String thename) throws IOException {
        if (thepath instanceof String) return openOutput((String) thepath, thename); else if (thepath instanceof URL) return openOutput((URL) thepath, thename); else throw new ClassCastException("the path is of class " + thepath.getClass());
    }

    public static Writer getWriter(Object thepath, String thename) throws IOException {
        return new OutputStreamWriter(getOutputStream(thepath, thename));
    }

    public static ObjectWriter getObjectWriter(Object thepath, String thename) throws IOException {
        return new ObjectWriter(getWriter(thepath, thename));
    }

    public static CollectionWriter getCollectionWriter(Object thepath, String thename) throws IOException {
        return new CollectionWriter(getWriter(thepath, thename));
    }
}
