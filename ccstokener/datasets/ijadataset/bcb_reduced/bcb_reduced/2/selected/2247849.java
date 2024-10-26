package alto.sys;

import alto.io.Authentication;
import alto.io.Check;
import alto.io.Code;
import alto.io.Principal;
import alto.io.Uri;
import alto.lang.Address;
import alto.lang.Component;
import alto.lang.HttpMessage;
import alto.lang.HttpRequest;
import alto.lang.Type;
import alto.lang.Value;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.Modifier;
import static javax.tools.JavaFileObject.Kind;

/**
 * The reference class is the primary user interface for handling URLs
 * in source and target programming relative to storage {@link Address
 * Addresses}.  
 * 
 * A reference can pull a resource from a URL, and then store it to an
 * internal location expressing that same URL.  Typically, however,
 * references are used in one mode or the other in the scope of the
 * stack frame lifetime of an instance.
 * 
 * <h3>Operation<h3>
 * 
 * The reference may be employed on a URL or Address.  To employ a
 * reference as a URL the <tt>toRemote()</tt> method must be called on
 * the reference.  Otherwise calling the <tt>toLocal()</tt> method
 * employs the reference in the default mode as operating on the
 * content storage subsystem.
 * 
 * @author jdp
 * @see Address
 * @since 1.6
 */
public class Reference extends java.lang.Object implements IO.Edge, IO.FileObject, alto.io.Uri {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public abstract static class List {

        public static final Reference[] Add(Reference[] list, Reference c) {
            if (null == c) return list; else if (null == list) return new Reference[] { c }; else {
                int len = list.length;
                Reference[] copier = new Reference[len + 1];
                System.arraycopy(list, 0, copier, 0, len);
                copier[len] = c;
                return copier;
            }
        }

        public static final Reference[] Add(Reference[] list, Reference[] c) {
            if (null == c) return list; else if (null == list) return c; else {
                int len = list.length;
                int clen = c.length;
                Reference[] copier = new Reference[len + clen];
                System.arraycopy(list, 0, copier, 0, len);
                System.arraycopy(c, 0, copier, len, clen);
                return copier;
            }
        }

        public static final boolean Equals(Reference[] a, Reference[] b) {
            if (null == a) return (null == b); else if (null == b) return false; else if (a.length == b.length) {
                for (int cc = 0, count = a.length; cc < count; cc++) {
                    if (!a.equals(b)) return false;
                }
                return true;
            } else return false;
        }
    }

    public abstract static class Tools {

        public static final String Parent(String path) {
            if (null == path || 1 > path.length()) return null; else {
                int ix1 = path.lastIndexOf('/');
                if (-1 < ix1) {
                    boolean isfilep = (-1 < path.indexOf('.', ix1));
                    if (isfilep) return path.substring(0, ix1); else return path;
                } else return null;
            }
        }

        public static final String Parent(Reference r) {
            if (null == r) return null; else return Parent(r.toString());
        }

        /**
         * Scan once for <code>"/(src|bin)/"</code>.
         * 
         * @param path String containing infix "/src/" or "/bin/".
         * @return Index of first character (path separator) in one of
         * "/src/" or "/bin/", or negative one for not found.
         */
        public static final int IndexOfSrcBin(String path) {
            if (null == path) return -1; else {
                if (File.pathSeparatorChar != '/') {
                    path = path.replace(File.pathSeparatorChar, '/');
                }
                char[] cary = path.toCharArray();
                int start = -1;
                boolean scan = false;
                for (int cc = 0, count = cary.length; cc < count; cc++) {
                    switch(cary[cc]) {
                        case '/':
                            if (-1 < start && start == (cc - 4)) return start; else {
                                scan = true;
                                start = cc;
                            }
                            break;
                        case 's':
                        case 'b':
                            if (scan) scan = (-1 < start && start == (cc - 1));
                            break;
                        case 'i':
                        case 'r':
                            if (scan) scan = (-1 < start && start == (cc - 2));
                            break;
                        case 'c':
                        case 'n':
                            if (scan) scan = (-1 < start && start == (cc - 3));
                            break;
                        default:
                            scan = false;
                            break;
                    }
                }
                return -1;
            }
        }
    }

    protected final String string;

    protected Address address;

    protected HttpMessage containerWrite;

    protected URL url;

    protected File storage;

    protected String path_request, fext;

    protected Type fextType;

    protected Uri parser;

    protected URI uri;

    public Reference(String string) {
        super();
        if (null != string) {
            this.string = string;
            this.address = new Address(string);
            this.parser = address.getUri();
        } else throw new alto.sys.Error.Argument();
    }

    public Reference(Address address) {
        super();
        if (null != address) {
            this.address = address;
            this.string = address.getAddressReference();
            this.parser = address.getUri();
        } else throw new alto.sys.Error.Argument();
    }

    public Reference(Component[] address) {
        this(new Address(address));
    }

