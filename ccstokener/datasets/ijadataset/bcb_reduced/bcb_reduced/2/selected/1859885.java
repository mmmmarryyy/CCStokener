package com.wutka.jox;

import java.io.*;
import java.net.*;
import java.util.Vector;

/** Stupid parser that extracts element names from a DTD.
 *
 * @author Mark Wutka
 * @version 1.1 05/09/2000
 */
class DTDReader {

    /** The URL of the DTD */
    protected URL dtdURL;

    /** Creates a DTDReader to read a DTD from a URL */
    public DTDReader(URL aDtdURL) {
        dtdURL = aDtdURL;
    }

    /** Creates a DTDReader to read a DTD from a URL */
    public DTDReader(String aDtdURL) throws IOException {
        try {
            dtdURL = new URL(aDtdURL);
        } catch (MalformedURLException exc) {
            throw new IOException("Invalid DTD URL " + aDtdURL + ": " + exc.toString());
        }
    }

    /** Returns an array of the names of elements defined in the DTD
 * @return The elements in the DTD
 * @throws IOException If there is an error reading the DTD
 */
    public String[] getElements() throws IOException {
        Vector v = new Vector();
        PushbackInputStream in = null;
        try {
            URLConnection urlConn = dtdURL.openConnection();
            in = new PushbackInputStream(new BufferedInputStream(urlConn.getInputStream()));
            while (scanForLTBang(in)) {
                String elementType = getString(in);
                if (elementType.equals("ELEMENT")) {
                    skipWhiteSpace(in);
                    String elementName = getString(in);
                    v.addElement(elementName);
                }
            }
            in.close();
            String[] elements = new String[v.size()];
            v.copyInto(elements);
            return elements;
        } catch (Exception exc) {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
            throw new IOException("Error reading DTD: " + exc.toString());
        }
    }

    /** Searches for &lt;! in an input stream.
 * @param in The input stream to read from
 * @throws IOException If there is an error reading the stream
 */
    protected boolean scanForLTBang(PushbackInputStream in) throws IOException {
        int ch;
        while ((ch = in.read()) >= 0) {
            if (ch == '<') {
                ch = in.read();
                if (ch < 0) return false;
                if (ch == '!') {
                    return true;
                }
                if (ch == '<') {
                    in.unread((byte) ch);
                }
            }
        }
        return false;
    }

    /** Skips over any whitespace characters in the stream
 * @param in The input stream to read
 * @throws IOException If there is an error reading the stream
 */
    protected void skipWhiteSpace(PushbackInputStream in) throws IOException {
        int ch;
        while ((ch = in.read()) >= 0) {
            if (!Character.isWhitespace((char) ch)) {
                in.unread((byte) ch);
                return;
            }
        }
    }

    /** Reads a whitespace-delimited string from a stream
 * @param in The input stream to read
 * @throws IOException If there is an error reading the stream
 */
    protected String getString(PushbackInputStream in) throws IOException {
        StringBuffer str = new StringBuffer();
        int ch;
        while ((ch = in.read()) >= 0) {
            if (Character.isWhitespace((char) ch)) {
                in.unread((byte) ch);
                return str.toString();
            } else {
                str.append((char) ch);
            }
        }
        return null;
    }
}
