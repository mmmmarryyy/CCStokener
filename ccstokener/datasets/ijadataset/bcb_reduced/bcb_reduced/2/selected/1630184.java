package gralej.fileIO;

import gralej.controller.StreamInfo;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * A file loader service class that takes care of loading a file/url in a seperated
 * thread.
 * 
 * @author Niels
 * @version $Id: FileLoader.java 361 2011-09-12 15:34:38Z martinlaz@gmail.com $
 */
public class FileLoader extends FileLoaderBaseImpl {

    private URL url;

    private boolean threaded;

    /**
     * Seperate thread for calling the stream handler
     */
    private class BackgroundFileLoader extends Thread {

        InputStream is;

        StreamInfo meta;

        public BackgroundFileLoader(InputStream is, StreamInfo meta) {
            super();
            this.setName("BackgroundFileLoader (" + url + ")");
            this.is = is;
            this.meta = meta;
        }

        public void run() {
            notifyListeners(is, meta);
        }
    }

    /**
     * @param url
     *            the url to load
     * @param threaded
     *            if set to true, url loading happens as background action
     *            (seperated thread).
     */
    public FileLoader(URL url, boolean threaded) {
        super();
        this.url = url;
        this.threaded = threaded;
    }

    /**
     * @param file
     *            the file to load
     * @param threaded
     *            if set to true, file loading happens as background action
     *            (seperated thread).
     */
    public FileLoader(File file, boolean threaded) {
        this(file2url(file), threaded);
    }

    public static URL file2url(File f) {
        try {
            return f.toURI().toURL();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * load file (without background process/threading)
     * 
     * @param file
     *            the file to load
     */
    public FileLoader(File file) {
        this(file, false);
    }

    /**
     * Mapping function that returns a protocol/data type from the file
     * extension
     * 
     * @param f
     *            the file
     * @return
     */
    private static String extension2type(URL url) {
        String lcfilename = url.getFile().toLowerCase();
        if (lcfilename.endsWith(".grale") || lcfilename.endsWith(".grale.gz")) {
            return "grisu";
        }
        if (lcfilename.endsWith(".gralej-simple") || lcfilename.endsWith(".gralej-simple.gz")) {
            return "gralej-simple";
        }
        if (lcfilename.endsWith(".gralej") || lcfilename.endsWith(".gralej.gz")) {
            return "gralej-simple";
        }
        return "unknown";
    }

    public void loadFile() throws IOException {
        StreamInfo info = new StreamInfo(extension2type(url), url.toString());
        InputStream is = url.openStream();
        if (url.getFile().toLowerCase().endsWith(".gz")) is = new GZIPInputStream(is); else is = new BufferedInputStream(is);
        if (!threaded) {
            notifyListeners(is, info);
        } else {
            new BackgroundFileLoader(is, info).start();
        }
    }
}