    public Reference(String string, Address address) {
        super();
        if (null != string) {
            this.string = string;
            this.address = address;
            this.parser = new alto.io.u.Uri(string);
        } else throw new alto.sys.Error.Argument();
    }

    public Reference(String hostname, String path) {
        this("http://" + alto.io.u.Chbuf.fcat(hostname, path));
    }

    public Reference(String hostname, Type type, String path) {
        this(("http://" + alto.io.u.Chbuf.fcat(hostname, path)), (new Address(Component.Host.Tools.ValueOf(hostname), Component.Type.Tools.ValueOf(type), Component.Path.Tools.ValueOf(path))));
    }

    public Reference(Component container, Type type, String path) {
        this(new Address(container, type, path));
    }

    /**
     * Change the connection target to URL from Storage.  This must be
     * called before a reference will operate on remote resources.  To
     * invert the effect, call {@link #toLocal()}.
     */
    public Reference toRemote() {
        if (null == this.url) {
            try {
                this.url = new URL(this.string);
                this.storage = null;
            } catch (java.net.MalformedURLException exc) {
                throw new Error.State(this.string, exc);
            }
        }
        return this;
    }

    /**
     * Change the connection target to the file system store from URL.
     */
    public Reference toLocal() {
        if (null == this.storage) {
            this.url = null;
            this.getStorage();
        }
        return this;
    }

    public alto.sys.File getStorage() {
        alto.sys.File storage = this.storage;
        if (null == storage) {
            Address address = this.getAddress();
            if (null != address) {
                FileManager fm = FileManager.Instance();
                if (null != fm) {
                    storage = fm.getStorage(address);
                    this.storage = storage;
                }
            }
        }
        return this.storage;
    }

    public alto.sys.File dropStorage() {
        alto.sys.File storage = this.storage;
        this.storage = null;
        Address address = this.getAddress();
        if (null != address) {
            FileManager fm = FileManager.Instance();
            if (null != fm) {
                storage = fm.dropStorage(address);
            }
        }
        return storage;
    }

    public boolean hasAddress() {
        return (null != this.address);
    }

    public boolean hasNotAddress() {
        return (null == this.address);
    }

