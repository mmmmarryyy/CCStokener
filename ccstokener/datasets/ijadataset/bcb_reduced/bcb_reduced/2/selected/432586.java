package tufts.vue;

import java.util.*;
import tufts.Util;
import static tufts.Util.*;
import tufts.vue.*;
import tufts.vue.action.OpenAction;
import tufts.vue.gui.GUI;
import java.net.*;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.regex.*;
import org.apache.commons.io.*;

public class WormholeResource extends URLResource {

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(URLResource.class);

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String IMAGE_KEY = HIDDEN_PREFIX + "Image";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String THUMB_KEY = HIDDEN_PREFIX + "Thumb";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String USER_URL = "URL";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String USER_FILE = "File";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String USER_DIRECTORY = "Directory";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String USER_FULL_FILE = RUNTIME_PREFIX + "Full File";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_RELATIVE = HIDDEN_PREFIX + "file.relative";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_RELATIVE_OLD = "file.relative";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_CANONICAL = HIDDEN_PREFIX + "file.canonical";

    /**
     * The most generic version of what we refer to.  Usually set to a full URL or absolute file path.
     * Note that this may have been set on a different platform that we're currently running on,
     * so it may no longer make a valid URL or File on this platform, which is why we need
     * this generic String version of it, and why the Resource/URLResource code can be so complicated.
     */
    private String spec = SPEC_UNSET;

    /**
     * The target file in the wormhole
     */
    private String targetFilename;

    /**
     * The URI String of the component to focus on once we open the map.
     */
    private String componentURIString;

    /**
     * The URI String of the component to focus on once we open the originating map.
     */
    private String originatingComponentURIString;

    /**
     * The originating file in the wormhole
     */
    private String originatingFilename;

    /**
     * A default URL for this resource.  This will be used for "browse" actions, so for
     * example, it may point to any content available through a URL: an HTML page, raw image data,
     * document files, etc.
     */
    private URL mURL;

    /** Points to raw image data (greatest resolution available) */
    private URL mURL_ImageData;

    /** Points to raw image data for an image thumbnail  */
    private URL mURL_ThumbData;

    /**
     * This will be set if we point to a local file the user has control over.
     * This will not be set to point to cache files or package files.
     */
    private File mFile;

    /**
     * If this resource is relative to it's map, this will be set (at least by the time we're persisted)
     */
    private URI mRelativeURI;

    /** an optional resource title */
    private String mTitle;

    /** See tufts.vue.URLResource - reimplementation of private member */
    private boolean mRestoreUnderway = false;

    /** See tufts.vue.URLResource - reimplementation of private member */
    private ArrayList<PropertyEntry> mXMLpropertyList;

    private final String strBackSlashPrefix = "\\\\";

    private final String strBackSlash = "\\";

    private final String strForwardSlashPrefix = "////";

    private final String strForwardSlash = "/";

    /**
     * Creates a WormholeResource given the URI of a target map and the URI of a target component.
     * @param mapURI, the URI of the target map.
     * @param componentURI, the URI of the target component.
     * @return a new WormholeResource.
     * @author Helen Oliver
     */
    static WormholeResource create(java.net.URI mapURI, java.net.URI componentURI) {
        return new WormholeResource(mapURI, componentURI);
    }

    /**
     * Creates a WormholeResource given the URIs of a target map and a source map,
     * and the URIs of a target component and a source component.
     * @param mapURI, the URI of the target map.
     * @param componentURI, the URI of the target component.
     * @param originatingMapURI, the URI of the source map.
     * @param originatingComponentURI, the URI of the source component.
     * @return a new WormholeResource.
     * @author Helen Oliver
     */
    static WormholeResource create(java.net.URI mapURI, java.net.URI componentURI, java.net.URI originatingMapURI, java.net.URI originatingComponentURI) {
        return new WormholeResource(mapURI, componentURI, originatingMapURI, originatingComponentURI);
    }

    /** 
     * @param spec, the String holding the spec for this Wormhole resource
     * which will become the linked-to map file link
     * @param componentURIString, the URI String of the component to focus on when we open that map
     * @author Helen Oliver
     */
    private WormholeResource(String spec, String componentURIString) {
        init();
        setTargetFilename(spec);
        setComponentURIString(componentURIString);
        super.setSpec(spec);
        this.setSpec(spec);
    }

    /** 
     * @param mapURI, the map URI for this Wormhole resource
     * which will become the linked-to map file link
     * @param componentURI, the URI of the component to focus on when we open that map
     * @author Helen Oliver
     */
    private WormholeResource(URI mapURI, URI componentURI) {
        init();
        setTargetFilename(mapURI.toString());
        setComponentURIString(componentURI.toString());
        super.setSpec(mapURI.toString());
        this.setSpec(mapURI.toString());
    }

    /** 
     * @param mapURI, the map URI for this Wormhole resource
     * which will become the linked-to map file link
     * @param componentURI, the URI of the component to focus on when we open that map
     * @param originatingMapURI, the map URI for the source map
     * @param originatingComponentURI, the URI for the source component
     * @author Helen Oliver
     */
    private WormholeResource(URI mapURI, URI componentURI, URI originatingMapURI, URI originatingComponentURI) {
        init();
        setTargetFilename(mapURI.toString());
        setComponentURIString(componentURI.toString());
        String strTarget = mapURI.toString();
        String strDisplayTarget = VueUtil.decodeURIStringToString(strTarget);
        super.setSpec(strDisplayTarget);
        this.setSpec(strDisplayTarget);
        setOriginatingComponentURIString(originatingComponentURI.toString());
        this.setOriginatingFilename(originatingMapURI.toString());
    }

