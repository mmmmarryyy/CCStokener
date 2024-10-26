package gov.lanl.Utility;

/**
 *  Static class to handle recurring problem of opening a file or resource wherever it may be
 *  @author $Author: dwforslund $
 *  @version $Revision: 2844 $ $Date: 2003-10-07 12:39:21 -0400 (Tue, 07 Oct 2003) $
 */
public class IOHelper {

    private static IOHelper ioh = new IOHelper();

    private static org.apache.log4j.Logger cat = org.apache.log4j.Logger.getLogger(IOHelper.class.getName());

    public IOHelper() {
    }

    /**
     * This method returns a java.io.InputStream associated with a
     * String representing a resource type.  It tries to open it as a file,
     *  as a resource, and then as a URL.  Failures throw a java.io.IOExceptoin
     *
     * @param ref a String representing a resource path or URL
     * @return a java.io.InputStream associated with that resource
     * @throws java.io.IOException
     */
    public static java.io.InputStream getInputStream(String ref) throws java.io.IOException {
        if (ref == null || ref.length() == 0) {
            throw new java.net.MalformedURLException("resource null or empty");
        } else if (ref.length() > 10 && ref.substring(0, 9).toUpperCase().equals("RESOURCE:")) {
            String resource = ref.substring(9, ref.length());
            try {
                return ClassLoader.getSystemResourceAsStream(resource);
            } catch (Exception ex) {
                throw new java.io.IOException(resource);
            }
        } else {
            java.io.File f = new java.io.File(ref);
            if (f.isFile() && f.canRead()) {
                try {
                    return new java.io.FileInputStream(f);
                } catch (java.io.IOException ioe) {
                    throw ioe;
                }
            }
            java.io.InputStream is = ioh.getClass().getResourceAsStream(ref);
            if (is != null) return is;
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(ref);
            if (is != null) return is;
            try {
                java.net.URL url = new java.net.URL(ref);
                try {
                    return url.openStream();
                } catch (java.io.IOException ioe) {
                    throw ioe;
                }
            } catch (java.net.MalformedURLException mue) {
                throw mue;
            }
        }
    }

    /**
     *  Main test program for reading in a stream resource
     */
    public static void main(String[] arg) {
        if (arg.length > 0) {
            try {
                java.io.InputStream is = getInputStream(arg[0]);
                System.out.println("read: " + is.read() + " bytes");
            } catch (java.io.IOException ioe) {
                cat.error(ioe, ioe);
            }
        } else cat.warn("No input arguments");
    }
}