    public Address getAddress() {
        return this.address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Reference toAddressRelation(Component.Relation relation) {
        if (this.inAddressRelation(relation)) return this; else return new Reference(this.getAddress().toRelation(relation));
    }

    public Component getAddressRelation() {
        Address address = this.getAddress();
        if (null != address) return address.getComponentRelation(); else return null;
    }

    public boolean isAddressContainerLocal() {
        Component host = getAddressContainer();
        if (null != host) return Component.Host.Local.equals(host); else return false;
    }

    public boolean isAddressContainerGlobal() {
        Component host = getAddressContainer();
        if (null != host) return Component.Host.Global.equals(host); else return false;
    }

    public Component getAddressContainer() {
        Address address = this.getAddress();
        if (null != address) return address.getComponentContainer(); else return null;
    }

    public Component getAddressClass() {
        Address address = this.getAddress();
        if (null != address) return address.getComponentClass(); else return null;
    }

    public Reference toAddressClass(Component.Type type) {
        if (this.inAddressClass(type)) return this; else return new Reference(this.getAddress().toAddressClass(type));
    }

    public Component getAddressPath() {
        Address address = this.getAddress();
        if (null != address) return address.getComponentPath(); else return null;
    }

    public Component getAddressTerminal() {
        Address address = this.getAddress();
        if (null != address) return address.getComponentTerminal(); else return null;
    }

    public boolean inAddressRelation(Component type) {
        Address address = this.getAddress();
        if (null != address) return address.inRelation(type); else return false;
    }

    public boolean inAddressContainer(Component type) {
        Address address = this.getAddress();
        if (null != address) return address.inContainer(type); else return false;
    }

    public boolean inAddressContainerOf(Reference that) {
        if (null != that) {
            Address thisAddress = this.getAddress();
            if (null != thisAddress) {
                Address thatAddress = that.getAddress();
                if (null != thatAddress) return thisAddress.inContainerOf(thatAddress);
            }
        }
        return false;
    }

    public boolean inAddressClass(Component type) {
        Address address = this.getAddress();
        if (null != address) return address.inClass(type); else return false;
    }

    public boolean inAddressClass(Type type) {
        Address address = this.getAddress();
        if (null != address) return address.inClass(type); else return false;
    }

    public boolean isAddressPersistent() {
        Address address = this.getAddress();
        if (null != address) return address.isPersistent(); else return false;
    }

    public boolean isAddressTransient() {
        Address address = this.getAddress();
        if (null != address) return address.isTransient(); else return false;
    }

    public boolean isAddressTransactional() {
        Address address = this.getAddress();
        if (null != address) return address.isTransactional(); else return false;
    }

    public boolean isNotAddressTransactional() {
        Address address = this.getAddress();
        if (null != address) return address.isNotTransactional(); else return false;
    }

    public boolean isAddressToCurrent() {
        Address address = this.getAddress();
        if (null != address) return address.isAddressToCurrent(); else return false;
    }

    public boolean isNotAddressToCurrent() {
        Address address = this.getAddress();
        if (null != address) return address.isNotAddressToCurrent(); else return false;
    }

    public boolean nocache() {
        Address address = this.address;
        if (null != address) return address.nocache(); else return false;
    }

    public String getRequestPath() {
        String path = this.path_request;
        if (null == path) {
            if (null != this.storage) path = this.getStoragePath(); else path = alto.io.u.Chbuf.fcat("/", this.getPath());
            this.path_request = path;
        }
        return path;
    }

    /**
     * Reference container from storage
     */
    public HttpMessage read() throws java.io.IOException {
        File storage = this.getStorage();
        if (null != storage) {
            try {
                return storage.read();
            } catch (alto.sys.UnauthorizedException exc) {
                throw new alto.sys.UnauthorizedException(this.toString(), exc);
            }
        } else return null;
    }

    /**
     * Clone container from storage
     */
    public HttpMessage write() throws java.io.IOException {
        File storage = this.getStorage();
        if (null != storage) {
            try {
                HttpMessage container = storage.write();
                container.setPathCompleteWithDefaults(this);
                this.containerWrite = container;
                return container;
            } catch (alto.sys.UnauthorizedException exc) {
                throw new alto.sys.UnauthorizedException(this.toString(), exc);
            }
        } else return null;
    }

    @Code(Check.TODO)
    public void close() throws java.io.IOException {
        HttpMessage containerWrite = this.containerWrite;
        if (null != containerWrite) {
            try {
                URL url = this.url;
                if (null != url) {
                    URLConnection connection = url.openConnection();
                    connection.setDoOutput(true);
                    if (connection instanceof alto.net.Connection) {
                        alto.net.Connection nc = (alto.net.Connection) connection;
                        nc.setReference(this);
                        nc.write(containerWrite);
                        return;
                    } else throw new alto.sys.Error.Bug(this.toString());
                }
                File storage = this.getStorage();
                if (null != storage) {
                    if (containerWrite.maySetAuthenticationMethodStore()) {
                        if (containerWrite.maySetContext()) {
                            if (containerWrite.authSign()) {
                                if (storage.write(containerWrite)) return; else throw new alto.sys.Error.State("Write failed");
                            } else throw new alto.sys.Error.State("Failed authentication");
                        } else if (containerWrite.authSign()) {
                            if (storage.write(containerWrite)) return; else throw new alto.sys.Error.State("Write failed");
                        } else throw new alto.sys.Error.State("Failed authentication");
                    } else throw new alto.sys.Error.State("Missing authentication method");
                } else throw new alto.sys.Error.Bug(this.toString());
            } finally {
                this.containerWrite = null;
            }
        }
    }

    /**
     * @return Success setting thread context from this reference.
     */
    public boolean enterThreadContextTry() {
        try {
            HttpMessage container = this.read();
            return Thread.MaySetContext(container);
        } catch (java.io.IOException exc) {
            return false;
        }
    }

    public java.nio.channels.ReadableByteChannel openChannelReadable() throws java.io.IOException {
        return null;
    }

    public java.nio.channels.ReadableByteChannel getChannelReadable() {
        return null;
    }

    public java.io.InputStream getInputStream() throws java.io.IOException {
        return null;
    }

    public alto.io.Input getInput() throws java.io.IOException {
        return null;
    }

    public java.io.InputStream openInputStream() throws java.io.IOException {
        return (java.io.InputStream) this.openInput();
    }

    public alto.io.Input openInput() throws java.io.IOException {
        URL url = this.url;
        if (null != url) {
            URLConnection connection = url.openConnection();
            if (connection instanceof alto.net.Connection) {
                ((alto.net.Connection) connection).setReference(this);
            }
            return new ReferenceInputStream(this, connection);
        }
        HttpMessage container = this.read();
        if (null != container) return new ReferenceInputStream(this, container); else return null;
    }

    public java.nio.channels.WritableByteChannel openChannelWritable() throws java.io.IOException {
        return null;
    }

    public java.nio.channels.WritableByteChannel getChannelWritable() {
        return null;
    }

    public java.io.OutputStream getOutputStream() throws java.io.IOException {
        return null;
    }

    public alto.io.Output getOutput() throws java.io.IOException {
        return null;
    }

    public java.io.OutputStream openOutputStream() throws java.io.IOException {
        return (java.io.OutputStream) this.openOutput();
    }

    public alto.io.Output openOutput(Object content) throws java.io.IOException {
        alto.io.Output out = this.openOutput();
        this.setStorageContent(content);
        return out;
    }

    public alto.io.Output openOutput() throws java.io.IOException {
        URL url = this.url;
        if (null != url) {
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            if (connection instanceof alto.net.Connection) {
                ((alto.net.Connection) connection).setReference(this);
            }
            return new ReferenceOutputStream(this, connection);
        }
        HttpMessage container = this.write();
        return new ReferenceOutputStream(this, container);
    }

    public java.io.Reader openReader(boolean ignoreEncodingErrors) throws java.io.IOException {
        java.io.InputStream in = this.openInputStream();
        if (null != in) return new java.io.InputStreamReader(in, UTF8); else throw new java.io.IOException("Not found");
    }

    public java.io.Writer openWriter() throws java.io.IOException {
        java.io.OutputStream out = this.openOutputStream();
        if (null != out) return new java.io.OutputStreamWriter(out, UTF8); else throw new java.io.IOException("Not found");
    }

    public Uri getUri() {
        return this;
    }

    public Uri getCreateParser(boolean meta) {
        Uri parser = this.parser;
        if (null == parser) {
            Address address = this.address;
            if (null != address && address.hasHostPath()) {
                parser = address.getCreateParser();
                this.parser = parser;
                return parser;
            }
            if (meta) {
                try {
                    String string = this.getContentLocation();
                    if (null != string) {
                        parser = new alto.io.u.Uri(string);
                        this.parser = parser;
                    }
                } catch (java.io.IOException ignore) {
                }
            }
            String string = this.string;
            if (null != string) {
                parser = new alto.io.u.Uri(string);
                this.parser = parser;
            }
        }
        return parser;
    }

    /**
     * @return If address is not null, return address storage path.
     */
    public String getStoragePath() {
        Address address = this.getAddress();
        if (null != address) return address.getPathStorage(); else return null;
    }

    public Object getStorageContent() throws java.io.IOException {
        File storage = this.getStorage();
        if (null != storage) return storage.getContent(); else return null;
    }

    /**
     * @return Argument on success
     */
    public Object setStorageContent(Object content) throws java.io.IOException {
        HttpMessage containerWrite = this.containerWrite;
        if (null != containerWrite) containerWrite.setStorageContent(content);
        File storage = this.getStorage();
        if (null != storage) storage.setContent(content);
        return content;
    }

    public Object dropStorageContent() throws java.io.IOException {
        File storage = this.getStorage();
        if (null != storage) return storage.dropContent(); else return null;
    }

    public boolean hasStorage() {
        return (null != this.getStorage());
    }

    public boolean hasNotStorage() {
        return (null == this.getStorage());
    }

    public boolean existsStorage() {
        File storage = this.getStorage();
        if (null != storage) return storage.exists(); else return false;
    }

    public boolean existsNotStorage() {
        File storage = this.getStorage();
        if (null != storage) return (!storage.exists()); else return true;
    }

    public boolean isStorageFile() {
        File storage = this.getStorage();
        if (null != storage) return storage.isFile(); else return false;
    }

    public boolean isNotStorageFile() {
        File storage = this.getStorage();
        if (null != storage) return (!storage.isFile()); else return false;
    }

    public long getStorageLastModified() {
        File storage = this.getStorage();
        if (null != storage) return storage.lastModified(); else return 0L;
    }

    public java.lang.String getStorageLastModifiedString() {
        File storage = this.getStorage();
        if (null != storage) return storage.lastModifiedString(); else return null;
    }

    public long getStorageLength() {
        File storage = this.getStorage();
        if (null != storage) return storage.length(); else return 0L;
    }

    public byte[] getBuffer() throws java.io.IOException {
        HttpMessage storage = this.read();
        if (null != storage) return storage.getBuffer(); else return null;
    }

    public int getBufferLength() throws java.io.IOException {
        HttpMessage storage = this.read();
        if (null != storage) return storage.getBufferLength(); else return 0;
    }

    public CharSequence getCharContent(boolean igEncErr) throws java.io.IOException {
        HttpMessage storage = this.read();
        if (null != storage) return storage.getCharContent(igEncErr); else return null;
    }

    public Class getCharContentAsClass() throws java.io.IOException {
        String name = (String) this.getCharContent(true);
        if (null != name) {
            name = name.trim();
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException exc) {
                throw new Error.State(name, exc);
            }
        } else return null;
    }