    /** 
     * @param file, the file for this Wormhole resource
     * which will become the linked-to map file link
     * @param component, the component to focus on when we open that map
     * @author Helen Oliver
     */
    private WormholeResource(File file, URI componentURI) {
        init();
        setTargetFilename(file);
        setComponentURIString(componentURI.toString());
        super.setSpecByFile(file);
        this.setSpecByFile(file);
    }

    /** 
     * @param file, the source file for this Wormhole resource
     * which will become the linked-to map file link
     * @param componentURI, the component to focus on when we open that map
     * @param originatingFile, the originating file for this Wormhole resource
     * @param originatingComponentURI, the component to focus on when we open the
     * originating map
     * @author Helen Oliver
     */
    private WormholeResource(File file, URI componentURI, File originatingFile, URI originatingComponentURI) {
        init();
        setTargetFilename(file);
        setComponentURIString(componentURI.toString());
        super.setSpecByFile(file);
        this.setSpecByFile(file);
        setOriginatingComponentURIString(originatingComponentURI.toString());
        setOriginatingFilename(originatingFile);
    }

    /**
     * @deprecated - This constructor needs to be public to support castor persistance ONLY -- it should not
     * be called directly by any code.
     */
    public WormholeResource() {
        init();
    }

    /**
     * Given the path string to the target file, constructs a WormholeResource.
     * @param spec, a String representing the path to the target file
     * @author Helen Oliver
     */
    private WormholeResource(String spec) {
        init();
        super.setSpec(spec);
        this.setSpec(spec);
    }

