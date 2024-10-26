package org.icepdf.core.pobjects;

import org.icepdf.core.SecurityCallback;
import org.icepdf.core.application.ProductInfo;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.io.*;
import org.icepdf.core.pobjects.security.SecurityManager;
import org.icepdf.core.util.Defs;
import org.icepdf.core.util.LazyObjectLoader;
import org.icepdf.core.util.Library;
import org.icepdf.core.util.Parser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import org.icepdf.core.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The <code>Document</code> class represents a PDF document and provides
 * access to the hierarchy of objects contained in the body section of the
 * PDF document.  Most of the objects in the hierarchy are dictionaries which
 * contain references to page content and other objects such such as annotations.
 * For more information on the document object hierarchy, see the <i>ICEpdf
 * Developer's Guide</i>.</p>
 * <p/>
 * <p>The <code>Document</code> class also provides access to methods responsible
 * for rendering PDF document content.  Methods are available to capture page
 * content to a graphics context or extract image and text data on a page-by-page
 * basis.</p>
 * <p/>
 * <p>If your PDF rendering application will be accessing encrypted documents,
 * it is important to implement the SecurityCallback.  This interface provides
 * methods for getting password data from a user if needed.<p>
 *
 * @since 1.0
 */
public class Document {

    private static final Logger logger = Logger.getLogger(Document.class.toString());

    /**
     * Gets the version number of ICEpdf rendering core.  This is not the version
     * number of the PDF format used to encode this document.
     *
     * @return version number of ICEpdf's rendering core.
     */
    public static String getLibraryVersion() {
        return new StringBuffer().append(ProductInfo.PRIMARY).append(".").append(ProductInfo.SECONDARY).append(".").append(ProductInfo.TERTIARY).append(" ").append(ProductInfo.RELEASE_TYPE).toString();
    }

    private Catalog catalog;

    private PTrailer pTrailer;

    private String origin;

    private String cachedFilePath;

    private SecurityCallback securityCallback;

    private static boolean isCachingEnabled;

    private Library library = null;

    private SeekableInput documentSeekableInput;

    static {
        isCachingEnabled = Defs.sysPropertyBoolean("org.icepdf.core.streamcache.enabled", true);
    }

    /**
     * Creates a new instance of a Document.  A Document class represents
     * one PDF document.
     */
    public Document() {
    }

    /**
     * Utility method for setting the origin (filepath or URL) of this Document
     *
     * @param o new origin value
     * @see #getDocumentOrigin()
     */
    private void setDocumentOrigin(String o) {
        origin = o;
        if (logger.isLoggable(Level.CONFIG)) {
            logger.config("MEMFREE: " + Runtime.getRuntime().freeMemory() + " of " + Runtime.getRuntime().totalMemory());
            logger.config("LOADING: " + o);
        }
    }

    /**
     * Sets the cached file path in the case of opening a file from a URL.
     *
     * @param o new cached file path value
     * @see #getDocumentCachedFilePath
     */
    private void setDocumentCachedFilePath(String o) {
        cachedFilePath = o;
    }

    /**
     * Returns the cached file path in the case of opening a file from a URL.
     *
     * @return file path
     */
    private String getDocumentCachedFilePath() {
        return cachedFilePath;
    }

    /**
     * Load a PDF file from the given path and initiates the document's Catalog.
     *
     * @param filepath path of PDF document.
     * @throws PDFException         if an invalid file encoding.
     * @throws PDFSecurityException if a security provider cannot be found
     *                              or there is an error decrypting the file.
     * @throws IOException          if a problem setting up, or parsing the file.
     */
    public void setFile(String filepath) throws PDFException, PDFSecurityException, IOException {
        setDocumentOrigin(filepath);
        RandomAccessFileInputStream rafis = RandomAccessFileInputStream.build(new File(filepath));
        setInputStream(rafis);
    }