    public void setBuffer(byte[] buf) throws java.io.IOException {
        HttpMessage storage = this.write();
        try {
            if (null == buf) storage.setBody(alto.lang.Buffer.Empty); else storage.setBody(buf);
        } finally {
            this.close();
        }
    }

    public void setBufferFrom(alto.lang.Buffer bits) throws java.io.IOException {
        if (null != bits) this.setBuffer(bits.getBuffer()); else throw new Error.Argument();
    }

    public void setCharContent(char[] cary) throws java.io.IOException {
        this.setBuffer(alto.io.u.Utf8.encode(cary));
    }

    public void setCharContent(String string) throws java.io.IOException {
        if (null != string) this.setCharContent(string.toCharArray()); else this.setBuffer(null);
    }

    public void setCharContent(CharSequence string) throws java.io.IOException {
        if (null != string) this.setCharContent(string.toString()); else this.setBuffer(null);
    }

    public void setCharContentAsClass(Class clas) throws java.io.IOException {
        this.setCharContent(clas.getName());
    }

    /**
     * Basic implementation of HTTP GET (this) Reference without
     * Conditional GET semantics.
     */
    public boolean get(HttpMessage response) throws java.io.IOException {
        return this.copyTo(response);
    }

    /**
     * Basic implementation of HTTP GET (this) Reference without
     * Conditional GET semantics.
     */
    public boolean get(Reference target) throws java.io.IOException {
        return this.copyTo(target);
    }

