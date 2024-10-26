package com.trapezium.parse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.PushbackInputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 *  Provides a static public method for creating
 *  an InputStream from a String indicating a file or URL.
 *
 *  @author          Johannes N. Johannsen
 *  @version         1.0, 17 Nov 1997
 *
 *  @since           1.0
 */
public class InputStreamFactory {

    private InputStreamFactory() {
    }

    /**
	 *  Creates an InputStream from a String indicating either a file or a URL.  
	 *  First tries to construct a URL, if this fails, assume it is a file.
	 *
	 *  If using JDK 1.0.x, GZIPInputStream not available.  If a GZIP file is
	 *  encountered, we attempt to use this class, exception is caught, null
	 *  returned.
	 *
	 *  @return     InputStream of data associated with url, null if InputStream
	 *              cannot be constructed
	 *  @exception  FileNotFoundException  thrown when url cannot be created
	 */
    public static InputStream getInputStream(String fileName) throws FileNotFoundException, IOException, NoClassDefFoundError {
        try {
            URL urlAttempt = new URL(fileName);
            PushbackInputStream test = new PushbackInputStream(urlAttempt.openStream());
            int testchar = test.read();
            test.unread(testchar);
            if (testchar == 0x1f) {
                try {
                    GZIPInputStream gzi = new GZIPInputStream(test);
                    return (new BufferedInputStream(gzi));
                } catch (Exception e) {
                    String lm = e.getLocalizedMessage();
                    if ((lm != null) && (lm.compareTo("Not in GZIP format") == 0)) {
                        System.out.println("File not in GZIP format");
                    }
                }
                return (null);
            } else {
                return (new BufferedInputStream(test));
            }
        } catch (Exception e) {
            File source = new File(fileName);
            return (getInputStream(source));
        }
    }

    /** Get an InputStream from a File, handle gzipped files */
    public static InputStream getInputStream(File source) throws IOException {
        PushbackInputStream test = new PushbackInputStream(new FileInputStream(source));
        int testchar = test.read();
        test.unread(testchar);
        if (testchar == 0x1f) {
            try {
                return (new BufferedInputStream(new GZIPInputStream(test)));
            } catch (NoClassDefFoundError ex) {
                System.out.println("Cannot check gzipped file.");
                throw ex;
            } catch (Exception ee) {
                System.out.println("Cannot check gzipped file.");
                return (null);
            }
        } else {
            return (new BufferedInputStream(test));
        }
    }

    /**
	 *  Convert a string url into an InputStream
	 *
	 *  @return  InputStream associated with the url, or null if there is any unexpected error
	 *  @exception  FileNotFoundException if the url cannot be found
	 */
    public static InputStream getRawInputStream(String fileName) throws FileNotFoundException, IOException, NoClassDefFoundError {
        try {
            URL urlAttempt = new URL(fileName);
            PushbackInputStream test = new PushbackInputStream(urlAttempt.openStream());
            return (new BufferedInputStream(test));
        } catch (Exception e) {
            return (null);
        }
    }
}
