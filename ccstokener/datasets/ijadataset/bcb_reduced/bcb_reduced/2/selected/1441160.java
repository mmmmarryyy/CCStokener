package tufts.vue;

import java.util.*;
import tufts.Util;
import static tufts.Util.*;
import tufts.vue.gui.GUI;
import java.net.*;
import java.awt.Image;
import java.io.*;
import java.util.regex.*;

/**
 * The Resource impl handles references to local files or single URL's, as well as
 * any underlying type of asset (OSID or not) that can obtain it's various parts via URL's.
 *
 * An "asset" is defined very generically as anything, that given some kind basic
 * key/meta-data (e.g., a file name, a URL, etc), can at some later point,
 * reliably and repeatably convert that name/key to underlyling data of interest.
 * This is basically what the Resource interface was created to handle.
 *
 * An "Asset" is a proper org.osid.repository.Asset.
 *
 * When this class is used for an asset with parts (e..g, Osid2AssetResource), it should
 * also be what allows us to completely throw away any underlying
 * org.osid.repository.Asset (using it only as a paramatizer for what is really a
 * factory constructor: should covert to that), because all the assets can be had via
 * URL's, and we've extracted the relvant information at construction time.  If the
 * asset part(s) CANNOT be accessed via URL, then we need a real, new subclass of
 * URLResource that handles the non URL cases, or even just a raw implementor of
 * Resource, if all the asset-parts need special I/O (e.g., non HTTP network traffic),
 * to be obtained.
 *
 * @version $Revision: 1.91 $ / $Date: 2010-02-03 19:17:40 $ / $Author: mike $
 */
public class URLResource extends Resource implements XMLUnmarshalListener {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(URLResource.class);

    private static final String IMAGE_KEY = HIDDEN_PREFIX + "Image";

    private static final String THUMB_KEY = HIDDEN_PREFIX + "Thumb";

    private static final String USER_URL = "URL";

    private static final String USER_FILE = "File";

    private static final String USER_DIRECTORY = "Directory";

    private static final String USER_FULL_FILE = RUNTIME_PREFIX + "Full File";

    private static final String FILE_RELATIVE = HIDDEN_PREFIX + "file.relative";

    private static final String FILE_RELATIVE_OLD = "file.relative";

    private static final String FILE_CANONICAL = HIDDEN_PREFIX + "file.canonical";

    /**
     * The most generic version of what we refer to.  Usually set to a full URL or absolute file path.
     * Note that this may have been set on a different platform that we're currently running on,
     * so it may no longer make a valid URL or File on this platform, which is why we need
     * this generic String version of it, and why the Resource/URLResource code can be so complicated.
     */
    private String spec = SPEC_UNSET;

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

    private boolean mRestoreUnderway = false;

    private ArrayList<PropertyEntry> mXMLpropertyList;

    static URLResource create(String spec) {
        return new URLResource(spec);
    }

    static URLResource create(URL url) {
        return new URLResource(url.toString());
    }

    static URLResource create(URI uri) {
        return new URLResource(uri.toString());
    }

    static URLResource create(File file) {
        return new URLResource(file);
    }

    private URLResource(String spec) {
        init();
        setSpec(spec);
    }

    private URLResource(File file) {
        init();
        setSpecByFile(file);
    }

    /**
     * @deprecated - This constructor needs to be public to support castor persistance ONLY -- it should not
     * be called directly by any code.
     */
    public URLResource() {
        init();
    }

    private void init() {
        if (DEBUG.RESOURCE) {
            String iname = getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
            setDebugProperty("0INSTANCE", iname);
        }
    }

    private static final String FILE_DIRECTORY = "directory";

    private static final String FILE_NORMAL = "file";

    private static final String FILE_UNKNOWN = "unknown";

    protected void setSpecByKnownFile(File file, boolean isDir) {
        setSpecByFile(file, isDir ? FILE_DIRECTORY : FILE_NORMAL);
    }

    protected void setSpecByFile(File file) {
        setSpecByFile(file, FILE_UNKNOWN);
    }

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

    private long mLastModified;

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
                out_warn(TERM_RED + "no such active data file: " + file + TERM_CLEAR);
                return FILE_UNKNOWN;
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