    /**
     * Basic implementation of HTTP HEAD (this) Reference without
     * Conditional GET semantics.
     */
    public boolean head(HttpMessage response) throws java.io.IOException {
        return this.headTo(response);
    }

    /**
     * Basic implementation of HTTP PUT (this) Reference without
     * Conditional PUT semantics.
     */
    public boolean put(HttpMessage request) throws java.io.IOException {
        return this.copyFrom(request);
    }

    /**
     * Basic implementation of HTTP PUT (this) Reference without
     * Conditional PUT semantics.
     */
    public boolean put(Reference source) throws java.io.IOException {
        source.copyTo(this);
        return true;
    }

    public boolean put(alto.io.Message request) throws java.io.IOException {
        alto.io.Output out = this.openOutput(request);
        try {
            request.writeMessage(out);
            return true;
        } finally {
            out.close();
        }
    }

    public boolean delete() {
        File storage = this.getStorage();
        if (null != storage) return storage.delete(); else return false;
    }

    public boolean revert() throws java.io.IOException {
        File storage = this.getStorage();
        if (null != storage) return storage.revert(); else return false;
    }

    public boolean copyFrom(HttpMessage request) throws java.io.IOException {
        if (null != request) {
            if (request.hasNotLocation() && request instanceof HttpRequest) ((HttpRequest) request).setLocation();
            try {
                this.containerWrite = request;
                return true;
            } finally {
                this.close();
            }
        } else return false;
    }

    /**
     * @see alto.sx.methods.Copy
     * @return True for response Created, False for response Not
     * Found.
     */
    public boolean copyTo(Reference target) throws java.io.IOException {
        if (null != this.url) {
            alto.io.Input in = this.openInput();
            try {
                alto.io.Output out = target.openOutput();
                try {
                    if (null != in && null != out) {
                        int r = in.copyTo(out);
                        return (0 < r);
                    }
                } finally {
                    if (null != out) out.close();
                }
            } finally {
                if (null != in) in.close();
            }
            return false;
        }
        File storage = this.getStorage();
        if (null != storage) {
            HttpMessage source = this.read();
            if (null != source) return target.copyFrom(source); else return false;
        } else throw new alto.sys.Error.Bug(this.string);
    }

    public boolean copyTo(HttpMessage response) throws java.io.IOException {
        HttpMessage storage = this.read();
        if (null != storage) return storage.copyTo(response); else return false;
    }

    public boolean headTo(HttpMessage response) throws java.io.IOException {
        HttpMessage storage = this.read();
        if (null != storage) return storage.headTo(response); else return false;
    }

    public boolean moveTo(Reference dst) throws java.io.IOException {
        File srcStorage = this.getStorage();
        if (null != srcStorage) {
            File dstStorage = dst.getStorage();
            if (null != dstStorage) return srcStorage.renameTo(dstStorage);
        }
        return false;
    }

    public org.w3c.dom.Document readDocument() throws java.io.IOException {
        HttpMessage storage = this.read();
        if (null != storage) return Xml.Tools.ReadDocument(null, storage); else return null;
    }

    public void writeDocument(org.w3c.dom.Document doc) throws java.io.IOException {
        HttpMessage storage = this.write();
        try {
            Xml.Tools.WriteDocument(null, doc, storage);
        } finally {
            this.close();
        }
    }

    public Kind getKind() {
        try {
            Type type = this.getContentType();
            if (Type.Tools.Of("java") == type) return Kind.SOURCE; else if (Type.Tools.Of("class") == type) return Kind.CLASS; else if (Type.Tools.Of("html") == type) return Kind.HTML; else return Kind.OTHER;
        } catch (java.io.IOException exc) {
            return Kind.OTHER;
        }
    }

    public boolean isNameCompatible(String simpleName, Kind kind) {
        return true;
    }

    public NestingKind getNestingKind() {
        return null;
    }

    public Modifier getAccessLevel() {
        return null;
    }

    /**
     * Called from {@link alto.sx.methods.Lock}
     */
    public boolean networkLockAcquire(long timeout) throws java.io.IOException {
        File file = this.getStorage();
        if (null != file) return file.networkLockAcquire(timeout); else throw new alto.sys.Error.State(this.toString());
    }

    /**
     * Called from {@link alto.sx.methods.Unlock}
     */
    public boolean networkLockRelease() throws java.io.IOException {
        File file = this.getStorage();
        if (null != file) return file.networkLockRelease(); else return false;
    }

