package org.rhinojetty;

import org.mozilla.javascript.*;
import com.syntelos.iou.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Url client for connecting to remote hosts using HTTP and FTP.
 * Wraps the whole <tt>java.net</tt> URL/URLConnection thing.  See
 * `socket' for a raw socket with duplex communications.  This class
 * is primarily for fetching web content objects.
 *
 * <p> Usage is exemplified by setting an HTTP URL and then reading,
 * or writing followed by reading.
 *
 * <p> Note that writing cannot preceed reading: this is not a defined
 * process in the HTTP context.  The process of writing and then
 * reading implies an HTTP POST operation.
 *
 * <p> This scriptable is defined via `reply'.
 *
 * @author John Pritchard */
public class urlc extends uio implements Cloneable {

    public static class QueryObject extends strmap {

        private boolean active = true;

        private urlc parent;

        public QueryObject(urlc u) {
            super(u);
            this.parent = u;
            activate();
        }

        public String getClassName() {
            return "query";
        }

        public boolean active() {
            return this.active;
        }

        public void activate() {
            this.active = true;
            if (0 < super.size()) super.clear();
            URL u = this.parent.url;
            if (null != u) {
                String qs = u.getQuery();
                if (null != qs) extractQuery(qs, this);
            }
        }

        public void deactivate() {
            this.active = false;
            super.clear();
        }

        public String queryString() {
            chbuf cb = new chbuf();
            Enumeration keys = super.keys();
            String name;
            Object value;
            boolean tag = true;
            while (keys.hasMoreElements()) {
                name = (String) keys.nextElement();
                value = super.get(name);
                if (tag) tag = false; else cb.append('&');
                if (null != value) {
                    cb.append(urlEncode(name));
                    cb.append('=');
                    if (value instanceof String) cb.append(urlEncode((String) value)); else cb.append(urlEncode(ScriptRuntime.toString(value)));
                } else {
                    cb.append(urlEncode(name));
                    cb.append('=');
                }
            }
            if (0 < cb.length()) return cb.toString(); else return "";
        }
    }

    /**
     * Closing the stream closes the connection.
     */
    public static class UrlcInputStream extends DataInputStream {

        private urlc uu;

        public UrlcInputStream(urlc u, InputStream in) {
            super(in);
            this.uu = u;
        }

        public void close() throws IOException {
            uu.close();
        }

        public InputStream getIn() {
            return in;
        }
    }

    /**
     * Closing the stream closes the connection.
     */
    public static class UrlcOutputStream extends FilterOutputStream {

        private urlc uu;

        public UrlcOutputStream(urlc u, OutputStream in) {
            super(in);
            this.uu = u;
        }

        public void close() throws IOException {
            uu.close();
        }

        public OutputStream getOut() {
            return out;
        }
    }

