package org.hl7.types.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashSet;
import java.util.Set;
import org.hl7.types.ANY;
import org.hl7.types.BL;
import org.hl7.types.CS;
import org.hl7.types.ST;

/**
 * Adapter for java.net.URL to the org.hl7.types.URL interface.
 * 
 * FIXME: There are a couple of quirks:
 *  - the j.n.URL has no way of getting at an opaque part (i.e. everything after the scheme, and I'm not sure we can
 * reassemble that from the URL accessors.
 *  - given that it's so hard to get at the opaque part, the equals implementation may not be ideal.
 */
public class URLjnURLAdapter extends ANYimpl implements org.hl7.types.URL {

    java.net.URL _data;

    private static final Set<String> KNOWN_PROTOCOLS = new HashSet<String>();

    static {
        KNOWN_PROTOCOLS.add("tel");
        KNOWN_PROTOCOLS.add("fax");
        KNOWN_PROTOCOLS.add("modem");
        KNOWN_PROTOCOLS.add("mllp");
        KNOWN_PROTOCOLS.add("nfs");
        KNOWN_PROTOCOLS.add("telnet");
    }

    private static final URLStreamHandler FAKE_STREAM_HANDLER = new URLStreamHandler() {

        protected URLConnection openConnection(URL url) throws IOException {
            throw new UnsupportedOperationException("Method not implemented.");
        }
    };

    private static final java.net.URL url(String url) throws MalformedURLException {
        int endProtocol = url.indexOf(':');
        String protocol = url.substring(0, endProtocol);
        if (KNOWN_PROTOCOLS.contains(protocol)) {
            int endProtocolSep = endProtocol + 1;
            while (url.length() >= endProtocolSep && url.charAt(endProtocolSep) == '/') {
                endProtocolSep++;
            }
            String afterProtocol = url.substring(endProtocolSep);
            java.net.URL javaUrl = new URL(protocol, "", -1, afterProtocol, FAKE_STREAM_HANDLER);
            return javaUrl;
        } else {
            java.net.URL javaUrl = new java.net.URL(url.toString());
            return javaUrl;
        }
    }

    protected URLjnURLAdapter(java.net.URL data) {
        super(null);
        if (data == null) throw new IllegalArgumentException(); else this._data = data;
    }

    protected URLjnURLAdapter(org.hl7.types.URL url) {
        super(null);
        if (url instanceof URLjnURLAdapter) _data = ((URLjnURLAdapter) url)._data; else {
            try {
                _data = new java.net.URL(url.toString());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    protected URLjnURLAdapter(String data) {
        super(null);
        try {
            if (data == null) throw new IllegalArgumentException(); else this._data = url(data);
        } catch (java.net.MalformedURLException x) {
            throw new IllegalArgumentException(x.getMessage());
        }
    }

    public static org.hl7.types.URL valueOf(java.net.URL data) {
        if (data == null) return URLnull.NI; else return new URLjnURLAdapter(data);
    }

    java.net.URL tojnURL() {
        return _data;
    }

    public final BL equal(ANY that) {
        if (that instanceof org.hl7.types.URL) {
            if (that instanceof URLjnURLAdapter) {
                return BLimpl.valueOf(this._data.equals(((URLjnURLAdapter) that)._data));
            } else {
                org.hl7.types.URL thatURL = (org.hl7.types.URL) that;
                return this.scheme().equal(thatURL.scheme()).and(this.address().equal(thatURL.address()));
            }
        } else return BLimpl.FALSE;
    }

    public CS scheme() {
        if (this.isNullJ() && _data == null) return CSnull.NA; else {
            return CSimpl.valueOf(_data.getProtocol(), "2.16.840.1.113883.5.143");
        }
    }

    public ST address() {
        if (this.isNullJ() && _data == null) return STnull.NA; else {
            String urlstring = _data.toString();
            String scheme = _data.getProtocol();
            return STjlStringAdapter.valueOf(urlstring.substring(scheme.length() + 1));
        }
    }

    public String toString() {
        return _data.toString();
    }

    /**
     * An eclipse code generator is used to add an Externalizable 
     * implementation to the org.hl7 types.  Normally this implementation is
     * not checked into SVN.  However, the code generated for this class
     * did not work and had to be replaced with a hand-coded implementation.
     * 
     * @author jmoore
     *
     * @hand-coded 
     */
    public void readExternal(ObjectInput oi) throws IOException {
        super.readExternal(oi);
        try {
            String url = oi.readUTF();
            _data = url(url);
        } catch (Throwable t) {
            IOException ioe = new IOException(t.getMessage());
            ioe.initCause(t);
            throw ioe;
        }
    }

    /**
     * An eclipse code generator is used to add an Externalizable 
     * implementation to the org.hl7 types.  Normally this implementation is
     * not checked into SVN.  However, the code generated for this class
     * did not work and had to be replaced with a hand-coded implementation.
     *
     * @author jmoore
     * 
     * @hand-coded
     */
    public void writeExternal(ObjectOutput oo) throws IOException {
        super.writeExternal(oo);
        try {
            String url = _data.toExternalForm();
            oo.writeUTF(url);
        } catch (Throwable t) {
            t.printStackTrace();
            IOException ioe = new IOException(t.getMessage());
            ioe.initCause(t);
            throw ioe;
        }
    }
}