    public int lockReadLockCount() {
        File storage = this.getStorage();
        if (null != storage) return storage.lockReadLockCount(); else return 0;
    }

    public boolean lockReadEnterTry() {
        File storage = this.getStorage();
        if (null != storage) return storage.lockReadEnterTry(); else return true;
    }

    public boolean lockReadEnterTry(long millis) throws java.lang.InterruptedException {
        File storage = this.getStorage();
        if (null != storage) return storage.lockReadEnterTry(millis); else return true;
    }

    public void lockReadEnter() {
        File storage = this.getStorage();
        if (null != storage) storage.lockReadEnter();
    }

    public void lockReadExit() {
        File storage = this.getStorage();
        if (null != storage) storage.lockReadExit();
    }

    public int lockWriteHoldCount() {
        File storage = this.getStorage();
        if (null != storage) return storage.lockWriteHoldCount(); else return 0;
    }

    public boolean lockWriteEnterTry() {
        File storage = this.getStorage();
        if (null != storage) return storage.lockWriteEnterTry(); else return true;
    }

    public boolean lockWriteEnterTry(long millis) throws java.lang.InterruptedException {
        File storage = this.getStorage();
        if (null != storage) return storage.lockWriteEnterTry(millis); else return true;
    }

    public void lockWriteEnter() {
        File storage = this.getStorage();
        if (null != storage) storage.lockWriteEnter();
    }

    public void lockWriteExit() {
        File storage = this.getStorage();
        if (null != storage) storage.lockWriteExit();
    }

    protected Reference getReferenceIn(char select) {
        String path = this.string;
        int idx = Reference.Tools.IndexOfSrcBin(path);
        if (-1 < idx) {
            int idx4 = (idx + 4);
            String test = path.substring(idx, idx4);
            if (select == test.charAt(1)) return this; else {
                String newpath;
                switch(select) {
                    case 'b':
                        newpath = path.substring(0, idx) + "/bin/" + path.substring(idx4);
                        return new Reference(newpath);
                    case 's':
                        newpath = path.substring(0, idx) + "/src/" + path.substring(idx4);
                        return new Reference(newpath);
                    default:
                        throw new Error.Bug(path);
                }
            }
        } else throw new Error.State(path);
    }

    /**
     * For reference into a subpath of '/src/' or '/bin/'
     * @return A reference into the corresponding subpath of '/bin/'.
     */
    public Reference getReferenceInBin() {
        return this.getReferenceIn('b');
    }

    /**
     * For reference into a subpath of '/src/' or '/bin/'
     * @return A reference into the corresponding subpath of '/src/'.
     */
    public Reference getReferenceInSrc() {
        return this.getReferenceIn('s');
    }

    /**
     * For a reference having URL host and path components
     * @return A correct and complete reference to the argument
     * filename extension.
     */
    public Reference getReferenceInFext(String fext) {
        if (null != fext) {
            String string = this.string;
            int idx = Type.Tools.IndexOfFext(string);
            if (-1 < idx) {
                String nstring = string.substring(0, (idx + 1)) + fext;
                return new Reference(nstring);
            } else throw new Error.State(string);
        } else throw new Error.Argument("Missing 'String fext'.");
    }

    public String getPathNameExt() {
        String fext = this.fext;
        if (null == fext) {
            String path = this.getPathTail();
            if (null != path) {
                fext = Type.Tools.FextX(path);
                this.fext = fext;
            }
        }
        return fext;
    }

    public String getPathWithoutNameExt() {
        String fext = this.getPathNameExt();
        if (null == fext) return this.getPath(); else {
            String path = this.getPath();
            int term = (path.length() - (fext.length() + 1));
            return path.substring(0, term);
        }
    }

    public String getPathNameExtMeta() {
        String fext = this.fext;
        if (null == fext) {
            String path = this.getPathTailMeta();
            if (null != path) {
                fext = Type.Tools.FextX(path);
                this.fext = fext;
            }
        }
        return fext;
    }

    public Type getPathNameType() {
        Type fextType = this.fextType;
        if (null == fextType) {
            String fext = this.getPathNameExt();
            if (null != fext) {
                fextType = Type.Tools.Of(fext);
                this.fextType = fextType;
            }
        }
        return fextType;
    }

    public Type getPathNameTypeMeta() {
        Type fextType = this.fextType;
        if (null == fextType) {
            String fext = this.getPathNameExtMeta();
            if (null != fext) {
                fextType = Type.Tools.Of(fext);
                this.fextType = fextType;
            }
        }
        return fextType;
    }

    public Type getContentType() throws java.io.IOException {
        Type type = this.getPathNameType();
        if (null != type) return type; else {
            HttpMessage meta = this.read();
            if (null != meta) {
                type = meta.getContentType();
                if (null != type) return type;
            }
            Address address = this.getAddress();
            if (null != address) {
                Component.Type typeComponent = address.getComponentClass();
                if (null != typeComponent) return Type.Tools.For(typeComponent);
            }
            return null;
        }
    }