    /**
     * Given the target File object, constructs the WormholeResource
     * @param file. the target File object
     * @author Helen Oliver
     */
    private WormholeResource(File file) {
        init();
        super.setSpecByFile(file);
        this.setSpecByFile(file);
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private void init() {
        if (DEBUG.RESOURCE) {
            String iname = getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
            setDebugProperty("0INSTANCE", iname);
        }
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_DIRECTORY = "directory";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_NORMAL = "file";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String FILE_UNKNOWN = "unknown";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private void setSpecByFile(File file, Object knownType) {
        if (file == null) {
            Log.error("setSpecByFile", new IllegalArgumentException("null java.io.File"));
            return;
        }
        if (DEBUG.RESOURCE) dumpField("setSpecByFile; type=" + knownType, file);
        if (mURL != null) mURL = null;
        setFile(file, knownType);
        String fileSpec = null;
        try {
            fileSpec = file.getPath();
        } catch (Throwable t) {
            Log.warn(file, t);
            fileSpec = file.getPath();
        }
        setSpec(fileSpec);
        if (DEBUG.RESOURCE && DEBUG.META && "/".equals(fileSpec)) {
            Util.printStackTrace("Root FileSystem Resource created from: " + Util.tags(file));
        }
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private long mLastModified;

    /** See tufts.vue.URLResource - reimplementation of private member */
    private void setFile(File file, Object type) {
        if (mFile == file) return;
        if (DEBUG.RESOURCE || file == null) dumpField("setFile", file);
        mFile = file;
        if (file == null) return;
        if (mURL != null) setURL(null);
        type = setDataFile(file, type);
        if (mTitle == null) {
            String name = file.getName();
            if (name.length() == 0) {
                setTitle(file.toString());
            } else {
                if (Util.isMacPlatform()) {
                    name = name.replace(':', '/');
                }
                setTitle(name);
            }
        }
        if (type == FILE_DIRECTORY) {
            setClientType(Resource.DIRECTORY);
        } else if (type == FILE_NORMAL) {
            setClientType(Resource.FILE);
            if (DEBUG.IO) dumpField("scanning mFile", file);
            mLastModified = file.lastModified();
            setByteSize(file.length());
            if (DEBUG.RESOURCE) {
                setDebugProperty("file.instance", mFile);
                setDebugProperty("file.modified", new Date(mLastModified));
            }
        }
    }

    /**
     * Set the local file that refers to this resource, if there is one.
     * If mFile is set, mDataFile will always to same.  If this is a packaged
     * resource, mFile will NOT be set, but mDataFile should be set to the package file
     */
    private Object setDataFile(File file, Object type) {
        if (type == FILE_DIRECTORY || (type == FILE_UNKNOWN && file.isDirectory())) {
            if (DEBUG.RESOURCE && DEBUG.META) out("setDataFile: ignoring directory: " + file);
            return FILE_DIRECTORY;
        }
        final String path = file.toString();
        if (path.length() == 3 && Character.isLetter(path.charAt(0)) && path.endsWith(":\\")) {
            if (DEBUG.Enabled) out_info("setDataFile: ignoring Win mount: " + file);
            return FILE_DIRECTORY;
        }
        if (type == FILE_UNKNOWN) {
            if (DEBUG.IO) out("testing " + file);
            if (!file.exists()) {
                try {
                    file = new File(new URI(file.getPath()));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                if (!file.isFile()) {
                    out_warn(TERM_RED + "no such active data file: " + file + TERM_CLEAR);
                    return FILE_UNKNOWN;
                }
            }
        }
        mDataFile = file;
        if (mDataFile != mFile) {
            if (DEBUG.IO) dumpField("scanning mDataFile ", mDataFile);
            setByteSize(mDataFile.length());
            mLastModified = mDataFile.lastModified();
        }
        if (DEBUG.RESOURCE) {
            dumpField("setDataFile", file);
            setDebugProperty("file.data", file);
        }
        return FILE_NORMAL;
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private void loadXMLProperties() {
        if (mXMLpropertyList == null) return;
        for (KVEntry entry : mXMLpropertyList) {
            String key = (String) entry.getKey();
            final Object value = entry.getValue();
            if (DEBUG.Enabled) {
                final String lowKey = key.toLowerCase();
                if (lowKey.startsWith("subject.")) key = "Subject"; else if (lowKey.startsWith("keywords.")) key = "Keywords";
            }
            try {
                if (IMAGE_KEY.equals(key)) {
                    if (DEBUG.RESOURCE) dumpField("processing key", key);
                    setURL_Image((String) value);
                } else if (THUMB_KEY.equals(key)) {
                    if (DEBUG.RESOURCE) dumpField("processing key", key);
                    setURL_Thumb((String) value);
                } else {
                    addProperty(key, value);
                }
            } catch (Throwable t) {
                Log.error(this + "; loadXMLProperties: " + Util.tags(mXMLpropertyList), t);
            }
        }
        mXMLpropertyList = null;
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private void setURL(URL url) {
        if (mURL == url) return;
        mURL = url;
        if (DEBUG.RESOURCE) {
            dumpField("setURL", url);
            setDebugProperty("URL", mURL);
        }
        if (url == null) return;
        if (mFile != null) setFile(null, FILE_UNKNOWN);
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private void parseAndInit() {
        if (spec == SPEC_UNSET) {
            if (targetFilename == null) {
                Log.error(new Throwable("cannot initialize resource " + Util.tags(this) + " without a spec: " + Util.tags(spec)));
                return;
            } else {
                setSpec(targetFilename);
            }
        }
        if (targetFilename == null) {
            Log.error(new Throwable("cannot initialize resource " + Util.tags(this) + " without a target file: " + Util.tags(targetFilename)));
            return;
        }
        if (componentURIString == null) {
            Log.error(new Throwable("cannot initialize resource " + Util.tags(this) + " without a component URI: " + Util.tags(componentURIString)));
            return;
        }
        if (isPackaged()) {
            setDataFile((File) getPropertyValue(PACKAGE_FILE), FILE_UNKNOWN);
            if (mFile != null) Log.warn("mFile != null" + this, new IllegalStateException(toString()));
        } else if (mFile == null && mURL == null) {
            File file = getLocalFileIfPresent(spec);
            if (file != null) {
                setFile(file, FILE_UNKNOWN);
            } else {
                URL url = makeURL(spec);
                if (url != null && !"file".equals(url.getProtocol())) {
                    setURL(url);
                }
            }
        }
        if (getClientType() == Resource.NONE) {
            if (isLocalFile()) {
                if (mFile != null && mFile.isDirectory()) setClientType(Resource.DIRECTORY); else setClientType(Resource.FILE);
            } else if (mURL != null) setClientType(Resource.URL);
        }
        if (getClientType() != Resource.DIRECTORY && !isImage()) {
            if (mFile != null) setAsImage(looksLikeImageFile(mFile.getName())); else setAsImage(looksLikeImageFile(this.spec));
            if (!isImage()) {
                checkForImageType();
            }
        }
        if (isLocalFile()) {
            if (mFile != null) {
                if (isRelative()) {
                    setProperty(USER_FULL_FILE, mFile);
                } else {
                    setProperty(USER_FILE, mFile);
                }
            } else {
                setProperty(USER_FILE, spec);
            }
            removeProperty(USER_URL);
        } else {
            String proto = null;
            if (mURL != null) proto = mURL.getProtocol();
            if (proto != null && (proto.startsWith("http") || proto.equals("ftp"))) {
                setProperty("URL", spec);
                removeProperty(USER_FILE);
            } else {
                if (DEBUG.RESOURCE) {
                    if (!isPackaged()) {
                        setDebugProperty("FileOrURL?", spec);
                        setDebugProperty("URL.proto", proto);
                    }
                }
            }
        }
        if (DEBUG.RESOURCE) {
            setDebugProperty("spec", spec);
            if (mTitle != null) setDebugProperty("title", mTitle);
        }
        if (!hasProperty(CONTENT_TYPE) && mURL != null) setProperty(CONTENT_TYPE, java.net.URLConnection.guessContentTypeFromName(mURL.getPath()));
        if (DEBUG.RESOURCE) {
            out(TERM_GREEN + "final---" + this + TERM_CLEAR);
        }
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private void checkForImageType() {
        if (!isImage()) {
            if (hasProperty(CONTENT_TYPE)) {
                setAsImage(isImageMimeType(getProperty(CONTENT_TYPE)));
            } else {
                setAsImage(looksLikeImageFile('.' + getDataType()));
            }
        }
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private boolean isRelative() {
        return mRelativeURI != null;
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private void setRelativeURI(URI relative) {
        mRelativeURI = relative;
        if (relative != null) {
            setProperty(FILE_RELATIVE, relative);
            setProperty(USER_FILE, getRelativePath());
            setProperty(USER_FULL_FILE, mFile);
        } else {
            removeProperty(FILE_RELATIVE);
            removeProperty(USER_FULL_FILE);
            setProperty(USER_FILE, mFile);
        }
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private String getRelativePath() {
        return mRelativeURI == null ? null : mRelativeURI.getPath();
    }

    /** @return a unique URI for this resource */
    private java.net.URI toAbsoluteURI() {
        if (mFile != null) return toCanonicalFile(mFile).toURI(); else if (mURL != null) return makeURI(mURL); else return makeURI(getSpec());
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private URI findRelativeURI(URI root) {
        final URI absURI = toAbsoluteURI();
        if (root.getScheme() == null || !root.getScheme().equals(absURI.getScheme())) {
            if (DEBUG.RESOURCE) Log.info(this + "; scheme=" + absURI.getScheme() + "; different scheme: " + root + "; can't be relative");
            return null;
        }
        if (!absURI.isAbsolute()) Log.warn("findRelativeURI: non-absolute URI: " + absURI);
        if (DEBUG.RESOURCE) Resource.dumpURI(absURI, "CURRENT ABSOLUTE:");
        final URI relativeURI = root.relativize(absURI);
        if (relativeURI == absURI) {
            return null;
        }
        if (relativeURI != absURI) {
            if (DEBUG.RESOURCE) Resource.dumpURI(relativeURI, "RELATIVE FOUND:");
        }
        if (DEBUG.Enabled) {
            out(TERM_GREEN + "FOUND RELATIVE: " + relativeURI + TERM_CLEAR);
        } else {
            Log.info("found relative to " + root + ": " + relativeURI.getPath());
        }
        return relativeURI;
    }

    /** @return a URI from a string that was known to already be properly encoded as a URI */
    private URI rebuildURI(String s) {
        return URI.create(s);
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private Object getBrowseReference() {
        if (mURL != null) return mURL; else if (mFile != null) return mFile; else if (mDataFile != null) return mDataFile; else return getSpec();
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static boolean isImageMimeType(final String s) {
        return s != null && s.toLowerCase().startsWith("image/");
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static boolean isHtmlMimeType(final String s) {
        return s != null && s.toLowerCase().startsWith("text/html");
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final String UNSET = "<unset-mimeType>";

    /** See tufts.vue.URLResource - reimplementation of private member */
    private String mimeType = UNSET;

    /** Return exactly whatever we were handed at creation time.  We
     * need this because if it's a local file (file: URL or just local
     * file path name), we need whatever the local OS gave us as a
     * reference in order to give that to give back to openURL, as
     * it's the most reliable string to give back to the underlying OS
     * for opening a local file.  */
    public String getSpec() {
        if (this.spec.equals(SPEC_UNSET)) {
            if ((this.getTargetFilename() != null) && (this.getTargetFilename() != "")) {
                return this.getTargetFilename();
            }
        }
        return this.spec;
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static boolean isHTML(final Resource r) {
        String s = r.getSpec().toLowerCase();
        if (s.endsWith(".html") || s.endsWith(".htm")) return true;
        return !s.endsWith(".vue") && isHtmlMimeType(r.getProperty("url.contentType"));
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private URL getThumbshotURL(URL url) {
        if (true) return makeURL(String.format("%s%s://%s/", THUMBSHOT_FETCH, url.getProtocol(), url.getHost())); else return makeURL(THUMBSHOT_FETCH + url);
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static String deco(String s) {
        return "<i><b>" + s + "</b></i>";
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private void invalidateToolTip() {
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private void _scanForMetaData(URL _url) throws java.io.IOException {
        if (DEBUG.Enabled) System.out.println(this + " _scanForMetaData: xml props " + mXMLpropertyList);
        if (DEBUG.Enabled) System.out.println("*** Opening connection to " + _url);
        markAccessAttempt();
        Properties metaData = scrapeHTMLmetaData(_url.openConnection(), 2048);
        if (DEBUG.Enabled) System.out.println("*** Got meta-data " + metaData);
        markAccessSuccess();
        String title = metaData.getProperty("title");
        if (title != null && title.length() > 0) {
            setProperty("title", title);
            title = title.replace('\n', ' ').trim();
            setTitle(title);
        }
        try {
            setByteSize(Integer.parseInt((String) getProperty("contentLength")));
        } catch (Exception e) {
        }
    }

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final Pattern HTML_Title_Regex = Pattern.compile(".*<\\s*title[^>]*>\\s*([^<]+)", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /** See tufts.vue.URLResource - reimplementation of private member */
    private static final Pattern Content_Charset_Regex = Pattern.compile(".*charset\\s*=\\s*([^\">\\s]+)", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /** See tufts.vue.URLResource - reimplementation of private member */
    private Properties scrapeHTMLmetaData(URLConnection connection, int maxSearchBytes) throws java.io.IOException {
        Properties metaData = new Properties();
        InputStream byteStream = connection.getInputStream();
        if (DEBUG.DND && DEBUG.META) {
            System.err.println("Getting headers from " + connection);
            System.err.println("Headers: " + connection.getHeaderFields());
        }
        if (DEBUG.DND) System.err.println("*** getting contentType & encoding...");
        final String contentType = connection.getContentType();
        final String contentEncoding = connection.getContentEncoding();
        final int contentLength = connection.getContentLength();
        if (DEBUG.DND) System.err.println("*** contentType [" + contentType + "]");
        if (DEBUG.DND) System.err.println("*** contentEncoding [" + contentEncoding + "]");
        if (DEBUG.DND) System.err.println("*** contentLength [" + contentLength + "]");
        setProperty("url.contentType", contentType);
        setProperty("url.contentEncoding", contentEncoding);
        if (contentLength >= 0) setProperty("url.contentLength", contentLength);
        if (!isHTML()) {
            if (DEBUG.Enabled) System.err.println("*** contentType [" + contentType + "] not HTML; skipping title extraction");
            return metaData;
        }
        if (DEBUG.DND) System.err.println("*** scanning for HTML meta-data...");
        try {
            final BufferedInputStream bufStream = new BufferedInputStream(byteStream, maxSearchBytes);
            bufStream.mark(maxSearchBytes);
            final byte[] byteBuffer = new byte[maxSearchBytes];
            int bytesRead = 0;
            int len = 0;
            do {
                int max = maxSearchBytes - bytesRead;
                len = bufStream.read(byteBuffer, bytesRead, max);
                System.out.println("*** read " + len);
                if (len > 0) bytesRead += len; else if (len < 0) break;
            } while (len > 0 && bytesRead < maxSearchBytes);
            if (DEBUG.DND) System.out.println("*** Got total chars: " + bytesRead);
            String html = new String(byteBuffer, 0, bytesRead);
            if (DEBUG.DND && DEBUG.META) System.out.println("*** HTML-STRING[" + html + "]");
            String charset = null;
            Matcher cm = Content_Charset_Regex.matcher(html);
            if (cm.lookingAt()) {
                charset = cm.group(1);
                if (DEBUG.DND) System.err.println("*** found HTML specified charset [" + charset + "]");
                setProperty("charset", charset);
            }
            if (charset == null && contentEncoding != null) {
                if (DEBUG.DND || true) System.err.println("*** no charset found: using contentEncoding charset " + contentEncoding);
                charset = contentEncoding;
            }
            final String decodedHTML;
            if (charset != null) {
                bufStream.reset();
                InputStreamReader decodedStream = new InputStreamReader(bufStream, charset);
                if (true || DEBUG.DND) System.out.println("*** decoding bytes into characters with official encoding " + decodedStream.getEncoding());
                setProperty("contentEncoding", decodedStream.getEncoding());
                char[] decoded = new char[bytesRead];
                int decodedChars = decodedStream.read(decoded);
                decodedStream.close();
                if (true || DEBUG.DND) System.err.println("*** " + decodedChars + " characters decoded using " + charset);
                decodedHTML = new String(decoded, 0, decodedChars);
            } else decodedHTML = html;
            byteStream.close();
            bufStream.close();
            Matcher m = HTML_Title_Regex.matcher(decodedHTML);
            if (m.lookingAt()) {
                String title = m.group(1);
                if (true || DEBUG.DND) System.err.println("*** found title [" + title + "]");
                metaData.put("title", title.trim());
            }
        } catch (Throwable e) {
            System.err.println("scrapeHTMLmetaData: " + e);
            if (DEBUG.DND) e.printStackTrace();
        }
        if (DEBUG.DND || DEBUG.Enabled) System.err.println("*** scrapeHTMLmetaData returning [" + metaData + "]");
        return metaData;
    }

    /**
     * A function to return the system spec representing the target file.
     * @return systemSpec, a String containing the path to the target file.
     */
    public String getSystemSpec() {
        Object contentRef = getBrowseReference();
        String systemSpec = contentRef.toString();
        return systemSpec;
    }

    /**
     * A function to find the map we just opened
     * by finding the selected tab index and getting the map
     * at that index.
     * @return the LWMap that we just opened.
     * @Author Helen Oliver
     */
    private LWMap findMapWeJustOpened() {
        LWMap theMap = null;
        MapTabbedPane tabbedPane = VUE.getCurrentTabbedPane();
        int sel = -1;
        sel = tabbedPane.getSelectedIndex();
        if (sel >= 0) theMap = tabbedPane.getMapAt(sel);
        return theMap;
    }

    /**
     * A method to move the screen to the target component.
     * @param theMap, the target LWMap
     * @param theComponent, the target LWComponent
     * @author Helen Oliver
     */
    private void moveScreenToTargetComponent(LWMap theMap, LWComponent theComponent) {
        if ((theMap == null) || (theComponent == null)) return;
        MapViewer viewer = VUE.getCurrentTabbedPane().getViewerWithMap(theMap);
        double dx = theComponent.getLocation().getX();
        double dy = theComponent.getLocation().getY();
        Double doubleX = new Double(dx);
        Double doubleY = new Double(dy);
        int x = doubleX.intValue();
        int y = doubleY.intValue();
        viewer.screenToMapPoint(x, y);
    }

    /**
     * A function to select the target component.
     * @author Helen Oliver
     */
    private void selectTargetComponent(boolean bSameMap) {
        LWMap theMap = findMapWeJustOpened();
        LWComponent theComponent = null;
        if (theMap != null) theComponent = theMap.findChildByURIString(componentURIString);
        if (theComponent != null) {
            MapViewer theViewer = VUE.getCurrentTabbedPane().getViewerWithMap(theMap);
            theViewer.setTargetComponent(theComponent);
            theViewer.selectionClearWormhole();
            theViewer.selectionAddWormhole(theComponent);
            if (bSameMap) {
                theViewer.selectionSet(theComponent);
                theViewer.repaintSelection();
            }
        }
    }

    /**
	 * @param stripThis, a String representing a filename that has its spaces
	 * in the HTML format
	 * @return the same String, html space codes replaced with single spaces
	 */
    private String stripHtmlSpaceCodes(String stripThis) {
        String strStripped = "";
        String strPeskySpace = "%20";
        String strCleanSpace = " ";
        strStripped = stripThis.replaceAll(strPeskySpace, strCleanSpace);
        return strStripped;
    }

    /**
	 * Calls a function to use string manipulation to figure out
	 * whether the originating file path and the target spec
	 * actually point to the same map.
	 * @return true if they point to the same map,
	 * false otherwise.
	 * @author Helen Oliver
	 */
    private boolean pointsToSameMap() {
        boolean bSameMap = false;
        String strSpec = this.getSystemSpec();
        String strOriginatingFile = this.getOriginatingFilename();
        if (strSpec.equals(SPEC_UNSET)) strSpec = this.getTargetFilename();
        bSameMap = VueUtil.pointsToSameMap(strSpec, strOriginatingFile);
        return bSameMap;
    }

    /**
     * reimplementation of URLResource.displayContent()
     * This one, after opening a Map, also has to find the target
     * component and focus on that
     * @author Helen Oliver
     */
    public void displayContent() {
        final Object contentRef = getBrowseReference();
        out("displayContent: " + Util.tags(contentRef));
        String strParentPath = getParentPathOfActiveMap();
        URI sourceMapURI = getParentURIOfActiveMap();
        String strSourceName = getFilenameOfActiveMap();
        String strDecodedSpec = VueUtil.decodeURIStringToString(contentRef.toString());
        strDecodedSpec = VueUtil.switchSlashDirection(strParentPath, strDecodedSpec);
        final String systemSpec = strDecodedSpec;
        File lastKnownFile = new File(systemSpec);
        File targFile = null;
        String strTargetName = lastKnownFile.getName();
        File fileForName = new File(systemSpec);
        String strFileName = fileForName.getName();
        try {
            markAccessAttempt();
            boolean bSameMap = pointsToSameMap();
            LWMap targMap = null;
            if (!bSameMap) {
                try {
                    if ((lastKnownFile != null) && (lastKnownFile.isFile())) {
                        targMap = VueUtil.checkIfMapContainsTargetNode(lastKnownFile, getComponentURIString());
                        if (targMap != null) {
                            recordRelativizedSpecChange(lastKnownFile, sourceMapURI);
                            VueUtil.openURL(lastKnownFile.toString());
                        }
                    }
                    URI systemSpecURI = VueUtil.getURIFromString(systemSpec);
                    targFile = VueUtil.resolveTargetRelativeToSource(systemSpecURI, sourceMapURI);
                    if ((targFile != null) && (targFile.isFile())) {
                        lastKnownFile = targFile;
                        targMap = VueUtil.checkIfMapContainsTargetNode(lastKnownFile, getComponentURIString());
                        if (targMap != null) {
                            recordRelativizedSpecChange(lastKnownFile, sourceMapURI);
                            VueUtil.openURL(lastKnownFile.toString());
                        }
                    }
                    try {
                        if (((lastKnownFile != null) && (!lastKnownFile.isFile())) || (targMap == null)) {
                            if ((strParentPath != null) && (strParentPath != "")) {
                                targFile = new File(strParentPath, strTargetName);
                                if (targFile.isFile()) {
                                    targMap = VueUtil.checkIfMapContainsTargetNode(targFile, getComponentURIString());
                                    if (targMap != null) {
                                        recordRelativizedSpecChange(targFile, sourceMapURI);
                                        VueUtil.openURL(targFile.toString());
                                    } else {
                                        if ((lastKnownFile != null) && (!lastKnownFile.isFile())) lastKnownFile = targFile;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                    if ((lastKnownFile != null) && (!lastKnownFile.isFile())) {
                        targFile = VueUtil.lazyFindTargetInSubfolders(strParentPath, strParentPath, strTargetName);
                        if ((targFile != null) && (targFile.isFile())) {
                            lastKnownFile = targFile;
                            recordRelativizedSpecChange(lastKnownFile, sourceMapURI);
                            VueUtil.openURL(lastKnownFile.toString());
                        }
                    }
                    if ((lastKnownFile != null) && (!lastKnownFile.isFile())) {
                        targFile = VueUtil.lazyFindTargetAboveCurrentPath(strParentPath, strParentPath, strTargetName);
                        if ((targFile != null) && (targFile.isFile())) {
                            lastKnownFile = targFile;
                            recordRelativizedSpecChange(lastKnownFile, sourceMapURI);
                            VueUtil.openURL(lastKnownFile.toString());
                        }
                    }
                    if ((lastKnownFile != null) && (!lastKnownFile.isFile())) {
                        VueUtil.alert("Can't find the file " + strTargetName + ".\n" + "Try the Refresh command from the File menu.", "Target File Not Found");
                    } else if (((lastKnownFile != null) && (lastKnownFile.isFile())) && (targMap == null)) {
                        recordRelativizedSpecChange(lastKnownFile, sourceMapURI);
                        VueUtil.alert("Can't find " + systemSpec + " with the target node in the expected location.\n" + "Opening the nearest file found with the name " + strTargetName + ".", "Target Node Not Found");
                        VueUtil.openURL(lastKnownFile.toString());
                    }
                } catch (IOException e) {
                    System.out.println("Gotcha");
                }
            }
            selectTargetComponent(bSameMap);
            markAccessSuccess();
        } catch (Throwable t) {
            Log.error(systemSpec + "; " + t);
        }
        tufts.vue.gui.VueFrame.setLastOpenedResource(this);
    }

    /**
     * A function to check whether a map is already open.
     * @param targMap, the LWMap to check for.
     * @return true if the map is already open, false otherwise.
     * @author Helen Oliver, Imperial College London
     */
    private static boolean isMapAlreadyOpen(LWMap targMap) {
        if (targMap == null) return false;
        boolean bOpen = false;
        File targFile = targMap.getFile();
        String targPath = "";
        if (targFile != null) {
            targPath = targFile.getAbsolutePath();
        }
        Collection<LWMap> coll = VUE.getAllMaps();
        for (LWMap map : coll) {
            if ((!map.equals(targMap)) && (targFile != null)) {
                File theFile = map.getFile();
                if (theFile != null) {
                    String theFileName = theFile.getAbsolutePath();
                    if (theFileName.equals(targPath)) {
                        bOpen = true;
                        break;
                    }
                }
            }
        }
        return bOpen;
    }

    /**
     * A function to relativize a changed spec
     * and record the change.
     * @param theFile, the File object to relativize
     * @param sourceMapURI, the URI of the source map against
     * which the file parameter is to be relativized
     * @author Helen Oliver
     */
    private void recordRelativizedSpecChange(File theFile, URI sourceMapURI) {
        if ((theFile == null) || (sourceMapURI == null)) return;
        String strRelativeSpec = relativizeTargetSpec(theFile, sourceMapURI);
        super.setSpec(strRelativeSpec);
        this.setSpec(strRelativeSpec);
    }

    /**
	 * A function to take a possibly-absolute path for the target map,
	 * and relativize it to the source map.
	 * @param theTargetFile, the File of the target map
	 * @param sourceMapURI, the URI of the source map
	 * @return a String representing the relative path
	 * @author Helen Oliver
	 */
    private String relativizeTargetSpec(File theTargetFile, URI sourceMapURI) {
        if (theTargetFile == null) return "";
        if (sourceMapURI == null) return "";
        File theSourceFile = new File(VueUtil.getStringFromURI(sourceMapURI));
        String strParentPath = theSourceFile.getParent();
        URI parentURI = null;
        try {
            parentURI = new URI(VueUtil.encodeStringForURI(strParentPath));
        } catch (URISyntaxException e) {
            return "";
        }
        String strTargetSpec = theTargetFile.toString();
        String strRelativizedSpec = VueUtil.relativizeUnknownTargetSpec(strParentPath, parentURI, strTargetSpec);
        return strRelativizedSpec;
    }

    /**
	 * A function to get the filename of the parent path of the active map.
	 * @return the String of the filename of the active map.
	 * @author Helen Oliver
	 */
    private String getFilenameOfActiveMap() {
        String strActiveName = "";
        File activeMapFile = VUE.getActiveMap().getFile();
        if (activeMapFile == null) return null;
        strActiveName = activeMapFile.getName();
        if ((strActiveName != null) && (strActiveName != "")) {
            strActiveName = VueUtil.decodeURIStringToString(strActiveName);
        }
        return strActiveName;
    }

    /**
	 * A function to get the STring of the parent path of the active map.
	 * @return the String of the parent path of the active map.
	 * @author Helen Oliver
	 */
    private String getParentPathOfActiveMap() {
        String strActiveParent = "";
        File activeMapFile = VUE.getActiveMap().getFile();
        if (activeMapFile == null) return null;
        strActiveParent = activeMapFile.getParent();
        if ((strActiveParent != null) && (strActiveParent != "")) {
            strActiveParent = VueUtil.decodeURIStringToString(strActiveParent);
        }
        return strActiveParent;
    }

    /**
	 * A function to get the URI of the parent path of the active map.
	 * @return the URI of the parent path of the active map.
	 * @author Helen Oliver
	 */
    private URI getParentURIOfActiveMap() {
        String strActiveParent = "";
        URI activeParent = null;
        File activeMapFile = VUE.getActiveMap().getFile();
        if (activeMapFile == null) return null;
        strActiveParent = activeMapFile.getParent();
        if ((strActiveParent != null) && (strActiveParent != "")) {
            activeParent = VueUtil.getURIFromString(strActiveParent);
        }
        return activeParent;
    }

    /**
	 * A function to get the URI of the parent path of the source map.
	 * @return the URI of the parent path of the source map.
	 * @author Helen Oliver
	 */
    private URI getParentURIOfSourceMap() {
        String strSourceParent = "";
        URI sourceParent = null;
        strSourceParent = new File(getOriginatingFilename()).getParent();
        if ((strSourceParent != null) && (strSourceParent != "")) {
            strSourceParent = VueUtil.encodeStringForURI(strSourceParent);
        }
        return sourceParent;
    }

    /**
     * A function to set the target file.
     * @param targetFile, the target file in this wormhole.
     * @author Helen Oliver
     */
    public void setTargetFilename(File targetFile) {
        targetFilename = targetFile.getAbsolutePath();
    }

    /**
     * A function to set the absolute path of the target file.
     * @param targetFilename, the target file in this wormhole.
     * @author Helen Oliver
     */
    public void setTargetFilename(String theTargetFilename) {
        targetFilename = theTargetFilename;
    }

    /**
     * A function to return the absolute path of the target file.
     * @return targetFilename, the absolute path of the target file in
     * this wormhole.
     * @author Helen Oliver
     */
    public String getTargetFilename() {
        return targetFilename;
    }

    /**
     * A function to set the URI string for the target component.
     * @param theComponentURIString, the URI String for the LWComponent we want to focus on
     * once we've opened the map.
     * @author Helen Oliver
     */
    public void setComponentURIString(String theComponentURIString) {
        componentURIString = theComponentURIString;
    }

    /**
     * A function to return the URI string for the target component.
     * @return componentURIString, the URI String for the LWComponent we want to focus on
     * once we've opened the map.
     * @author Helen Oliver
     */
    public String getComponentURIString() {
        return componentURIString;
    }

    /**
     * A function to set the absolute path of the originating file
     * from the File object itself.
     * @param originatingFile, the originating file in this wormhole.
     * @author Helen Oliver
     */
    public void setOriginatingFilename(File originatingFile) {
        originatingFilename = originatingFile.getAbsolutePath();
    }

    /**
     * A function to set the absolute path of the source file.
     * @param originatingFilename, a String representing the absolute path of the source file.
     * @author Helen Oliver
     */
    public void setOriginatingFilename(String theOriginatingFilename) {
        originatingFilename = theOriginatingFilename;
    }

    /**
     * A function to return the absolute path of the source file.
     * @return originatingFilename, a String representing the absolute path of the source file in
     * this wormhole.
     * @author Helen Oliver
     */
    public String getOriginatingFilename() {
        return originatingFilename;
    }

    /**
     * A function to set the URI string for the target component.
     * @param theComponentURIString, the URI String for the LWComponent we want to focus on
     * once we've opened the originating map.
     * @author Helen Oliver
     */
    public void setOriginatingComponentURIString(String theComponentURIString) {
        originatingComponentURIString = theComponentURIString;
    }

    /**
     * A function to return the URI string for the target component.
     * @return originatingComponentURIString, the URI String for the LWComponent we want to focus on
     * once we've opened the map.
     * @author Helen Oliver
     */
    public String getOriginatingComponentURIString() {
        return originatingComponentURIString;
    }

    public void setSpec(final String newSpec) {
        if ((DEBUG.RESOURCE || DEBUG.WORK) && this.spec != SPEC_UNSET) {
            out("setSpec; already set: replacing " + Util.tags(this.spec) + " " + Util.tag(spec) + " with " + Util.tags(newSpec) + " " + Util.tag(newSpec));
        }
        if (DEBUG.RESOURCE) dumpField(TERM_CYAN + "setSpec------------------------" + TERM_CLEAR, newSpec);
        if (newSpec == null) throw new IllegalArgumentException(Util.tags(this) + "; setSpec: null value");
        if (SPEC_UNSET.equals(newSpec)) {
            this.spec = SPEC_UNSET;
            return;
        }
        this.spec = newSpec;
        reset();
        if (!mRestoreUnderway) parseAndInit();
    }

    @Override
    protected void initFinal(Object context) {
        if (DEBUG.RESOURCE) out("initFinal in " + context);
        parseAndInit();
    }

    @Override
    public void restoreRelativeTo(URI root) {
        try {
            String relative = getProperty(FILE_RELATIVE_OLD);
            if (relative == null) {
                relative = getProperty(FILE_RELATIVE);
                if (relative == null) {
                    return;
                }
            } else {
                removeProperty(FILE_RELATIVE_OLD);
                setProperty(FILE_RELATIVE, relative);
            }
            System.out.println("Component URI string is: " + getComponentURIString());
            String currentSpec = this.getTargetFilename();
            if ((currentSpec == null) || (currentSpec == "")) return;
            final URI relativeURI = rebuildURI(relative);
            final URI absoluteURI = root.resolve(relativeURI);
            final URI fixedAbsoluteURI = new URI(currentSpec);
            if (DEBUG.RESOURCE) {
                System.out.print(TERM_PURPLE);
                Resource.dumpURI(fixedAbsoluteURI, "fixed absolute:");
                Resource.dumpURI(absoluteURI, "resolved absolute:");
                Resource.dumpURI(relativeURI, "from relative:");
                System.out.print(TERM_CLEAR);
            }
            if (fixedAbsoluteURI != null) {
                final File file = new File(fixedAbsoluteURI);
                final File relativeFile = new File(absoluteURI);
                if (file.canRead()) {
                    if (DEBUG.RESOURCE) setDebugProperty("relative URI", relativeURI);
                    Log.info(TERM_PURPLE + "resolved " + relativeURI.getPath() + " to: " + file + TERM_CLEAR);
                    setRelativeURI(relativeURI);
                    setSpecByFile(file);
                } else if (relativeFile.canRead()) {
                    if (DEBUG.RESOURCE) setDebugProperty("relative URI", relativeURI);
                    Log.info(TERM_PURPLE + "resolved " + relativeURI.getPath() + " to: " + file + TERM_CLEAR);
                    setRelativeURI(relativeURI);
                    setSpecByFile(relativeFile);
                } else {
                    out_warn(TERM_RED + "can't find data relative to " + root + " at " + relative + "; can't read " + file + TERM_CLEAR);
                }
            } else {
                out_error("failed to find relative " + relative + "; in " + root + " for " + this);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void recordRelativeTo(URI root) {
        setRelativeURI(findRelativeURI(root));
    }

    public String getRelativeURI() {
        if (mRelativeURI != null) return mRelativeURI.toString(); else return null;
    }
}
