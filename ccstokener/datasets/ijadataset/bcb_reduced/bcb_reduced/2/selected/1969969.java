package org.keel.comm;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Handles the transfer and serializatoin issues BLOBs and other binary objects
 * between Keel's client and server sides. Designed specifically to handle both
 * VERY large objects (1GB+) as well as small binary objects.
 * 
 * Based partly on the DefaultFileItem object from the Jarkarta Commons
 * Fileupload project.
 * 
 * This class can handle both local (e.g. single-VM deployments) and remote
 * requests and responses with binary objects, of basically any size. Small
 * objects ("small" being less than the adjustable threshold) can be handled
 * in-memory as a simple byte array. Larger objects will be written to a file,
 * and passed as a URL reference to this file. The client-side must then access
 * this URL in order to access the content. (For secure content, ensure that the
 * URL is accessible only to the user running the web/application server (e.g.
 * Tomcat), not to the end-user (e.g. with the browser) directly.
 * 
 * BinaryWrapper is "two-way", that is, used for both uploaded files (binary
 * objects coming from the client) and binary responses (such as PDF's or
 * charts, images, etc) coming from the server as the result of execution of
 * Models. For use in Models, use a BinaryWrapper as the content of an Output.
 * 
 * @author Stephen Davidson
 * @author Michael Nash - extended to use URLs for cross-vm communications
 */
public class BinaryWrapper implements Serializable {

    /**
     * Counter used in unique identifier generation.
     */
    private static int counter = 0;

    /**
     * The mode of this BinaryWrapper has not yet been determined
     */
    private static final int MODE_UNKNOWN = -1;

    /**
     * The content is stored in memory
     */
    private static final int MODE_INMEMORY = 1;

    /**
     * The content is stored at a URL location
     */
    private static final int MODE_URL = 2;

    /**
     * This BinaryWrapper represents an executable task, no stored content at
     * all.
     */
    private static final int MODE_EXECUTABLE = 3;

    /**
     * Content is stored in memory, no matter what it's size (see the
     * setKeepInMemory() method)
     */
    private static final int MODE_KEEPINMEMORY = 4;

    /**
     * The current mode of this BinaryWrapper.
     */
    private int mode = MODE_UNKNOWN;

    /**
     * The content type passed by the browser, or <code>null</code> if not
     * defined (this is typically a mime type and subtype).
     */
    protected String contentType = null;

    /**
     * The original filename in originating filesystem. Used for uploaded files
     */
    protected String fileName = null;

    /**
     * External filename for content - not retained when serialized
     */
    protected transient String contentFileName = null;

    /**
     * When the content is supplied, we know it's size. Store this info
     */
    protected long size = 0l;

    /**
     * The threshold above which contents will be stored on disk.
     */
    protected int sizeThreshold = 1048576;

    /**
     * The directory in which uploaded and temporary files will be stored, if
     * stored on disk. Storage to disk only happens if the content size exceeds
     * the threshold, and setKeepInMemory is not called). If we use this
     * feature, the "urlPrefix" below must be set for the client to be able to
     * retrieve the content. The default values are suitable for a single-VM
     * deploy, or a multi-VM deploy on the same server.
     */
    protected File repository = new File(System.getProperty("java.io.tmpdir"));

    /**
     * If the content we're dealing with is larger than the threshold (defined
     * above), then we must write it to disk on the originating system and read
     * it back from a URL on the destination side. To do this we need a
     * urlPrefix accessible from this server by all destination. This URL prefix
     * has a direct relationship with the repository path, defined above. See
     * the note in the class Javadoc regaring security.
     */
    private String urlPrefix = "file://" + System.getProperty("java.io.tmpdir");

    /**
     * The URL to access the content from the "receiving" side of this
     * BinaryWrapper
     */
    private String url = null;

    /**
     * Cached content, if below the threshold (or setKeepInMemory was called)
     */
    protected byte[] cachedContent;

    /**
     * A BinaryWrapper can be a reference to an external process (e.g. some
     * other, possibly even non-Java executable application) which will supply
     * the output for this BinaryWrapper. If the execPath is not null, then this
     * BinaryWrapper is pointing to such an executable. This must be used with
     * caution, as the server on which the executable is run may be different
     * from where the request originates!
     */
    private String execPath = null;

    /**
     * Handle to the current process, if any
     */
    private transient Process currentProcess = null;

    /**
     * Create an uninitialized BinaryWrapper
     */
    public BinaryWrapper() {
        super();
    }

    /**
     * Insist that the data be kept in memory, no matter the size. If there is
     * no opportunity to use external storage. This essentially disregards the
     * threshold setting.
     */
    public void setKeepInMemory() {
        mode = MODE_KEEPINMEMORY;
    }

    /**
     * Specify the "filename" for this content. Note that this is an informative
     * name, not necessarily where the content is really stored!
     * 
     * @param newName
     *            Name to set the filename property to
     */
    public void setFileName(String newName) {
        fileName = newName;
    }

    /**
     * Retrieve the "filename" property of this BinaryWrapper. Note that this is
     * an informative name, not necessarily where the content is in fact stored!
     * For "uploaded" files, this is the original filename specified by the
     * browser.
     * 
     * @return The filename property of this BinaryWrapper.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Specify the content type of the content to be transferred. This is
     * usually the mime-type used by the browser to display this content.
     * 
     * @param newType
     *            The mime type of the content of this BinaryWrapper
     */
    public void setContentType(String newType) {
        contentType = newType;
    }

    /**
     * Specify the file repository used to hold content in temporary files if
     * it's size exceeds the threshold size.
     * 
     * @param newRepository
     *            A File object indicating a writeable directory.
     */
    public void setRepository(File newRepository) {
        assert repository != null;
        newRepository.mkdirs();
        repository = newRepository;
        if (repository != null) {
            if (!repository.isDirectory()) {
                throw new IllegalArgumentException("Specified repository '" + newRepository.getAbsolutePath() + "' is not a directory");
            }
        }
    }

    /**
     * The threshold, in bytes, below which items will be retained in memory and
     * above which they will be stored as a file. Ignored if setKeelInMemory is
     * called.
     * 
     * @param newThreshold
     *            A new size in bytes to use as the threshold.
     */
    public void setThreshold(int newThreshold) {
        if (sizeThreshold <= 512) {
            throw new IllegalArgumentException("Threshold is too small for buffer: " + sizeThreshold);
        }
        sizeThreshold = newThreshold;
    }

    /**
     * Set the content cache directly. Makes no attempt to store the content
     * externally unless it is over the threshold size.
     * 
     * @param newContent
     *            A byte array of content (which may not be null or zero-length)
     */
    public void setContent(byte[] newContent) {
        assert newContent != null;
        assert newContent.length > 0;
        cachedContent = newContent;
        size = cachedContent.length;
        if (cachedContent.length > sizeThreshold) {
            try {
                File tmpFile = File.createTempFile("tmp", "dat", repository);
                FileOutputStream fout = new FileOutputStream(tmpFile.getAbsolutePath());
                fout.write(cachedContent);
                fout.flush();
                fout.close();
                url = urlPrefix + tmpFile.getName();
                mode = MODE_URL;
                cachedContent = null;
            } catch (IOException ie) {
                throw new IllegalArgumentException("Unable to create temporary file:" + ie.getMessage());
            }
        } else {
            mode = MODE_INMEMORY;
        }
    }

    /**
     * Specify a path to an executable from which this BinaryWrapper get's it's
     * content. You can supply command-line arguments as part of the string, as
     * necessary.
     * 
     * @param newPath
     */
    public void setExecutablePath(String newPath) {
        assert newPath != null;
        if (mode != MODE_UNKNOWN) {
            throw new IllegalArgumentException("This BinaryWrapper is already mode " + modeToString(mode) + ", unable to re-set to executable - release it first");
        }
        if (newPath == null) {
            throw new IllegalArgumentException("Path may not be null here");
        }
        execPath = newPath;
        mode = MODE_EXECUTABLE;
    }

    /**
     * Does this BinaryWrapper refer to some external executable program in
     * order to get it's content?
     * 
     * @return True if this is an "executable"-style BinaryWrapper, false if
     *         it's not.
     */
    public boolean isExecutable() {
        if (mode == MODE_EXECUTABLE) {
            return true;
        }
        return false;
    }

    /**
     * Returns an {@link java.io.InputStream InputStream}that can be used to
     * retrieve the contents of this BinaryWrapper.
     * 
     * @return An {@link java.io.InputStream InputStream}that can be used to
     *         retrieve the contents of the file.
     * 
     * @exception IOException
     *                if an error occurs.
     */
    public InputStream getInputStream() throws IOException {
        if (mode == MODE_UNKNOWN) {
            throw new IOException("Unknown mode in BinaryWrapper");
        }
        if (isInMemory()) {
            return new ByteArrayInputStream(cachedContent);
        }
        if (mode == MODE_EXECUTABLE) {
            if (currentProcess != null) {
                throw new IOException("InputStream already open, please close first");
            }
            currentProcess = Runtime.getRuntime().exec(execPath);
            return currentProcess.getInputStream();
        }
        if (mode == MODE_URL) {
            URL contentUrl = new URL(url);
            return contentUrl.openStream();
        }
        throw new IllegalArgumentException("BinaryWrapper had no contents");
    }

    /**
     * Returns the content type passed by the browser or <code>null</code> if
     * not defined.
     * 
     * @return The content type passed by the browser or <code>null</code> if
     *         not defined.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Provides a hint as to whether or not the file contents will be read from
     * memory.
     * 
     * @return <code>true</code> if the file contents will be read from
     *         memory; <code>false</code> otherwise.
     */
    public boolean isInMemory() {
        if ((mode == MODE_INMEMORY) || (mode == MODE_KEEPINMEMORY)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the size of the content, if we know it.
     * 
     * @return The size of the content, in bytes.
     */
    public long getSize() {
        return size;
    }

    public void loadFromURL(String p_url) throws IOException {
        loadFromStream(new URL(p_url).openStream());
    }

    /**
     * Returns the data as an array of bytes. If the contents were not yet
     * cached in memory, they will be loaded from disk storage first, or
     * accessed from the URL if appropriate.
     * 
     * @return The contents of the file as an array of bytes.
     */
    public byte[] get() throws IOException {
        if (mode == MODE_URL) {
            loadFromURL(url);
        }
        return cachedContent;
    }

    /**
     * Returns the contents of the file as a String, using the specified
     * encoding. This method uses {@link #get()}to retrieve the contents of the
     * file.
     * 
     * @param encoding
     *            The character encoding to use.
     * 
     * @return The contents of the file, as a string.
     * 
     * @exception UnsupportedEncodingException
     *                if the requested character encoding is not available.
     * @throws IOException If the get() method throws an IOException retrieving the content.
     */
    public String getString(String encoding) throws UnsupportedEncodingException, IOException {
        return new String(get(), encoding);
    }

    /**
     * Returns the contents of the file as a String, using the default character
     * encoding. This method uses {@link #get()}to retrieve the contents of the
     * file.
     * 
     * @return The contents of the file, as a string.
     * @throws IOException If the get() method throws an IOException retrieving the content.
     */
    public String getString() throws IOException {
        return new String(get());
    }

    /**
     * Closes and flushes the underlying output stream.
     * 
     * @throws IOException
     *             Thrown if any errors occur with the output streams
     */
    public void close() throws IOException {
        IOException ioe = null;
        if (currentProcess != null) {
            try {
                final int exitValue = currentProcess.exitValue();
                if (exitValue != 0) {
                    System.err.println(this.getClass().getName() + ".close:\'" + this.execPath + "\' exited with value of " + exitValue);
                }
            } catch (IllegalThreadStateException e) {
                currentProcess.destroy();
            }
            currentProcess = null;
        }
        if (ioe != null) {
            throw ioe;
        }
    }

    /**
     * Deletes the underlying storage for a file item, including deleting any
     * associated temporary disk file. Although this storage will be deleted
     * automatically when the <code>FileItem</code> instance is garbage
     * collected, this method can be used to ensure that this is done at an
     * earlier time, thus preserving system resources.
     */
    public void delete() {
        cachedContent = null;
    }

    /**
     * Returns an {@link java.io.OutputStream OutputStream}that can be used for
     * storing the contents of the file.
     * 
     * @return An {@link java.io.OutputStream OutputStream}that can be used for
     *         storing the contensts of the file.
     * 
     * @exception IOException
     *                if an error occurs.
     */
    public OutputStream getOutputStream() throws IOException {
        if (mode == MODE_EXECUTABLE) {
            if (currentProcess != null) {
                currentProcess = Runtime.getRuntime().exec(execPath);
            }
            return currentProcess.getOutputStream();
        }
        if (!isInMemory()) {
            loadFromURL(url);
        } else {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            bout.write(cachedContent);
            return bout;
        }
        return null;
    }

    /**
     * Load the content of this BinaryWrapper from
     * the specified input stream.
     * @param is The input stream to load from
     * @throws IOException If an IOException occurs during loading
     */
    public void loadFromStream(InputStream is) throws IOException {
        assert is != null;
        BufferedInputStream bin = new BufferedInputStream(is);
        cachedContent = new byte[bin.available()];
        bin.read(cachedContent, 0, bin.available());
        bin.close();
        if ((cachedContent == null) || (cachedContent.length == 0)) {
            throw new IOException("Read no data from stream");
        }
        setContent(cachedContent);
    }

    /**
     * Call release to clean up if we have not already
     */
    protected void finalize() throws Throwable {
        release();
    }

    /**
     * Removes the file contents from the temporary storage.
     */
    public void release() throws Throwable {
        if (mode == MODE_INMEMORY) {
            cachedContent = null;
            return;
        }
        try {
            this.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert the specified mode value into a string
     * @param p_mode The mode
     * @return A string representation of the mode
     */
    private String modeToString(int p_mode) {
        switch(p_mode) {
            case MODE_UNKNOWN:
                return "unknown";
            case MODE_INMEMORY:
                return "in-memory";
            case MODE_URL:
                return "url";
            case MODE_EXECUTABLE:
                return "executable";
        }
        return "unknown mode code " + p_mode;
    }

    /**
     * @param newUrlPrefix
     *            The urlPrefix to set. See class Javadoc for a description.
     */
    public void setURLPrefix(String newUrlPrefix) {
        if (urlPrefix == null) {
            throw new IllegalArgumentException("Null URL prefix not allowed");
        }
        this.urlPrefix = newUrlPrefix;
    }

    /**
     * Return this BinaryWrapper as a string description, useful for debugging
     * @return String representation of this BinaryWrapper
     */
    public String toString() {
        StringBuffer s = new StringBuffer("BinaryWrapper:\n");
        s.append("\tcounter:" + counter + "\n");
        s.append("\tmode:" + mode + "\n");
        s.append("\tcontent type:" + contentType + "\n");
        s.append("\tfilename:" + fileName + "\n");
        s.append("\tcontent filename:" + contentFileName + "\n");
        s.append("\tsize threshold:" + sizeThreshold + "\n");
        s.append("\trepository:" + repository.getAbsolutePath() + "\n");
        s.append("\turl prefix:" + urlPrefix + "\n");
        if (cachedContent != null) {
            s.append("\tcached content size:" + cachedContent.length + "\n");
        } else {
            s.append("\tno cached content");
        }
        s.append("\texecPath:" + execPath + "\n");
        if (isInMemory()) {
            s.append("\tinmemory: true");
        } else {
            s.append("\tinmemory: false");
        }
        return s.toString();
    }

    /**
     * Write the current content to the specified output stream
     * @param bout The output stream to write the contents to
     * @throws IOException If an error occurs during writing
     */
    public void writeToStream(OutputStream bout) throws IOException {
        if (mode == MODE_UNKNOWN) {
            throw new IOException("BinaryWrapper in unknown mode - unable to write file " + fileName);
        }
        if (mode == MODE_URL) {
            loadFromURL(url);
        }
        if (cachedContent == null) {
            throw new IOException("No content in BinaryWrapper - unable to write file " + fileName);
        }
        bout.write(cachedContent);
        bout.flush();
        bout.close();
    }

    /**
     * Convenience method to write the contents to the specified filename
     * @param p_fieldname A full pathname of the output file to write
     * @throws IOException If an error occurs during writing.
     */
    public void writeToFile(String p_fieldname) throws IOException {
        assert p_fieldname != null;
        File fout = new File(p_fieldname);
        File dir = fout.getParentFile();
        dir.mkdirs();
        FileOutputStream bout = new FileOutputStream(p_fieldname);
        writeToStream(bout);
    }
}