    public String getContentLocation() throws java.io.IOException {
        HttpMessage storage = this.read();
        if (null != storage) return storage.getLocation(); else return this.getLocationMeta();
    }

    public String getETag() throws java.io.IOException {
        HttpMessage storage = this.read();
        if (null != storage) return storage.getETag(); else return null;
    }

    public boolean hasLocation() {
        return (null != this.getHostName()) && (null != this.getPath());
    }

    public String getLocation() {
        String string = this.string;
        if (null != string && string.startsWith("http://")) return string; else {
            String host = this.getHostName();
            if (null != host) {
                String path = this.getPath();
                if (null != path) return "http://" + alto.io.u.Chbuf.fcat(host, path);
            }
        }
        return null;
    }

    /**
     * For constructing meta data, don't pull on meta data.
     */
    public String getLocationMeta() {
        String string = this.string;
        if (null != string && string.startsWith("http://")) return string; else {
            Uri parser = this.getCreateParser(false);
            String host = parser.getHostName();
            if (null != host) {
                String path = parser.getPath();
                if (null != path) return "http://" + alto.io.u.Chbuf.fcat(host, path);
            }
        }
        return null;
    }

    public long lastModified() {
        File storage = this.getStorage();
        if (null != storage) return storage.lastModified(); else return 0L;
    }

    public java.lang.String lastModifiedString() {
        File storage = this.getStorage();
        if (null != storage) return storage.lastModifiedString(); else return null;
    }

    public boolean setLastModified(long last) {
        File storage = this.getStorage();
        if (null != storage) {
            storage.setLastModified(last);
            return true;
        } else return false;
    }

    public long getLastModified() {
        try {
            HttpMessage storage = this.read();
            if (null != storage) return storage.getLastModified();
        } catch (java.io.IOException exc) {
        }
        return this.lastModified();
    }

    public java.lang.String getLastModifiedString() {
        try {
            HttpMessage storage = this.read();
            if (null != storage) return storage.getLastModifiedString();
        } catch (java.io.IOException exc) {
        }
        return this.lastModifiedString();
    }

    /**
     * @see javax.tools.FileObject
     */
    public URI toUri() {
        URI uri = this.uri;
        if (null == uri) {
            try {
                uri = new URI(this.string);
                this.uri = uri;
            } catch (java.net.URISyntaxException exc) {
                throw new Error.State(this.string, exc);
            }
        }
        return uri;
    }

    /**
     * @see IO$FileObject
     */
    public java.lang.String getName() {
        return this.getPathTail();
    }

    public boolean isRelative() {
        return this.getCreateParser(true).isRelative();
    }

    public boolean isAbsolute() {
        return this.getCreateParser(true).isAbsolute();
    }

    public java.lang.String getScheme() {
        return this.getCreateParser(true).getScheme();
    }

    public boolean hasScheme() {
        return this.getCreateParser(true).hasScheme();
    }

    public int countScheme() {
        return this.getCreateParser(true).countScheme();
    }

    public java.lang.String getScheme(int idx) {
        return this.getCreateParser(true).getScheme(idx);
    }

    public java.lang.String getSchemeTail() {
        return this.getCreateParser(true).getSchemeTail();
    }

    public java.lang.String getSchemeHead() {
        return this.getCreateParser(true).getSchemeHead();
    }

    public java.lang.String getHostUser() {
        return this.getCreateParser(true).getHostUser();
    }

    public java.lang.String getHostPass() {
        return this.getCreateParser(true).getHostPass();
    }

    public java.lang.String getHostName() {
        return this.getHostName(true);
    }

    public java.lang.String getHostNameMeta() {
        return this.getHostName(false);
    }

    public java.lang.String getHostName(boolean meta) {
        URL url = this.url;
        if (null != url) {
            String hostname = url.getHost();
            if (null != hostname) return hostname;
        }
        Address address = this.address;
        if (null != address) {
            String hostname = address.getHostName();
            if (null != hostname) return hostname;
        }
        Uri parser = this.getCreateParser(meta);
        if (null != parser) {
            return parser.getHostName();
        } else return null;
    }

    public java.lang.String getHostPort() {
        return this.getCreateParser(true).getHostPort();
    }

    public boolean inPath(String searchPath, boolean recurse) {
        if (null == searchPath) return true; else {
            String thisPath = this.getPath();
            if (null != thisPath) {
                int idx = thisPath.indexOf(searchPath);
                if (-1 < idx) {
                    if (recurse) {
                        if ('/' == searchPath.charAt(0)) return (0 == idx); else return true;
                    } else {
                        int term = thisPath.lastIndexOf('/');
                        if (0 > term) return true; else {
                            int sl = searchPath.length();
                            int valid = idx + sl;
                            if ('/' == searchPath.charAt(sl - 1)) return (term == valid); else return ((term - 1) == valid);
                        }
                    }
                }
            }
            return false;
        }
    }