    /**
     * Scriptable object constructor function.
     */
    public static Scriptable jsConstructor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) throws JavaScriptException {
        urlc uu = new urlc();
        Scriptable scope = (Scriptable) ((null != ctorObj) ? (ctorObj.getParentScope()) : (null));
        if (null != scope) {
            uu.setParentScope(scope);
        }
        if (null != args && 0 < args.length) {
            Object o = args[0];
            if (o instanceof Scriptable) {
                Scriptable so = (Scriptable) o;
                Object o0 = so.get("url", so);
                if (null != o0) {
                    uu.jsSet_url(ScriptRuntime.toString(o0));
                    return uu;
                } else return uu;
            } else uu.jsSet_url(ScriptRuntime.toString(args[0]));
        }
        return uu;
    }

    /**
     * Replace path in original with argument path, maintaining any
     * <tt>";"</tt> session ID.
     * 
     * <p> This function drops any query string since it's already been
     * consumed in script usage -- as this gets used from
     * <tt>`jsSet_path()'</tt>.
     */
    private static final URL constructPath(URL original, String path) {
        String u = original.toString();
        int ai = u.indexOf("//");
        int bi = u.indexOf('/', ai + 1);
        int qi = u.lastIndexOf('?');
        int si = u.lastIndexOf(';');
        boolean pa = ('/' == path.charAt(0));
        if (0 < ai && ai < bi) {
            if (0 < qi && 0 < si) {
                if (si < qi) {
                    if (pa) u = chbuf.cat(u.substring(0, bi), path, u.substring(si, qi)); else u = chbuf.cat(u.substring(0, bi + 1), path, u.substring(si, qi));
                    try {
                        return new URL(u);
                    } catch (MalformedURLException malurl) {
                        throw new IllegalStateException("Cannot construct URL \"" + u + "\".");
                    }
                } else {
                    if (pa) u = chbuf.cat(u.substring(0, bi), path, u.substring(si)); else u = chbuf.cat(u.substring(0, bi + 1), path, u.substring(si));
                    try {
                        return new URL(u);
                    } catch (MalformedURLException malurl) {
                        throw new IllegalStateException("Cannot construct URL \"" + u + "\".");
                    }
                }
            } else if (0 < si) {
                if (pa) u = chbuf.cat(u.substring(0, bi), path, u.substring(si)); else u = chbuf.cat(u.substring(0, bi + 1), path, u.substring(si));
                try {
                    return new URL(u);
                } catch (MalformedURLException malurl) {
                    throw new IllegalStateException("Cannot construct URL \"" + u + "\".");
                }
            } else {
                if (pa) u = chbuf.cat(u.substring(0, bi), path); else u = chbuf.cat(u.substring(0, bi + 1), path);
                try {
                    return new URL(u);
                } catch (MalformedURLException malurl) {
                    throw new IllegalStateException("Cannot construct URL \"" + u + "\".");
                }
            }
        } else if (0 < bi) {
            if (pa) u = chbuf.cat(u.substring(0, bi), path); else u = chbuf.cat(u.substring(0, bi + 1), path);
            try {
                return new URL(u);
            } catch (MalformedURLException malurl) {
                throw new IllegalStateException("Cannot construct URL \"" + u + "\".");
            }
        } else {
            int ci = u.indexOf(':');
            if (0 < ci) {
                u = chbuf.cat(u.substring(0, ci), path);
                try {
                    return new URL(u);
                } catch (MalformedURLException malurl) {
                    throw new IllegalStateException("Cannot construct URL \"" + u + "\".");
                }
            } else throw new IllegalStateException("BBBUGGGG!  Unrecognizable URL \"" + u + "\".");
        }
    }

    private static final URL constructQuery(URL original, QueryObject query) {
        String u = original.toString();
        int qi = u.lastIndexOf('?');
        int si = u.lastIndexOf(';');
        if (0 < qi && 0 < si) {
            if (si > qi) {
                String head = u.substring(0, qi + 1);
                String tail = u.substring(si);
                u = head + query.queryString() + tail;
                try {
                    return new URL(u);
                } catch (MalformedURLException malurl) {
                    try {
                        return new URL(original.toString() + '?' + query.queryString());
                    } catch (MalformedURLException malurl2) {
                        throw new IllegalStateException("Cannot construct URL \"" + u + "\".");
                    }
                }
            } else {
                u = u.substring(0, qi + 1) + query.queryString();
                try {
                    return new URL(u);
                } catch (MalformedURLException malurl) {
                    try {
                        return new URL(original.toString() + '?' + query.queryString());
                    } catch (MalformedURLException malurl2) {
                        throw new IllegalStateException("Cannot construct URL \"" + u + "\".");
                    }
                }
            }
        } else if (0 < qi) {
            u = u.substring(0, qi + 1) + query.queryString();
            try {
                return new URL(u);
            } catch (MalformedURLException malurl) {
                try {
                    return new URL(original.toString() + '?' + query.queryString());
                } catch (MalformedURLException malurl2) {
                    throw new IllegalStateException("Cannot construct URL \"" + u + "\".");
                }
            }
        } else try {
            return new URL(u + '?' + query.queryString());
        } catch (MalformedURLException malurl) {
            throw new IllegalStateException("Cannot construct URL \"" + u + '?' + query.queryString() + "\".");
        }
    }

    private URL url = null;

    private URLConnection _uu = null;

    private OutputStream _uu_out = null;

    private String encoding = null;

    private int bytesize = 0;

    private String mimetype = null;

    private long serviceDate = 0;

    private long contentExpires = 0;

    private long contentLastmod = 0;

    private QueryObject query = null;

    public urlc() {
        super();
    }

    public urlc(String url) throws MalformedURLException {
        super();
        this.url = new URL(url);
    }

    public urlc(URL u) {
        super();
        this.url = u;
    }

    public urlc cloneUrlc() {
        try {
            urlc uu = (urlc) super.clone();
            uu._uu = null;
            uu._uu_out = null;
            uu._din = null;
            uu._ps = null;
            return uu;
        } catch (CloneNotSupportedException cnx) {
            return null;
        }
    }

    /**
     * Open connection as for input if its hasn't been opened before.
     * Read and make all headers available.
     * 
     * @returns True if headers' info is available.
     */
    public boolean open() {
        if (null == _uu) {
            try {
                if (null == url) return false; else {
                    if (null != this.query) this.url = constructQuery(url, this.query);
                    _uu = url.openConnection();
                    _uu.setAllowUserInteraction(false);
                    _uu.setDoInput(true);
                    if (null != super._loc) {
                        try {
                            _uu.setRequestProperty("Accept-Language", jsGet_locale());
                        } catch (JavaScriptException jsx) {
                        }
                    }
                    encoding = _uu.getContentEncoding();
                    bytesize = _uu.getContentLength();
                    mimetype = _uu.getContentType();
                    serviceDate = _uu.getDate();
                    contentExpires = _uu.getExpiration();
                    contentLastmod = _uu.getLastModified();
                    return true;
                }
            } catch (Exception exc) {
                close();
                return false;
            }
        } else return true;
    }

    public boolean isOpen() {
        return (null != _uu);
    }

    public Object fetch() throws IOException {
        if (open()) return _uu.getContent(); else return null;
    }

    /**
     * Never throws any exception, called from `finalize'.
     */
    public boolean close() {
        int errors = 0;
        _ps = null;
        if (null != _uu_out) {
            try {
                _uu_out = ((UrlcOutputStream) _uu_out).getOut();
                _uu_out.flush();
            } catch (Throwable t) {
            } finally {
                try {
                    _uu_out.close();
                } catch (Throwable t) {
                    errors += 1;
                } finally {
                    _uu_out = null;
                }
            }
        }
        if (null != _din) {
            try {
                ((UrlcInputStream) _din).getIn().close();
            } catch (Throwable t) {
                errors += 1;
            } finally {
                _din = null;
            }
        }
        _uu = null;
        if (0 < errors) return false; else return true;
    }

    public DataInputStream getInputStream() {
        if (open()) {
            if (null == _din) {
                try {
                    _din = new UrlcInputStream(this, _uu.getInputStream());
                    return _din;
                } catch (IOException iox) {
                    return null;
                }
            } else return _din;
        } else return null;
    }

    /**
     * Open as for output, or return print stream.
     */
    public PrintStream getPrintStream() {
        if (null != _ps) return _ps; else {
            if (null == url) return null; else if (null == _uu) {
                try {
                    _uu = url.openConnection();
                    _uu.setAllowUserInteraction(false);
                    _uu.setDoOutput(true);
                    try {
                        if (null != super._loc) _uu.setRequestProperty("Accept-Language", jsGet_locale());
                    } catch (JavaScriptException jsx) {
                    }
                } catch (IOException iox) {
                    return null;
                }
            }
            if (null != _din) throw new IllegalStateException("A URL transaction is not valid for output following input.  Please close the connection, then do output."); else if (null == _uu_out) {
                try {
                    _uu_out = new UrlcOutputStream(this, _uu.getOutputStream());
                } catch (IOException iox) {
                    return null;
                }
            }
            if (null == _ps) _ps = new PrintStream(_uu_out);
            return _ps;
        }
    }

    /**
     * Open as for output, or return print stream.
     */
    public OutputStream getOutputStream() {
        if (null != _uu_out) return _uu_out; else {
            if (null == url) return null; else if (null == _uu) {
                try {
                    _uu = url.openConnection();
                    _uu.setAllowUserInteraction(false);
                    _uu.setDoOutput(true);
                    try {
                        if (null != super._loc) _uu.setRequestProperty("Accept-Language", jsGet_locale());
                    } catch (JavaScriptException jsx) {
                    }
                } catch (IOException iox) {
                    return null;
                }
            }
            if (null != _din) throw new IllegalStateException("A URL transaction is not valid for output following input.  Please close the connection, then do output."); else if (null == _uu_out) {
                try {
                    _uu_out = new UrlcOutputStream(this, _uu.getOutputStream());
                } catch (IOException iox) {
                    return null;
                }
            }
            return _uu_out;
        }
    }

    public URL url() {
        return url;
    }

    /**
     * Absolute path for use from `file' is the entire url.
     * 
     * <p> This is not the JS <tt>"urlc.path()"</tt> function!
     */
    public final String path() {
        if (null != url) return url.toString(); else return null;
    }

    public boolean isDirectory() {
        String path = path();
        if (null == path) return false; else return ('/' == path.charAt(path.length() - 1));
    }

    public String name() {
        if (null == url) return null; else {
            String path = url.getPath();
            if (null == path) return null; else {
                int strlen = path.length();
                if (1 > strlen) return null; else {
                    int idx = path.lastIndexOf('/');
                    if (0 > idx) return path; else return path.substring(idx + 1);
                }
            }
        }
    }

    public final String parent() {
        if (null == url) return null; else {
            String path = url.getPath();
            int strlen = path.length();
            if (1 > strlen) return url.toString(); else {
                int idx = path.lastIndexOf('/');
                if (0 > idx) return url.toString(); else {
                    path = path.substring(0, idx);
                    idx = path.lastIndexOf('/');
                    if (0 > idx) return url.toString(); else {
                        try {
                            path = path.substring(0, idx);
                            URL u = new URL(url, path);
                            return u.toString();
                        } catch (Exception exc) {
                            return url.toString();
                        }
                    }
                }
            }
        }
    }

    public final boolean exists() {
        if (isOpen()) return true; else {
            try {
                return open();
            } finally {
                close();
            }
        }
    }

    public final boolean canRead() {
        if (isOpen()) return true; else {
            try {
                return open();
            } finally {
                close();
            }
        }
    }

    public final boolean canWrite() {
        return false;
    }

    public final boolean delete() {
        close();
        return false;
    }

    public long lastModified() {
        if (isOpen()) return contentLastmod; else {
            try {
                if (open()) return contentLastmod; else return 0;
            } finally {
                close();
            }
        }
    }

    public final long length() {
        if (isOpen()) return bytesize; else {
            try {
                if (open()) return bytesize; else return 0;
            } finally {
                close();
            }
        }
    }

    public final int send() throws JavaScriptException {
        InputStream in = getInputStream();
        int sent = -1;
        if (null == in) return sent;
        try {
            int flen = bytesize;
            Context cx = Context.getCurrentContext();
            uio out = uio.findUio(cx, this);
            if (null == out) {
                return sent;
            } else {
                sent = 0;
                int read, buflen;
                if (flen > 102400) buflen = 4096; else if (flen < 8192) buflen = 512; else buflen = 1024;
                byte buf[] = new byte[buflen], copier[] = null;
                Object[] uioargs = new Object[1];
                while (0 < (read = in.read(buf, 0, buflen))) {
                    if (null == copier || copier.length != read) copier = new byte[read];
                    System.arraycopy(buf, 0, copier, 0, read);
                    out._write(cx, uioargs, null);
                    sent += read;
                }
                return sent;
            }
        } catch (IOException iox) {
            return sent;
        } finally {
            try {
                in.close();
            } catch (Exception exc) {
            } finally {
                _din = null;
            }
        }
    }

    /**
     * Return a compact string that uniquely identifies a file-
     * resource and its last modification date (a hash of filename and
     * timestamp), for the <tt>`HTTP/1.1'</tt> "ETag" header.  
     */
    public final String eTag() {
        String pathname = toString();
        long dirhash = utf8.xor64(pathname);
        long dirtims = lastModified();
        return Long.toHexString(dirhash ^ dirtims);
    }

    /**
     * As in <tt>`java.net.URL'</tt>, return the URL.
     */
    public String toString() {
        if (null == url) return getClass().getName() + "@" + System.identityHashCode(this) + "[empty]"; else return url.toString();
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object ano) {
        if (ano == this) return true; else if (ano instanceof urlc) return toString().equals(ano.toString()); else return false;
    }

    /**
     * Finalize calls `close' to be sure to not leave any open file
     * descriptors since `urlc' objects in the `file' stack can be
     * forgotten and left open.
     */
    protected void finalize() throws Throwable {
        close();
    }

    public Boolean jsGet_isOpen() {
        if (isOpen()) return Boolean.TRUE; else return Boolean.FALSE;
    }

    public String jsGet_url() {
        if (null != url) return url.toString(); else return null;
    }

    public void jsSet_url(Object u) throws JavaScriptException {
        if (null == u) return; else {
            String url = u.toString();
            try {
                jsFunction_close(null, this, null, null);
            } catch (Exception exc) {
            }
            try {
                this.url = new URL(url);
                if (null != this.query) this.query.activate(); else this.query = new QueryObject(this);
                encoding = null;
                bytesize = 0;
                mimetype = null;
                serviceDate = 0;
                contentExpires = 0;
                contentLastmod = 0;
            } catch (MalformedURLException malurl) {
                throw new JavaScriptException("Unrecognized url \"" + url + "\".");
            }
        }
    }

    /**
     * Returns a "java.util.Locale" wrapped as a native javascript string.
     */
    public String jsGet_locale() throws JavaScriptException {
        if (null != _uu) super.jsSet_locale(_uu.getHeaderField("Content-Language"));
        return super.jsGet_locale();
    }

    public String jsGet_encoding() {
        return encoding;
    }

    public Integer jsGet_bytesize() {
        return new Integer(bytesize);
    }

    public String jsGet_mimetype() {
        return mimetype;
    }

    public Integer jsGet_serviceDate() {
        if (0 < serviceDate) return new Integer((int) (serviceDate / 1000L)); else return new Integer(0);
    }

    public Integer jsGet_contentExpires() {
        if (0 < contentExpires) return new Integer((int) (contentExpires / 1000L)); else return new Integer(0);
    }

    public Integer jsGet_contentLastmod() {
        if (0 < contentLastmod) return new Integer((int) (contentLastmod / 1000L)); else return new Integer(0);
    }

    public String jsGet_hostname() {
        if (null == url) return null; else return url.getHost();
    }

    public Integer jsGet_portnum() {
        if (null == url) return new Integer(0); else {
            int pn = url.getPort();
            if (0 >= pn) {
                String pr = url.getProtocol().toLowerCase();
                if ("http".equals(pr)) return new Integer(80); else if ("ftp".equals(pr)) return new Integer(21); else return new Integer(-1);
            } else return new Integer(pn);
        }
    }

    public String jsGet_path() {
        if (null == url) return null; else return url.getFile();
    }

    public void jsSet_path(Object value) throws JavaScriptException {
        if (isOpen()) throw new JavaScriptException("Connection is open."); else {
            if (null == value || value instanceof Undefined) try {
                if (null == this.url) throw new JavaScriptException("Require url."); else this.url = new URL(this.url, "/");
            } catch (MalformedURLException malurl) {
                throw new IllegalStateException("BBBUGGGG! Unreachable.");
            } else {
                String p;
                if (value instanceof String) p = (String) value; else p = ScriptRuntime.toString(value);
                this.url = constructPath(this.url, p);
            }
        }
    }

    public String jsGet_anchor() {
        if (null == url) return null; else return url.getRef();
    }

    public Scriptable jsGet_query() {
        if (null == this.query) {
            if (null == url) return null; else this.query = new QueryObject(this);
        } else if (!this.query.active()) this.query.activate();
        return this.query;
    }

    /**
     * Returns a PrintStream 
     *
     * @see org.rhinojetty.uio */
    public PrintStream jsGet_out() throws JavaScriptException {
        return getPrintStream();
    }

    /**
     * Returns a DataInputStream 
     *
     * @see org.rhinojetty.uio */
    public DataInputStream jsGet_in() throws JavaScriptException {
        return getInputStream();
    }

    public Object get(Object key) {
        return get(key.toString(), this);
    }

    public Object get(String name, Scriptable start) {
        if (null == _uu) return super.get(name, start); else return _uu.getHeaderField(name);
    }

    public boolean contains(String name) {
        return has(name, this);
    }

    public boolean has(String name, Scriptable start) {
        if (null == _uu) return super.has(name, start); else return (null != _uu.getHeaderField(name));
    }

    public void put(String name, Object value) {
        put(name, this, value);
    }

    public void put(String name, Scriptable start, Object value) {
        if (null == url || null != _din || null != _uu_out) super.put(name, start, value); else {
            if (null == _uu) {
                try {
                    _uu = url.openConnection();
                    try {
                        if (null != super._loc) _uu.setRequestProperty("Accept-Language", jsGet_locale().toString());
                    } catch (JavaScriptException jsx) {
                    }
                } catch (IOException iox) {
                    throw new IllegalStateException("Connection failed, cannot assign request headers.");
                }
            }
            _uu.setRequestProperty(name, ScriptRuntime.toString(value));
        }
    }

    /**
     * Returns an Integer with the total number of bytes written.
     * Defined as a static "jsFunction_write" with a `thisObj' argument. 
     *
     * @see org.rhinojetty.uio */
    public Object _write(Context cx, Object[] args, Function fun) throws JavaScriptException {
        try {
            OutputStream out = getOutputStream();
            if (null == out) return Undefined.instance; else {
                byte[] bb = bbuf.toByteArray(args);
                if (null == bb) return new Integer(0); else {
                    out.write(bb);
                    return new Integer(bb.length);
                }
            }
        } catch (IOException iox) {
            String msg = iox.getMessage();
            if (null != msg) throw new JavaScriptException("Connection errors (" + msg + ")"); else throw new JavaScriptException("Connection errors (" + iox + ")");
        }
    }

    /**
     * Returns an Integer with the total number of bytes written.
     * Defined as a static "jsFunction_print" with a `thisObj' argument. 
     *
     * @see org.rhinojetty.uio */
    public Object _print(Context cx, Object[] args, Function fun) throws JavaScriptException {
        try {
            OutputStream out = getOutputStream();
            if (null == out) return Undefined.instance; else {
                char[] cb = chbuf.toCharArray(args);
                if (null == cb) return new Integer(0); else {
                    byte[] bb = utf8.encode(cb);
                    int bblen = bb.length;
                    out.write(bb, 0, bblen);
                    return new Integer(bblen);
                }
            }
        } catch (IOException iox) {
            String msg = iox.getMessage();
            if (null != msg) throw new JavaScriptException("Connection errors (" + msg + ")"); else throw new JavaScriptException("Connection errors (" + iox + ")");
        }
    }

    /**
     * Returns an Integer with the total number of bytes written,
     * including newlines. 
     * Defined as a static "jsFunction_println" with a `thisObj' argument.
     *
     * @see org.rhinojetty.uio */
    public Object _println(Context cx, Object[] args, Function fun) throws JavaScriptException {
        try {
            OutputStream out = getOutputStream();
            if (null == out) return Undefined.instance; else {
                String s = linebuf.toString(args);
                if (null == s) {
                    byte[] bb = { '\r', '\n' };
                    _uu_out.write(bb, 0, 2);
                    return new Integer(2);
                } else {
                    char[] newline = { '\r', '\n' };
                    byte[] bb = utf8.encode(chbuf.cat(s.toCharArray(), newline));
                    if (null == bb) return new Integer(0); else {
                        int bblen = bb.length;
                        _uu_out.write(bb, 0, bblen);
                        return new Integer(bblen);
                    }
                }
            }
        } catch (IOException iox) {
            String msg = iox.getMessage();
            if (null != msg) throw new JavaScriptException("Connection errors (" + msg + ")"); else throw new JavaScriptException("Connection errors (" + iox + ")");
        }
    }

    /**
     * Returns an Integer for one byte read.
     */
    public Object _read(Context cx, Object[] args, Function fun) throws JavaScriptException {
        try {
            if (null == _din) jsGet_in();
            if (null != _din) return new Integer(_din.read()); else return Undefined.instance;
        } catch (IOException iox) {
            String msg = iox.getMessage();
            if (null == msg) throw new JavaScriptException("Connection errors (" + iox + ")"); else throw new JavaScriptException("Connection errors (" + msg + ")");
        }
    }

    /**
     * Returns a String for one text line read.
     */
    public Object _readLine(Context cx, Object[] args, Function fun) throws JavaScriptException {
        try {
            if (null == _din) jsGet_in();
            if (null != _din) return _din.readLine(); else return Undefined.instance;
        } catch (IOException iox) {
            String msg = iox.getMessage();
            if (null == msg) throw new JavaScriptException("Connection errors (" + iox + ")"); else throw new JavaScriptException("Connection errors (" + msg + ")");
        }
    }

    public static Object jsFunction_write(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        urlc thisUrlc = thisUrlc(thisObj);
        return thisUrlc._write(cx, args, fun);
    }

    public static Object jsFunction_print(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        urlc thisUrlc = thisUrlc(thisObj);
        return thisUrlc._print(cx, args, fun);
    }

    public static Object jsFunction_println(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        urlc thisUrlc = thisUrlc(thisObj);
        return thisUrlc._println(cx, args, fun);
    }

    public static Object jsFunction_read(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        urlc thisUrlc = thisUrlc(thisObj);
        return thisUrlc._read(cx, args, fun);
    }

    public static Object jsFunction_readLine(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        urlc thisUrlc = thisUrlc(thisObj);
        return thisUrlc._readLine(cx, args, fun);
    }

    public static Object jsFunction_fetch(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        try {
            urlc thisUrlc = thisUrlc(thisObj);
            Object content = thisUrlc.fetch();
            if (null == content) return Undefined.instance; else return content;
        } catch (IOException iox) {
            String msg = iox.getMessage();
            if (null != msg) throw new JavaScriptException("Connection errors (" + msg + ")"); else throw new JavaScriptException("Connection errors (" + iox + ")");
        }
    }

    /**
     * Returns integer 'zero' for no errors, otherwise the number of
     * errors encountered in flushing and closing any output or input
     * streams.  */
    public static Object jsFunction_close(Context cx, Scriptable thisObj, Object[] args, Function fun) throws JavaScriptException {
        urlc thisUrlc = thisUrlc(thisObj);
        if (thisUrlc.close()) return Boolean.TRUE; else return Boolean.FALSE;
    }

    public String[][] helpary() {
        return HELPARY;
    }

    private static final String[][] HELPARY = { { "Help `urlc'", "A `urlc' can connect to a remote host using HTTP or FTP.  Assign a valid string to the `url' field and then do one of either reading or writing before closing the `url'.  The fields of a `urlc' reflect receipt headers after reading, or can be assigned to for creating request headers before writing or reading.  Note that request headers properly set appear to disappear into the `urlc', and receipt headers cannot be set and so they would appear in the `urlc'." }, { "headline", "Note that like all scriptable objects, a single `urlc' object cannot be used by multiple threads with well defined behavior, for example daemon handlers.  A daemon handler object should not contain a `urlc' outside its handler function, and then the `urlc' must be declared with \"var\" so that this object is not shared by any others.  Use a string URL in the handler object, while the `handler' function makes its own `urlc' using \"var ... = new urlc ...\"." }, null, { "url", "A target URL." }, null, { "headline", "The following are \"readonly\" fields that are found after assigning to the `url' field." }, null, { "protocol", "One of either `http' or `ftp'." }, { "hostname", "Remote host name or IP address." }, { "portnum", "Remote port number." }, { "path", "File path before any query string." }, { "anchor", "A `#anchor-name' reference as used to goto a particular anchor within an HTML file." }, { "query", "Request url query string object.  Modifying this object updates the request url query string for subsequent requests." }, { "locale", "Request- reply language locale." }, { "isOpen", "Whether the connection is currently open and needs to be closed." }, null, { "headline", "The following fields used after using a read function." }, null, { "close()", "Shutdown the connection.  (Very important!)" }, { "encoding", "Transmission encoding for content in receipt." }, { "bytesize", "Size in bytes of content object in receipt." }, { "mimetype", "Mimetype for content in receipt." }, { "serviceDate", "Date reported by server." }, { "contentExpires", "Date content in receipt expires." }, { "contentLastmod", "Date content in receipt last modified." }, null, { "headline", "The following are output functions that can only be used before reading." }, null, { "write(b)", "Write a byte." }, { "print(s)", "Write a string (UTF-8)." }, { "println(s)", "Write a string with a newline." }, null, { "headline", "The following are input functions." }, null, { "read()", "Read a byte." }, { "readLine()", "Read a string up to a newline." }, { "fetch()", "Get an object, exclusive of any other reading methods." } };

    public String getClassName() {
        return "urlc";
    }

    protected static urlc thisUrlc(Scriptable thisObj) throws JavaScriptException {
        Scriptable obj = thisObj, m;
        while (obj != null) {
            m = obj;
            do {
                if (m instanceof urlc) return (urlc) m;
                m = m.getPrototype();
            } while (m != null);
            obj = obj.getParentScope();
        }
        throw new JavaScriptException("Can't resolve `urlc' instance.");
    }

    public int mapNameToId(String s) {
        int id = 0;
        L0: {
            id = 0;
            String X = null;
            int c;
            L: switch(s.length()) {
                case 2:
                    if (s.charAt(0) == 'i' && s.charAt(1) == 'n') {
                        id = Id_in;
                        break L0;
                    }
                    break L;
                case 3:
                    c = s.charAt(0);
                    if (c == 'o') {
                        if (s.charAt(2) == 't' && s.charAt(1) == 'u') {
                            id = Id_out;
                            break L0;
                        }
                    } else if (c == 'u') {
                        if (s.charAt(2) == 'l' && s.charAt(1) == 'r') {
                            id = Id_url;
                            break L0;
                        }
                    }
                    break L;
                case 4:
                    c = s.charAt(0);
                    if (c == 'p') {
                        X = "path";
                        id = Id_path;
                    } else if (c == 'r') {
                        X = "read";
                        id = Id_read;
                    }
                    break L;
                case 5:
                    switch(s.charAt(0)) {
                        case 'c':
                            X = "close";
                            id = Id_close;
                            break L;
                        case 'f':
                            X = "fetch";
                            id = Id_fetch;
                            break L;
                        case 'p':
                            X = "print";
                            id = Id_print;
                            break L;
                        case 'q':
                            X = "query";
                            id = Id_query;
                            break L;
                        case 'w':
                            X = "write";
                            id = Id_write;
                            break L;
                    }
                    break L;
                case 6:
                    c = s.charAt(0);
                    if (c == 'a') {
                        X = "anchor";
                        id = Id_anchor;
                    } else if (c == 'i') {
                        X = "isOpen";
                        id = Id_isOpen;
                    } else if (c == 'l') {
                        X = "locale";
                        id = Id_locale;
                    }
                    break L;
                case 7:
                    c = s.charAt(1);
                    if (c == 'o') {
                        X = "portnum";
                        id = Id_portnum;
                    } else if (c == 'r') {
                        X = "println";
                        id = Id_println;
                    }
                    break L;
                case 8:
                    switch(s.charAt(0)) {
                        case 'b':
                            X = "bytesize";
                            id = Id_bytesize;
                            break L;
                        case 'e':
                            X = "encoding";
                            id = Id_encoding;
                            break L;
                        case 'h':
                            X = "hostname";
                            id = Id_hostname;
                            break L;
                        case 'm':
                            X = "mimetype";
                            id = Id_mimetype;
                            break L;
                        case 'r':
                            X = "readLine";
                            id = Id_readLine;
                            break L;
                    }
                    break L;
                case 11:
                    c = s.charAt(0);
                    if (c == 'c') {
                        X = "constructor";
                        id = Id_constructor;
                    } else if (c == 's') {
                        X = "serviceDate";
                        id = Id_serviceDate;
                    }
                    break L;
                case 14:
                    c = s.charAt(7);
                    if (c == 'E') {
                        X = "contentExpires";
                        id = Id_contentExpires;
                    } else if (c == 'L') {
                        X = "contentLastmod";
                        id = Id_contentLastmod;
                    }
                    break L;
            }
            if (X != null && X != s && !X.equals(s)) id = 0;
        }
        return id;
    }

    private static final int Id_constructor = 1, Id_write = 2, Id_print = 3, Id_println = 4, Id_read = 5, Id_readLine = 6, Id_fetch = 7, Id_close = 8, LAST_METHOD_ID = 8, Id_isOpen = 9, Id_url = 10, Id_locale = 11, Id_encoding = 12, Id_bytesize = 13, Id_mimetype = 14, Id_serviceDate = 15, Id_contentExpires = 16, Id_contentLastmod = 17, Id_hostname = 18, Id_portnum = 19, Id_path = 20, Id_anchor = 21, Id_query = 22, Id_out = 23, Id_in = 24, MAX_INSTANCE_ID = 24;

    public String getIdName(int id) {
        switch(id) {
            case Id_constructor:
                return "constructor";
            case Id_write:
                return "write";
            case Id_print:
                return "print";
            case Id_println:
                return "println";
            case Id_read:
                return "read";
            case Id_readLine:
                return "readLine";
            case Id_fetch:
                return "fetch";
            case Id_close:
                return "close";
            case Id_isOpen:
                return "isOpen";
            case Id_url:
                return "url";
            case Id_locale:
                return "locale";
            case Id_encoding:
                return "encoding";
            case Id_bytesize:
                return "bytesize";
            case Id_mimetype:
                return "mimetype";
            case Id_serviceDate:
                return "serviceDate";
            case Id_contentExpires:
                return "contentExpires";
            case Id_contentLastmod:
                return "contentLastmod";
            case Id_hostname:
                return "hostname";
            case Id_portnum:
                return "portnum";
            case Id_path:
                return "path";
            case Id_anchor:
                return "anchor";
            case Id_query:
                return "query";
            case Id_out:
                return "out";
            case Id_in:
                return "in";
            default:
                return null;
        }
    }

    public Object execMethod(int methodId, IdFunction f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) throws JavaScriptException {
        switch(methodId) {
            case Id_constructor:
                return jsConstructor(cx, args, f, (null == thisObj));
            case Id_write:
                return jsFunction_write(cx, this, args, f);
            case Id_print:
                return jsFunction_print(cx, this, args, f);
            case Id_println:
                return jsFunction_println(cx, this, args, f);
            case Id_read:
                return jsFunction_read(cx, this, args, f);
            case Id_readLine:
                return jsFunction_readLine(cx, this, args, f);
            case Id_fetch:
                return jsFunction_fetch(cx, this, args, f);
            case Id_close:
                return jsFunction_close(cx, this, args, f);
            default:
                return super.execMethod(methodId, f, cx, scope, thisObj, args);
        }
    }

    public int methodArity(int methodId) {
        switch(methodId) {
            case Id_constructor:
                return -1;
            case Id_write:
                return -1;
            case Id_print:
                return -1;
            case Id_println:
                return -1;
            case Id_read:
                return -1;
            case Id_readLine:
                return -1;
            case Id_fetch:
                return -1;
            case Id_close:
                return -1;
            default:
                return super.methodArity(methodId);
        }
    }

    public Object getIdValue(int id) {
        if (id > LAST_METHOD_ID) {
            try {
                switch(id) {
                    case Id_isOpen:
                        return jsGet_isOpen();
                    case Id_url:
                        return jsGet_url();
                    case Id_locale:
                        return jsGet_locale();
                    case Id_encoding:
                        return jsGet_encoding();
                    case Id_bytesize:
                        return jsGet_bytesize();
                    case Id_mimetype:
                        return jsGet_mimetype();
                    case Id_serviceDate:
                        return jsGet_serviceDate();
                    case Id_contentExpires:
                        return jsGet_contentExpires();
                    case Id_contentLastmod:
                        return jsGet_contentLastmod();
                    case Id_hostname:
                        return jsGet_hostname();
                    case Id_portnum:
                        return jsGet_portnum();
                    case Id_path:
                        return jsGet_path();
                    case Id_anchor:
                        return jsGet_anchor();
                    case Id_query:
                        return jsGet_query();
                    case Id_out:
                        return jsGet_out();
                    case Id_in:
                        return jsGet_in();
                    default:
                        return super.getIdValue(id);
                }
            } catch (Exception exc) {
                return null;
            }
        } else return super.getIdValue(id);
    }

    public void setIdValue(int id, Object value) {
        if (id > LAST_METHOD_ID) {
            try {
                switch(id) {
                    case Id_anchor:
                        return;
                    case Id_bytesize:
                        return;
                    case Id_contentExpires:
                        return;
                    case Id_contentLastmod:
                        return;
                    case Id_encoding:
                        return;
                    case Id_hostname:
                        return;
                    case Id_in:
                        return;
                    case Id_isOpen:
                        return;
                    case Id_locale:
                        return;
                    case Id_mimetype:
                        return;
                    case Id_out:
                        return;
                    case Id_path:
                        return;
                    case Id_portnum:
                        return;
                    case Id_query:
                        return;
                    case Id_serviceDate:
                        return;
                    case Id_url:
                        jsSet_url(value);
                        return;
                    default:
                        super.setIdValue(id, value);
                        return;
                }
            } catch (Exception exc) {
                return;
            }
        }
    }

    public int getIdDefaultAttributes(int id) {
        switch(id) {
            case Id_anchor:
                return READONLY | PERMANENT | DONTENUM;
            case Id_bytesize:
                return READONLY | PERMANENT | DONTENUM;
            case Id_contentExpires:
                return READONLY | PERMANENT | DONTENUM;
            case Id_contentLastmod:
                return READONLY | PERMANENT | DONTENUM;
            case Id_encoding:
                return READONLY | PERMANENT | DONTENUM;
            case Id_hostname:
                return READONLY | PERMANENT | DONTENUM;
            case Id_in:
                return READONLY | PERMANENT | DONTENUM;
            case Id_isOpen:
                return READONLY | PERMANENT | DONTENUM;
            case Id_locale:
                return READONLY | PERMANENT | DONTENUM;
            case Id_mimetype:
                return READONLY | PERMANENT | DONTENUM;
            case Id_out:
                return READONLY | PERMANENT | DONTENUM;
            case Id_path:
                return READONLY | PERMANENT | DONTENUM;
            case Id_portnum:
                return READONLY | PERMANENT | DONTENUM;
            case Id_query:
                return READONLY | PERMANENT | DONTENUM;
            case Id_serviceDate:
                return READONLY | PERMANENT | DONTENUM;
            case Id_url:
                return PERMANENT | DONTENUM;
            default:
                return super.getIdDefaultAttributes(id);
        }
    }

    public int maxInstanceId() {
        return MAX_INSTANCE_ID;
    }
}