    /** for use by tufts.vue.action.Archive */
    public void setPackageFile(File packageFile, File archiveFile) {
        if (DEBUG.RESOURCE) dumpField("setPackageFile", packageFile);
        reset();
        setURL(null);
        setFile(null, FILE_UNKNOWN);
        setProperty(PACKAGE_FILE, packageFile);
        removeProperty(USER_FILE);
        setProperty(PACKAGE_ARCHIVE, archiveFile);
        setCached(true);
    }

    @Override
    public void reset() {
        super.reset();
        invalidateToolTip();
    }

    public final void XML_setSpec(final String XMLspec) {
        if (DEBUG.RESOURCE) dumpField("XML_setSpec", XMLspec);
        this.spec = XMLspec;
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

    public void XML_completed(Object context) {
        mRestoreUnderway = false;
        if (context != MANAGED_UNMARSHALLING) {
            if (DEBUG.RESOURCE && DEBUG.META) out("XML_completed: unmanaged (immediate) init in context " + Util.tags(context) + "; " + this);
            initAfterDeserialize(context);
            initFinal(context);
            if (DEBUG.RESOURCE) out("XML_completed");
        } else {
            if (DEBUG.RESOURCE && DEBUG.META) out("XML_completed; delayed init");
        }
    }

    @Override
    protected void initAfterDeserialize(Object context) {
        loadXMLProperties();
    }

    @Override
    protected void initFinal(Object context) {
        if (DEBUG.RESOURCE) out("initFinal in " + context);
        parseAndInit();
    }

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

    @Override
    protected String extractExtension() {
        if (mURL != null) return super.extractExtension(mURL.getPath()); else return super.extractExtension();
    }

    private void parseAndInit() {
        if (spec == SPEC_UNSET) {
            Log.error(new Throwable("cannot initialize resource " + Util.tags(this) + " without a spec: " + Util.tags(spec)));
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
                if (url != null && !"file".equals(url.getProtocol())) setURL(url);
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

    private void checkForImageType() {
        if (!isImage()) {
            if (hasProperty(CONTENT_TYPE)) {
                setAsImage(isImageMimeType(getProperty(CONTENT_TYPE)));
            } else {
                setAsImage(looksLikeImageFile('.' + getDataType()));
            }
        }
    }

    public boolean isJpegFile(String s) throws IOException {
        URI url;
        try {
            url = new URI(s);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
        URLConnection conn = url.toURL().openConnection();
        InputStream in = new BufferedInputStream(conn.getInputStream());
        try {
            return (in.read() == 'J' && in.read() == 'F' && in.read() == 'I' && in.read() == 'F');
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {
            }
        }
    }

    public boolean isGifFile(String s) {
        URI url;
        try {
            url = new URI(s);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
        URLConnection conn = null;
        try {
            conn = url.toURL().openConnection();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        InputStream in = null;
        try {
            in = new BufferedInputStream(conn.getInputStream());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            return (in.read() == 'G' && in.read() == 'I' && in.read() == 'F');
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {
            }
        }
        return false;
    }

    private boolean isRelative() {
        return mRelativeURI != null;
    }

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

    private String getRelativePath() {
        return mRelativeURI == null ? null : mRelativeURI.getPath();
    }

    @Override
    public void recordRelativeTo(URI root) {
        setRelativeURI(findRelativeURI(root));
    }

    /** @return a unique URI for this resource */
    private java.net.URI toAbsoluteURI() {
        if (mFile != null) return toCanonicalFile(mFile).toURI(); else if (mURL != null) return makeURI(mURL); else return makeURI(getSpec());
    }

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

    @Override
    public void restoreRelativeTo(URI root) {
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
        final URI relativeURI = rebuildURI(relative);
        final URI absoluteURI = root.resolve(relativeURI);
        if (DEBUG.RESOURCE) {
            System.out.print(TERM_PURPLE);
            Resource.dumpURI(absoluteURI, "resolved absolute:");
            Resource.dumpURI(relativeURI, "from relative:");
            System.out.print(TERM_CLEAR);
        }
        if (absoluteURI != null) {
            final File file = new File(absoluteURI);
            if (file.canRead()) {
                if (DEBUG.RESOURCE) setDebugProperty("relative URI", relativeURI);
                Log.info(TERM_PURPLE + "resolved " + relativeURI.getPath() + " to: " + file + TERM_CLEAR);
                setRelativeURI(relativeURI);
                setSpecByFile(file);
            } else {
                out_warn(TERM_RED + "can't find data relative to " + root + " at " + relative + "; can't read " + file + TERM_CLEAR);
            }
        } else {
            out_error("failed to find relative " + relative + "; in " + root + " for " + this);
        }
    }

    /**
     * This impl will return true the FIRST time after the data has changed,
     * and subsequent calls will return false, until the data changes again.
     * This currently only monitors local disk resources (e.g., not web resources).
     */
    @Override
    public boolean dataHasChanged() {
        final File file;
        if (mDataFile != null) file = mDataFile; else file = mFile;
        if (file != null) {
            if (DEBUG.Enabled || DEBUG.IO) dumpField("re-scanning", file);
            final long curLastMod = file.lastModified();
            final long curSize = file.length();
            if (curLastMod != mLastModified || curSize != getByteSize()) {
                if (DEBUG.Enabled) {
                    long diff = curLastMod - mLastModified;
                    out_info(TERM_CYAN + Util.tags(file) + "; lastMod=" + new Date(curLastMod) + "; timeDelta=" + (diff / 100) + " seconds" + "; sizeDelta=" + (curSize - getByteSize()) + " bytes" + TERM_CLEAR);
                }
                mLastModified = curLastMod;
                if (curSize != getByteSize()) setByteSize(curSize);
                return true;
            }
        }
        return false;
    }

    @Override
    public String getLocationName() {
        File archive = null;
        try {
            archive = (File) getPropertyValue(PACKAGE_ARCHIVE);
        } catch (Throwable t) {
            Log.warn(this, t);
        }
        if (archive == null) {
            if (isRelative()) return getRelativePath(); else return getSpec();
        } else if (hasProperty(USER_URL)) {
            return getProperty(USER_URL);
        } else {
            final String name;
            if (mDataFile != null) name = mDataFile.getName(); else if (mTitle != null) name = mTitle; else name = getSpec();
            return String.format("%s(%s)", archive.getName(), name);
        }
    }

    /** @see tufts.vue.Resource */
    @Override
    public Object getImageSource() {
        if (mDataFile != null) {
            return mDataFile;
        } else if (mURL_ImageData != null) {
            return mURL_ImageData;
        } else {
            if (mURL == null && getClientType() != NONE) {
                if (DEBUG.RESOURCE) {
                    Log.warn("mURL == null, likely missing file.", new Throwable(toString()));
                } else {
                    Log.warn("mURL == null, likely missing file: " + this);
                }
            }
            return mURL;
        }
    }

    @Override
    public int hashCode() {
        return spec == null ? super.hashCode() : spec.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof Resource) {
            if (spec == SPEC_UNSET || spec == null) return false;
            final String spec2 = ((Resource) o).getSpec();
            if (spec2 == SPEC_UNSET || spec2 == null) return false;
            return spec.equals(spec2);
        }
        return false;
    }

    private Object getBrowseReference() {
        if (mURL != null) return mURL; else if (mFile != null) return mFile; else if (mDataFile != null) return mDataFile; else return getSpec();
    }

    public void displayContent() {
        final Object contentRef = getBrowseReference();
        out("displayContent: " + Util.tags(contentRef));
        final String systemSpec = contentRef.toString();
        try {
            markAccessAttempt();
            VueUtil.openURL(systemSpec);
            markAccessSuccess();
        } catch (Throwable t) {
            Log.error(systemSpec + "; " + t);
        }
        tufts.vue.gui.VueFrame.setLastOpenedResource(this);
    }

    public void setTitle(String title) {
        if (mTitle == title) return;
        mTitle = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(title);
        if (DEBUG.RESOURCE) {
            dumpField("setTitle", title);
            if (hasProperty(DEBUG_PREFIX + "title")) setDebugProperty("title", title);
        }
    }

    public String getTitle() {
        return mTitle;
    }

    /** Return exactly whatever we were handed at creation time.  We
     * need this because if it's a local file (file: URL or just local
     * file path name), we need whatever the local OS gave us as a
     * reference in order to give that to give back to openURL, as
     * it's the most reliable string to give back to the underlying OS
     * for opening a local file.  */
    public String getSpec() {
        return this.spec;
    }

    public String getRelativeURI() {
        return null;
    }

    /** persistance only */
    public void setRelativeURI(String s) {
    }

    /** this is only meaninful if this resource points to a local file */
    protected Image getFileIconImage() {
        return GUI.getSystemIconForExtension(getDataType(), 128);
    }

    @Override
    public boolean isLocalFile() {
        return mFile != null || (mURL != null && "file".equals(mURL.getProtocol()));
    }

    /** @deprecated */
    public void setProperties(Properties p) {
        tufts.Util.printStackTrace("URLResource.setProperties: deprecated " + p);
    }

    /** this is for castor persistance only */
    public java.util.List getPropertyList() {
        if (mRestoreUnderway == false) {
            if (mProperties.size() == 0) return null;
            mXMLpropertyList = new ArrayList(mProperties.size());
            for (Map.Entry e : mProperties.entries()) mXMLpropertyList.add(new PropertyEntry(e));
        }
        if (DEBUG.CASTOR) System.out.println(this + " getPropertyList " + mXMLpropertyList);
        return mXMLpropertyList;
    }

    public void XML_initialized(Object context) {
        if (DEBUG.CASTOR) System.out.println(getClass() + " XML INIT");
        mRestoreUnderway = true;
        mXMLpropertyList = new ArrayList();
    }

    public void XML_fieldAdded(Object context, String name, Object child) {
        if (DEBUG.XML) out("XML_fieldAdded <" + name + "> = " + child);
    }

    public void XML_addNotify(Object context, String name, Object parent) {
        if (DEBUG.CASTOR) System.out.println(this + " XML ADDNOTIFY as \"" + name + "\" to parent " + parent);
    }

    private static boolean isImageMimeType(final String s) {
        return s != null && s.toLowerCase().startsWith("image/");
    }

    private static boolean isHtmlMimeType(final String s) {
        return s != null && s.toLowerCase().startsWith("text/html");
    }

    private static final String UNSET = "<unset-mimeType>";

    private String mimeType = UNSET;

    @Override
    protected String determineDataType() {
        final String spec = getSpec();
        if (spec.endsWith("=jpeg")) {
            return "jpeg";
        } else if (mimeType != UNSET) {
            return mimeType;
        } else if (spec != SPEC_UNSET && spec.startsWith("http") && spec.contains("fedora")) {
            if (spec.endsWith("bdef:AssetDef/getFullView/")) {
                return "html";
            } else {
                String type = getProperty(CONTENT_TYPE);
                if (type == null || type.length() < 1) {
                    try {
                        final URL url = (mURL != null ? mURL : new URL(getSpec()));
                        if (DEBUG.Enabled) out("polling actual HTTP server for content-type: " + url);
                        if (!VUE.isApplet()) type = url.openConnection().getHeaderField("Content-type"); else type = null;
                        if (DEBUG.Enabled) {
                            out("got contentType " + url + " [" + type + "]");
                        }
                        if (type != null && type.length() > 0) setProperty(CONTENT_TYPE, type);
                    } catch (Throwable t) {
                        Log.error("content-type-fetch: " + this, t);
                    }
                }
                if (type != null && type.contains("/")) {
                    mimeType = type.split("/")[1];
                    if (mimeType.indexOf(';') > 0) {
                        mimeType = mimeType.substring(0, mimeType.indexOf(';'));
                    }
                    return mimeType;
                }
            }
        }
        return super.determineDataType();
    }

    private static boolean isHTML(final Resource r) {
        String s = r.getSpec().toLowerCase();
        if (s.endsWith(".html") || s.endsWith(".htm")) return true;
        return !s.endsWith(".vue") && isHtmlMimeType(r.getProperty("url.contentType"));
    }

    public boolean isHTML() {
        if (isImage()) return false; else return isHTML(this);
    }

    /** Currently, this just calls setSpec -- the "browse" URL is the default URL */
    protected void setURL_Browse(String s) {
        if (DEBUG.RESOURCE) dumpField("setURL_Browse", s);
        setSpec(s);
    }

    public void setURL_Thumb(String s) {
        if (DEBUG.RESOURCE) dumpField("setURL_Thumb", s);
        mURL_ThumbData = makeURL(s);
        setProperty(THUMB_KEY, mURL_ThumbData);
    }

    /** If given any valid URL, this resource will consider itself image content, no matter
     * what's at the other end of that URL, so care should be taken to ensure it's
     * valid image data (as opposed to say, an HTML page)
     */
    protected void setURL_Image(String s) {
        if (DEBUG.RESOURCE) dumpField("setURL_Image", s);
        mURL_ImageData = makeURL(s);
        setProperty(IMAGE_KEY, mURL_ImageData);
        if (mURL_ImageData != null) setAsImage(true);
    }

    /**
     * Either immediately return an Image object if available, otherwise return an
     * object that is some kind of valid image source (e.g., a URL or image Resource)
     * that can be fed to Images.getImage and fetch asynchronously w/callbacks if it
     * isn't already in the cache.
     */
    public Object getPreview() {
        if (isCached() && isImage()) return this; else if (mURL_ThumbData != null) return mURL_ThumbData; else if (mURL_ImageData != null) return mURL_ImageData; else if (isImage()) return this; else if (isLocalFile() || getClientType() == Resource.FILE || getClientType() == Resource.DIRECTORY) {
            return getFileIconImage();
        } else if (mURL != null && !isLocalFile()) {
            if (mURL.toString().toLowerCase().endsWith(VueUtil.VueExtension)) return VueResources.getBufferedImage("vueIcon64x64"); else return getThumbshotURL(mURL);
        } else return null;
    }

    public static final String THUMBSHOT_FETCH = "http://open.thumbshots.org/image.pxf?url=";

    private URL getThumbshotURL(URL url) {
        if (true) return makeURL(String.format("%s%s://%s/", THUMBSHOT_FETCH, url.getProtocol(), url.getHost())); else return makeURL(THUMBSHOT_FETCH + url);
    }

    /** @deprecated -- for backward compat with lw_mapping_1.0.xml only, where this never worked */
    public void setPropertyList(Vector propertyList) {
        Log.info("IGNORING OLD SAVE FILE DATA " + propertyList + " for " + this);
    }

    private static String deco(String s) {
        return "<i><b>" + s + "</b></i>";
    }

    private void invalidateToolTip() {
    }

    public void scanForMetaDataAsync(final LWComponent c) {
        scanForMetaDataAsync(c, false);
    }

    public void scanForMetaDataAsync(final LWComponent c, final boolean setLabelFromTitle) {
        new Thread("VUE-URL-MetaData") {

            public void run() {
                scanForMetaData(c, setLabelFromTitle);
            }
        }.start();
    }

    void scanForMetaData(final LWComponent c, boolean setLabelFromTitle) {
        if (true) {
            return;
        }
        URL _url = mURL;
        if (_url == null) {
            if (DEBUG.Enabled) out("couldn't get URL");
            return;
        }
        final boolean forceTitleToLabel = setLabelFromTitle || c.getLabel() == null || c.getLabel().equals(mTitle) || c.getLabel().equals(getSpec());
        try {
            _scanForMetaData(_url);
        } catch (Throwable t) {
            Log.info(_url + ": meta-data extraction failed: " + t);
            if (DEBUG.Enabled) tufts.Util.printStackTrace(t, _url.toString());
        }
        if (forceTitleToLabel && getTitle() != null) c.setLabel(getTitle());
        if (DEBUG.Enabled) out("properties " + mProperties);
    }

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

    private static final Pattern HTML_Title_Regex = Pattern.compile(".*<\\s*title[^>]*>\\s*([^<]+)", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern Content_Charset_Regex = Pattern.compile(".*charset\\s*=\\s*([^\">\\s]+)", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

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

    public static void main(String args[]) {
        String rs = args.length > 0 ? args[0] : "/";
        VUE.parseArgs(args);
        DEBUG.Enabled = true;
        DEBUG.DND = true;
        URLResource r = (URLResource) Resource.instance(rs);
        System.out.println("Resource: " + r);
        r.displayContent();
    }
}
