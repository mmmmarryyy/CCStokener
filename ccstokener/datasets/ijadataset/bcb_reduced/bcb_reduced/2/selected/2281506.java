package org.eiichiro.jazzmaster.examples.petstore.ui.popup;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author basler
 */
public class PopupUtil {

    private static boolean bDebug = false;

    /** Creates a new instance of PopupUtil */
    public PopupUtil() {
    }

    /**
     * Performs Substitution on passed in strings.  This method could also use a cache to loop up strings previously used
     *
     * @param sxString String to parse
     * @param hmSub A Hashmap of of keys/variables to change
     * @param bLookup Whether to see if the value is to be looked up in the cache.  If true the sxString is used as a key
     */
    public static String parseString(String sxString, HashMap hmSub, boolean bLookup) {
        int iPos = 0;
        String sxStringx = "";
        if (bLookup) {
        } else {
            sxStringx = sxString;
        }
        if (sxStringx != null) {
            Iterator iter = hmSub.keySet().iterator();
            String sxKey, sxValue;
            while (iter.hasNext()) {
                sxKey = (String) iter.next();
                sxValue = (String) hmSub.get(sxKey);
                sxStringx = sxStringx.replaceAll(sxKey, sxValue);
            }
        }
        return sxStringx;
    }

    public static String readInFragmentAsString(String sxURL, String resource) throws IOException {
        sxURL = getResourceURL(sxURL, resource);
        return readInFragmentAsString(new URL(sxURL));
    }

    public static String readInFragmentAsString(URL sxURL) throws IOException {
        BufferedReader bfReader = null;
        StringBuffer sxOut = new StringBuffer();
        try {
            bfReader = new BufferedReader(new InputStreamReader(openStream(sxURL)));
            int byteCnt = 0;
            char[] buffer = new char[4096];
            while ((byteCnt = bfReader.read(buffer)) != -1) {
                if (byteCnt > 0) {
                    sxOut.append(buffer, 0, byteCnt);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (bfReader != null) {
                try {
                    bfReader.close();
                } catch (Exception ee) {
                }
            }
        }
        return sxOut.toString();
    }

    /**
     * The URL looks like a request for a resource, such as a JavaScript or CSS file. Write
     * the given resource to the response output in binary format (needed for images).
     */
    public static void readWriteBinaryUtil(URL sxURL, OutputStream outStream) throws IOException {
        DataOutputStream outData = null;
        DataInputStream inData = null;
        int byteCnt = 0;
        byte[] buffer = new byte[4096];
        if (bDebug) System.out.println("RW Loading - " + sxURL);
        try {
            outData = new DataOutputStream(outStream);
            inData = new DataInputStream(openStream(sxURL));
            while ((byteCnt = inData.read(buffer)) != -1) {
                if (outData != null && byteCnt > 0) {
                    outData.write(buffer, 0, byteCnt);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (inData != null) {
                    inData.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * The URL looks like a request for a resource, such as a JavaScript or CSS file. Write
     * the given resource to the response output in binary format (needed for images).
     */
    public static void readWriteBinaryUtil(String sxURL, String resource, OutputStream outStream) throws IOException {
        sxURL = getResourceURL(sxURL, resource);
        readWriteBinaryUtil(new URL(sxURL), outStream);
    }

    /**
     * The URL looks like a request for a resource, such as a JavaScript or CSS file. Write
     * the given resource to the response writer in "char" format.
     */
    public static void readWriteCharUtil(URL sxURL, Writer writer) throws IOException {
        BufferedReader bfReader = null;
        int byteCnt = 0;
        char[] buffer = new char[4096];
        if (bDebug) System.out.println("RW Base Directory - " + sxURL);
        if (bDebug) System.out.println("RW Loading - " + sxURL);
        try {
            bfReader = new BufferedReader(new InputStreamReader(openStream(sxURL)));
            while ((byteCnt = bfReader.read(buffer)) != -1) {
                if (writer != null && byteCnt > 0) {
                    writer.write(buffer, 0, byteCnt);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (bfReader != null) {
                    bfReader.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * The URL looks like a request for a resource, such as a JavaScript or CSS file. Write
     * the given resource to the response writer in "char" format.
     */
    public static void readWriteCharUtil(String sxURL, String resource, Writer writer) throws IOException {
        sxURL = getResourceURL(sxURL, resource);
        readWriteCharUtil(new URL(sxURL), writer);
    }

    public static String getResourceURL(String sxURL, String resource) {
        return sxURL.substring(0, sxURL.lastIndexOf("/")) + resource;
    }

    private static InputStream openStream(URL sxURL) throws IOException {
        InputStream isx = null;
        if (sxURL != null) {
            URLConnection urlConn = sxURL.openConnection();
            urlConn.setUseCaches(false);
            isx = urlConn.getInputStream();
        }
        return isx;
    }

    private static InputStream openStream(String sxURL) throws IOException {
        return openStream(new URL(sxURL));
    }
}