    public java.lang.String getPath() {
        return this.getPath(true);
    }

    public java.lang.String getPathMeta() {
        return this.getPath(false);
    }

    public java.lang.String getPath(boolean meta) {
        if (null != this.parser) return this.parser.getPath(); else {
            URL url = this.url;
            if (null != url) {
                String path = url.getPath();
                if (null != path) return path;
            }
            Address address = this.address;
            if (null != address) {
                String path = address.getPath();
                if (null != path) return path;
            }
            Uri parser = this.getCreateParser(meta);
            if (null != parser) {
                return parser.getPath();
            }
            return null;
        }
    }

    public boolean hasPath() {
        return this.getCreateParser(true).hasPath();
    }

    public boolean hasPathMeta() {
        return this.getCreateParser(false).hasPath();
    }

    public int countPath() {
        return this.getCreateParser(true).countPath();
    }

    public int countPathMeta() {
        return this.getCreateParser(false).countPath();
    }

    public java.lang.String getPath(int idx) {
        return this.getCreateParser(true).getPath(idx);
    }

    public java.lang.String getPathMeta(int idx) {
        return this.getCreateParser(false).getPath(idx);
    }

    public java.lang.String getPathTail() {
        return this.getCreateParser(true).getPathTail();
    }

    public java.lang.String getPathTailMeta() {
        return this.getCreateParser(false).getPathTail();
    }

    public java.lang.String getPathHead() {
        return this.getCreateParser(true).getPathHead();
    }

    public java.lang.String getPathHeadMeta() {
        return this.getCreateParser(false).getPathHead();
    }

    public java.lang.String getPathParent() {
        return this.getCreateParser(true).getPathParent();
    }

    public java.lang.String getPathParentMeta() {
        return this.getCreateParser(false).getPathParent();
    }

    public boolean hasIntern() {
        return this.getCreateParser(true).hasIntern();
    }

    public int countIntern() {
        return this.getCreateParser(true).countIntern();
    }

    public java.lang.String getIntern() {
        return this.getCreateParser(true).getIntern();
    }

    public java.lang.String getIntern(int idx) {
        return this.getCreateParser(true).getIntern(idx);
    }

    public java.lang.String getInternTail() {
        return this.getCreateParser(true).getInternTail();
    }

    public java.lang.String getInternHead() {
        return this.getCreateParser(true).getInternHead();
    }

    public boolean hasQuery() {
        return this.getCreateParser(true).hasQuery();
    }

    public int countQuery() {
        return this.getCreateParser(true).countQuery();
    }

    public java.lang.String getQuery() {
        return this.getCreateParser(true).getQuery();
    }

    public java.lang.String getQuery(int idx) {
        return this.getCreateParser(true).getQuery(idx);
    }

    public java.lang.String[] getQueryKeys() {
        return this.getCreateParser(true).getQueryKeys();
    }

    public java.lang.String getQuery(java.lang.String key) {
        return this.getCreateParser(true).getQuery(key);
    }

    public boolean hasQuery(java.lang.String key) {
        return this.getCreateParser(true).hasQuery(key);
    }

    public boolean hasFragment() {
        return this.getCreateParser(true).hasFragment();
    }

    public int countFragment() {
        return this.getCreateParser(true).countFragment();
    }

    public java.lang.String getFragment() {
        return this.getCreateParser(true).getFragment();
    }

    public java.lang.String getFragment(int idx) {
        return this.getCreateParser(true).getFragment(idx);
    }

    public java.lang.String getFragmentHead() {
        return this.getCreateParser(true).getFragmentHead();
    }

    public java.lang.String getFragmentTail() {
        return this.getCreateParser(true).getFragmentTail();
    }

    public java.lang.String getTerminal() {
        return this.getCreateParser(true).getTerminal();
    }

    public boolean hasTerminal() {
        return this.getCreateParser(true).hasTerminal();
    }

    public int countTerminal() {
        return this.getCreateParser(true).countTerminal();
    }

    public java.lang.String getTerminal(int idx) {
        return this.getCreateParser(true).getTerminal(idx);
    }

    public java.lang.String getTerminalHead() {
        return this.getCreateParser(true).getTerminalHead();
    }

    public java.lang.String getTerminalTail() {
        return this.getCreateParser(true).getTerminalTail();
    }

    public java.lang.String[] getTerminalKeys() {
        return this.getCreateParser(true).getTerminalKeys();
    }

    public java.lang.String getTerminalLookup(java.lang.String key) {
        return this.getCreateParser(true).getTerminalLookup(key);
    }

    public String toString() {
        return this.string;
    }

    public int hashCode() {
        return this.string.hashCode();
    }

    public boolean equals(Object ano) {
        if (ano == this) return true; else if (ano instanceof String) return this.toString().equals((String) ano); else return this.toString().equals(ano.toString());
    }
}