    /**
     * Load a PDF file from the given URL and initiates the document's Catalog.
     * If the system property org.icepdf.core.streamcache.enabled=true, the file
     * will be cached to a temp file; otherwise, the complete document stream will
     * be stored in memory.
     *
     * @param url location of file.
     * @throws PDFException         an invalid file encoding.
     * @throws PDFSecurityException if a security provider can not be found
     *                              or there is an error decrypting the file.
     * @throws IOException          if a problem downloading, setting up, or parsing the file.
     */
    public void setUrl(URL url) throws PDFException, PDFSecurityException, IOException {
        InputStream in = null;
        try {
            URLConnection urlConnection = url.openConnection();
            in = urlConnection.getInputStream();
            String pathOrURL = url.toString();
            setInputStream(in, pathOrURL);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Load a PDF file from the given input stream and initiates the document's Catalog.
     * If the system property org.icepdf.core.streamcache.enabled=true, the file
     * will be cached to a temp file; otherwise, the complete document stream will
     * be stored in memory.
     *
     * @param in        input stream containing PDF data
     * @param pathOrURL value assigned to document origin
     * @throws PDFException         an invalid stream or file encoding
     * @throws PDFSecurityException if a security provider can not be found
     *                              or there is an error decrypting the file.
     * @throws IOException          if a problem setting up, or parsing the SeekableInput.
     */
    public void setInputStream(InputStream in, String pathOrURL) throws PDFException, PDFSecurityException, IOException {
        setDocumentOrigin(pathOrURL);
        if (!isCachingEnabled) {
            ConservativeSizingByteArrayOutputStream byteArrayOutputStream = new ConservativeSizingByteArrayOutputStream(100 * 1024, null);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer, 0, buffer.length)) > 0) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
            int size = byteArrayOutputStream.size();
            byteArrayOutputStream.trim();
            byte[] data = byteArrayOutputStream.relinquishByteArray();
            SeekableByteArrayInputStream byteArrayInputStream = new SeekableByteArrayInputStream(data, 0, size);
            setInputStream(byteArrayInputStream);
        } else {
            File tempFile = File.createTempFile("ICEpdfTempFile" + getClass().hashCode(), ".tmp");
            tempFile.deleteOnExit();
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile.getAbsolutePath(), true);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer, 0, buffer.length)) > 0) {
                fileOutputStream.write(buffer, 0, length);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            setDocumentCachedFilePath(tempFile.getAbsolutePath());
            RandomAccessFileInputStream rafis = RandomAccessFileInputStream.build(tempFile);
            setInputStream(rafis);
        }
    }

    /**
     * Load a PDF file from the given byte array and initiates the document's Catalog.
     * If the system propertyorg.icepdf.core.streamcache.enabled=true, the file
     * will be cached to a temp file; otherwise, the complete document stream will
     * be stored in memory.
     * The given byte array is not necessarily copied, and will try to be directly
     * used, so do not modify it after passing it to this method.
     *
     * @param data      byte array containing PDF data
     * @param offset    the index into the byte array where the PDF data begins
     * @param length    the number of bytes in the byte array belonging to the PDF data
     * @param pathOrURL value assigned to document origin
     * @throws PDFException         an invalid stream or file encoding
     * @throws PDFSecurityException if a security provider can not be found
     *                              or there is an error decrypting the file.
     * @throws IOException          if a problem setting up, or parsing the SeekableInput.
     */
    public void setByteArray(byte[] data, int offset, int length, String pathOrURL) throws PDFException, PDFSecurityException, IOException {
        setDocumentOrigin(pathOrURL);
        if (!isCachingEnabled) {
            SeekableByteArrayInputStream byteArrayInputStream = new SeekableByteArrayInputStream(data, offset, length);
            setInputStream(byteArrayInputStream);
        } else {
            File tempFile = File.createTempFile("ICEpdfTempFile" + getClass().hashCode(), ".tmp");
            tempFile.deleteOnExit();
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile.getAbsolutePath(), true);
            fileOutputStream.write(data, offset, length);
            fileOutputStream.flush();
            fileOutputStream.close();
            setDocumentCachedFilePath(tempFile.getAbsolutePath());
            RandomAccessFileInputStream rafis = RandomAccessFileInputStream.build(tempFile);
            setInputStream(rafis);
        }
    }

    /**
     * Load a PDF file from the given SeekableInput stream and initiates the
     * document's Catalog.
     *
     * @param in        input stream containing PDF data
     * @param pathOrURL value assigned to document origin
     * @throws PDFException         an invalid stream or file encoding
     * @throws PDFSecurityException if a security provider can not be found
     *                              or there is an error decrypting the file.
     * @throws IOException          if a problem setting up, or parsing the SeekableInput.
     */
    public void setInputStream(SeekableInput in, String pathOrURL) throws PDFException, PDFSecurityException, IOException {
        setDocumentOrigin(pathOrURL);
        setInputStream(in);
    }

    /**
     * Sets the input stream of the PDF file to be rendered.
     *
     * @param in inputstream containing PDF data stream
     * @throws PDFException         if error occurs
     * @throws PDFSecurityException security error
     * @throws IOException          io error during stream handling
     */
    private void setInputStream(final SeekableInput in) throws PDFException, PDFSecurityException, IOException {
        try {
            documentSeekableInput = in;
            library = new Library();
            boolean loaded = false;
            try {
                loadDocumentViaXRefs(in);
                loaded = true;
            } catch (PDFException e) {
                throw e;
            } catch (PDFSecurityException e) {
                throw e;
            } catch (Exception e) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("Cross reference deferred loading failed, will fall back to linear reading.");
                }
            }
            if (!loaded) {
                if (catalog != null) {
                    catalog.dispose(false);
                    catalog = null;
                }
                if (library != null) {
                    library.dispose();
                    library = null;
                }
                library = new Library();
                pTrailer = null;
                in.seekAbsolute(0L);
                loadDocumentViaLinearTraversal(in.getInputStream());
            }
            catalog.init();
        } catch (PDFException e) {
            logger.log(Level.FINE, "Error loading PDF file during linear parse.", e);
            dispose();
            throw e;
        } catch (PDFSecurityException e) {
            dispose();
            throw e;
        } catch (IOException e) {
            dispose();
            throw e;
        } catch (Exception e) {
            dispose();
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Uitility method for loading the documents objects from the Xref table.
     *
     * @param in input stream to parse
     * @throws IOException          an i/o problem
     * @throws PDFException         an invalid stream or file encoding
     * @throws PDFSecurityException if a security provider can not be found
     *                              or there is an error decrypting the file.
     */
    private void loadDocumentViaXRefs(SeekableInput in) throws PDFException, PDFSecurityException, IOException {
        long xrefPosition = getInitialCrossReferencePosition(in);
        PTrailer documentTrailer = null;
        while (xrefPosition > 0L) {
            in.seekAbsolute(xrefPosition);
            Parser parser = new Parser(in);
            Object obj = parser.getObject(library);
            if (obj instanceof PObject) obj = ((PObject) obj).getObject();
            PTrailer trailer = (PTrailer) obj;
            if (trailer == null) throw new RuntimeException("Could not find trailer");
            if (trailer.getPrimaryCrossReference() == null) throw new RuntimeException("Could not find cross reference");
            if (documentTrailer == null) documentTrailer = trailer; else documentTrailer.addPreviousTrailer(trailer);
            if (true) break;
            xrefPosition = trailer.getPrev();
        }
        if (documentTrailer == null) throw new RuntimeException("Could not find document trailer");
        LazyObjectLoader lol = new LazyObjectLoader(library, in, documentTrailer.getPrimaryCrossReference());
        library.setLazyObjectLoader(lol);
        pTrailer = documentTrailer;
        catalog = documentTrailer.getRootCatalog();
        library.setCatalog(catalog);
        if (catalog == null) throw new NullPointerException("Loading via xref failed to find catalog");
        boolean madeSecurityManager = makeSecurityManager(documentTrailer);
        if (madeSecurityManager) attemptAuthorizeSecurityManager();
    }

    private long getInitialCrossReferencePosition(SeekableInput in) throws IOException {
        in.seekEnd();
        long endOfFile = in.getAbsolutePosition();
        long currentPosition = endOfFile - 1;
        long afterStartxref = -1;
        String startxref = "startxref";
        int startxrefIndexToMatch = startxref.length() - 1;
        while (currentPosition >= 0 && (endOfFile - currentPosition) < 2048) {
            in.seekAbsolute(currentPosition);
            int curr = in.read();
            if (curr < 0) throw new EOFException("Could not find startxref at end of file");
            if (curr == startxref.charAt(startxrefIndexToMatch)) {
                if (startxrefIndexToMatch == 0) {
                    afterStartxref = currentPosition + startxref.length();
                    break;
                }
                startxrefIndexToMatch--;
            } else startxrefIndexToMatch = startxref.length() - 1;
            currentPosition--;
        }
        if (afterStartxref < 0) throw new EOFException("Could not find startxref near end of file");
        in.seekAbsolute(afterStartxref);
        Parser parser = new Parser(in);
        Number xrefPositionObj = (Number) parser.getToken();
        if (xrefPositionObj == null) throw new RuntimeException("Could not find ending cross reference position");
        return xrefPositionObj.longValue();
    }

    /**
     * Uitily method for parsing a PDF documents object.  This should only be
     * called when the xref lookup fails.
     *
     * @param in stream representing whole pdf document
     * @throws PDFException         an invalid stream or file encoding
     * @throws PDFSecurityException if a security provider can not be found
     *                              or there is an error decrypting the file.
     */
    private void loadDocumentViaLinearTraversal(InputStream in) throws PDFException, PDFSecurityException {
        skipPastAnyPrefixJunk(in);
        library.setLinearTraversal();
        Parser parser = new Parser(in);
        PTrailer documentTrailer = null;
        Object pdfObject;
        while (true) {
            pdfObject = parser.getObject(library);
            if (pdfObject == null) {
                break;
            }
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(pdfObject.getClass().getName() + " " + pdfObject);
            }
            if (pdfObject instanceof PObject) {
                PObject tmp = (PObject) pdfObject;
                Object obj = tmp.getObject();
                if (obj != null) pdfObject = obj;
            }
            if (pdfObject instanceof Catalog) {
                catalog = (Catalog) pdfObject;
            }
            if (pdfObject instanceof PTrailer) {
                if (documentTrailer == null) {
                    documentTrailer = (PTrailer) pdfObject;
                } else {
                    PTrailer nextTrailer = (PTrailer) pdfObject;
                    documentTrailer.addNextTrailer(nextTrailer);
                    documentTrailer = nextTrailer;
                }
            }
        }
        if (documentTrailer != null) {
            LazyObjectLoader lol = new LazyObjectLoader(library, null, documentTrailer.getPrimaryCrossReference());
            library.setLazyObjectLoader(lol);
        }
        pTrailer = documentTrailer;
        library.setCatalog(catalog);
        if (documentTrailer != null) {
            boolean madeSecurityManager = makeSecurityManager(documentTrailer);
            if (madeSecurityManager) attemptAuthorizeSecurityManager();
        }
    }

    /**
     * Typically, if we're doing a linear traversal, it's because the PDF file
     * is corrupted, usually by junk being appended to it, or the ending
     * being truncated, or, in this case, from junk being inserted into the
     * beginning of the file, skewing all the xref object offsets.
     * <p/>
     * We're going to look for the "%PDF-1." string that most PDF files start
     * with. If we do find it, then leave the InputStream after the next
     * whitespace, else rewind back to the beginning, in case the file was
     * never encoded with the PDF version comment.
     *
     * @param in InputStream derived from SeekableInput.getInputStream()
     */
    private void skipPastAnyPrefixJunk(InputStream in) {
        if (!in.markSupported()) return;
        try {
            final int scanLength = 2048;
            final String scanFor = "%PDF-1.";
            int scanForIndex = 0;
            boolean scanForWhiteSpace = false;
            in.mark(scanLength);
            for (int i = 0; i < scanLength; i++) {
                int data = in.read();
                if (data < 0) {
                    in.reset();
                    return;
                }
                if (scanForWhiteSpace) {
                    if (Parser.isWhitespace((char) data)) {
                        return;
                    }
                } else {
                    if (data == scanFor.charAt(scanForIndex)) {
                        scanForIndex++;
                        if (scanForIndex == scanFor.length()) {
                            scanForWhiteSpace = true;
                        }
                    } else scanForIndex = 0;
                }
            }
            in.reset();
        } catch (IOException e) {
            try {
                in.reset();
            } catch (IOException e2) {
            }
        }
    }

    /**
     * Utility method for building the SecurityManager if the document
     * contains a crypt entry in the PTrailer.
     *
     * @param documentTrailer document trailer
     * @return Whether or not a SecurityManager was made, and set in the Library
     * @throws PDFSecurityException if there is an issue finding encryption libraries.
     */
    private boolean makeSecurityManager(PTrailer documentTrailer) throws PDFSecurityException {
        boolean madeSecurityManager = false;
        Hashtable encryptDictionary = documentTrailer.getEncrypt();
        Vector fileID = documentTrailer.getID();
        if (encryptDictionary != null && fileID != null) {
            library.securityManager = new SecurityManager(library, encryptDictionary, fileID);
            madeSecurityManager = true;
        }
        return madeSecurityManager;
    }

    /**
     * If the document has a SecurityManager it is encrypted and as a result the
     * following method is used with the SecurityCallback to prompt a user for
     * a password if needed.
     *
     * @throws PDFSecurityException error during authorization manager setup
     */
    private void attemptAuthorizeSecurityManager() throws PDFSecurityException {
        if (!library.securityManager.isAuthorized("")) {
            int count = 1;
            String password;
            while (true) {
                if (securityCallback != null) {
                    password = securityCallback.requestPassword(this);
                    if (password == null) {
                        throw new PDFSecurityException("Encryption error");
                    }
                } else {
                    throw new PDFSecurityException("Encryption error");
                }
                if (library.securityManager.isAuthorized(password)) {
                    break;
                }
                count++;
                if (count > 3) {
                    throw new PDFSecurityException("Encryption error");
                }
            }
        }
        library.setEncrypted(true);
    }

    /**
     * Gets the page dimension of the indicated page number using the specified
     * rotation factor.
     *
     * @param pageNumber   Page number for the given dimension.  The page
     *                     number is zero-based.
     * @param userRotation Rotation, in degrees, that has been applied to page
     *                     when calculating the dimension.
     * @return page dimension for the specified page number
     * @see #getPageDimension(int, float, float)
     */
    public PDimension getPageDimension(int pageNumber, float userRotation) {
        Page page = catalog.getPageTree().getPage(pageNumber, this);
        PDimension pd = page.getSize(userRotation);
        catalog.getPageTree().releasePage(page, this);
        return pd;
    }

    /**
     * Gets the page dimension of the indicated page number using the specified
     * rotation and zoom settings.  If the page does not exist then a zero
     * dimension is returned.
     *
     * @param pageNumber   Page number for the given dimension.  The page
     *                     number is zero-based.
     * @param userRotation Rotation, in degrees, that has been applied to page
     *                     when calculating the dimension.
     * @param userZoom     Any deviation from the page's actual size, by zooming in or out.
     * @return page dimension for the specified page number.
     * @see #getPageDimension(int, float)
     */
    public PDimension getPageDimension(int pageNumber, float userRotation, float userZoom) {
        Page page = catalog.getPageTree().getPage(pageNumber, this);
        if (page != null) {
            PDimension pd = page.getSize(userRotation, userZoom);
            catalog.getPageTree().releasePage(page, this);
            return pd;
        } else {
            return new PDimension(0, 0);
        }
    }

    /**
     * Returns the origin (filepath or URL) of this Document.  This is the original
     * location of the file where the method getDocumentLocation returns the actual
     * location of the file.  The origin and location of the document will only
     * be different if it was loaded from a URL or an input stream.
     *
     * @return file path or URL
     * @see #getDocumentLocation
     */
    public String getDocumentOrigin() {
        return origin;
    }

    /**
     * Returns the file location or URL of this Document. This location may be different
     * from the file origin if the document was loaded from a URL or input stream.
     * If the file was loaded from a URL or input stream the file location is
     * the path to where the document content is cached.
     *
     * @return file path
     * @see #getDocumentOrigin()
     */
    public String getDocumentLocation() {
        if (cachedFilePath != null) return cachedFilePath;
        return origin;
    }

    /**
     * Returns the total number of pages in this document.
     *
     * @return number of pages in the document
     */
    public int getNumberOfPages() {
        try {
            return catalog.getPageTree().getNumberOfPages();
        } catch (Exception e) {
            logger.log(Level.FINE, "Error getting number of pages.", e);
        }
        return 0;
    }

    /**
     * Paints the contents of the given page number to the graphics context using
     * the specified rotation, zoom, rendering hints and page boundary.
     *
     * @param pageNumber     Page number to paint.  The page number is zero-based.
     * @param g              graphics context to which the page content will be painted.
     * @param renderHintType Constant specified by the GraphicsRenderingHints class.
     *                       There are two possible entries, SCREEN and PRINT, each with configurable
     *                       rendering hints settings.
     * @param pageBoundary   Constant specifying the page boundary to use when
     *                       painting the page content.
     * @param userRotation   Rotation factor, in degrees, to be applied to the rendered page.
     * @param userZoom       Zoom factor to be applied to the rendered page.
     */
    public void paintPage(int pageNumber, Canvas g, final int renderHintType, final int pageBoundary, float userRotation, float userZoom) {
        Page page = catalog.getPageTree().getPage(pageNumber, this);
        PDimension sz = page.getSize(userRotation, userZoom);
        int pageWidth = (int) sz.getWidth();
        int pageHeight = (int) sz.getHeight();
        Canvas gg = new Canvas(Bitmap.createBitmap(pageWidth, pageHeight, Config.ARGB_8888));
        page.paint(gg, renderHintType, pageBoundary, userRotation, userZoom);
        gg = null;
        catalog.getPageTree().releasePage(page, this);
    }

    /**
     * Dispose of Document, freeing up all used resources.
     */
    public void dispose() {
        if (catalog != null) {
            catalog.dispose(false);
            catalog = null;
        }
        if (library != null) {
            library.dispose();
            library = null;
        }
        pTrailer = null;
        if (documentSeekableInput != null) {
            try {
                documentSeekableInput.close();
            } catch (IOException e) {
                logger.log(Level.FINE, "Error closing document input stream.", e);
            }
            documentSeekableInput = null;
        }
        String fileToDelete = getDocumentCachedFilePath();
        if (fileToDelete != null) {
            File file = new File(fileToDelete);
            boolean success = file.delete();
            if (!success && logger.isLoggable(Level.WARNING)) {
                logger.warning("Error deleting URL cached to file " + fileToDelete);
            }
        }
    }

    /**
     * Takes the internal PDF data, which may be in a file or in RAM,
     * and write it to the provided OutputStream.
     * The OutputStream is not flushed or closed, in case this method's
     * caller requires otherwise.
     *
     * @param out OutputStream to which the PDF file bytes are written.
     * @throws IOException if there is some problem reading or writing the PDF data
     */
    public void writeToOutputStream(OutputStream out) throws IOException {
        long documentLength = documentSeekableInput.getLength();
        SeekableInputConstrainedWrapper wrapper = new SeekableInputConstrainedWrapper(documentSeekableInput, 0L, documentLength, false);
        try {
            wrapper.prepareForCurrentUse();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = wrapper.read(buffer, 0, buffer.length)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch (Throwable e) {
            logger.log(Level.FINE, "Error writting PDF output stream.", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                wrapper.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Gets an Image of the specified page.  The image size is automatically
     * calculated given the page boundary, user rotation and zoom.  The rendering
     * quality is defined by GraphicsRenderingHints.SCREEN.
     *
     * @param pageNumber     Page number of the page to capture the image rendering.
     *                       The page number is zero-based.
     * @param renderHintType Constant specified by the GraphicsRenderingHints class.
     *                       There are two possible entries, SCREEN and PRINT each with configurable
     *                       rendering hints settings.
     * @param pageBoundary   Constant specifying the page boundary to use when
     *                       painting the page content. Typically use Page.BOUNDARY_CROPBOX.
     * @param userRotation   Rotation factor, in degrees, to be applied to the rendered page.
     *                       Arbitrary rotations are not currently supported for this method,
     *                       so only the following values are valid: 0.0f, 90.0f, 180.0f, 270.0f.
     * @param userZoom       Zoom factor to be applied to the rendered page.
     * @return an Image object of the current page.
     */
    public Bitmap getPageImage(int pageNumber, final int renderHintType, final int pageBoundary, float userRotation, float userZoom) {
        Page page = catalog.getPageTree().getPage(pageNumber, this);
        PDimension sz = page.getSize(pageBoundary, userRotation, userZoom);
        int pageWidth = (int) sz.getWidth();
        int pageHeight = (int) sz.getHeight();
        Bitmap image = Bitmap.createBitmap(pageWidth, pageHeight, Config.ARGB_8888);
        Canvas g = new Canvas(image);
        page.paint(g, renderHintType, pageBoundary, userRotation, userZoom);
        g = null;
        catalog.getPageTree().releasePage(page, this);
        return image;
    }

    /**
     * Gets a vector of StringBuffers where each index represents a text block inside
     * the specified page number.  Due to limitations in how PDF document are encoded,
     * it is not always possible to extract valid ASCII or unicode text and order
     * is not always guaranteed.
     *
     * @param pageNumber Page number of page in which text extraction will act on.
     *                   The page number is zero-based.
     * @return vector of StringBuffers of all text objects inside the specified page.
     */
    public Vector<StringBuffer> getPageText(int pageNumber) {
        PageTree pageTree = catalog.getPageTree();
        if (pageNumber >= 0 && pageNumber < pageTree.getNumberOfPages()) {
            Page pg = pageTree.getPage(pageNumber, this);
            Vector<StringBuffer> text = pg.getText();
            catalog.getPageTree().releasePage(pg, this);
            return text;
        } else {
            return new Vector<StringBuffer>();
        }
    }

    /**
     * Gets the security manager for this document. If the document has no
     * security manager null is returned.
     *
     * @return security manager for document if available.
     */
    public SecurityManager getSecurityManager() {
        return library.securityManager;
    }

    /**
     * Sets the security callback to be used for this document.  The security
     * callback allows a mechanism for prompting a user for a password if the
     * document is password protected.
     *
     * @param securityCallback a class which implements the SecurityCallback
     *                         interface.
     */
    public void setSecurityCallback(SecurityCallback securityCallback) {
        this.securityCallback = securityCallback;
    }

    /**
     * Gets the document's information as specified in the PTrailer in the document
     * hierarchy.
     *
     * @return document information
     * @see org.icepdf.core.pobjects.PInfo for more information.
     */
    public PInfo getInfo() {
        if (pTrailer == null) return null;
        return pTrailer.getInfo();
    }

    /**
     * Gets a vector of Images where each index represents an image  inside
     * the specified page.  The images are returned in the size in which they
     * where embedded in the PDF document, which may be different than the
     * size displayed when the complete PDF page is rendered.
     *
     * @param pageNumber page number to act on.  Zero-based page number.
     * @return vector of Images inside the current page
     */
    public Vector getPageImages(int pageNumber) {
        Page pg = catalog.getPageTree().getPage(pageNumber, this);
        Vector images = pg.getImages();
        catalog.getPageTree().releasePage(pg, this);
        return images;
    }

    /**
     * Gets the Document Catalog's PageTree entry as specified by the Document
     * hierarchy.  The PageTree can be used to obtain detailed information about
     * the Page object which makes up the document.
     *
     * @return PageTree specified by the document hierarchy.
     */
    public PageTree getPageTree() {
        return catalog.getPageTree();
    }

    /**
     * Gets the Document's Catalog as specified by the Document hierarchy. The
     * Catalog can be used to traverse the Document's hierarchy.
     *
     * @return document's Catalog object; null, if one does not exist.
     */
    public Catalog getCatalog() {
        return catalog;
    }
}
