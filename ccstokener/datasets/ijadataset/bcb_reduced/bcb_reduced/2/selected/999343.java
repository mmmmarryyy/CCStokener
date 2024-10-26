package net.sourceforge.javautil.common.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import net.sourceforge.javautil.common.IOUtil;
import net.sourceforge.javautil.common.URLUtil;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;

/**
 * The basic contract/interface for a source of input that can be modified.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: IModifiableInputSource.java 2736 2011-02-04 02:53:50Z ponderator $
 */
public interface IModifiableInputSource extends IInputSource {

    /**
	 * @return The timestamp representing the last time this source was modified
	 */
    long getLastModified();

    /**
	 * Write the contents to the file.
	 * 
	 * @param contents The new contents for the file
	 */
    void writeAll(byte[] contents);

    /**
	 * Write text/string contents to the file.
	 * 
	 * @param textContents The new text contents for the file
	 */
    void writeAsText(String textContents);

    /**
	 * @return A writer for writing to this file
	 */
    Writer getWriter() throws IOException;

    /**
	 * @return A new output stream for writing to the virtual artifact
	 * @throws IOException
	 */
    OutputStream getOutputStream() throws IOException;

    /**
	 * The base for most {@link IModifiableInputSource} implementations. 
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: IModifiableInputSource.java 2736 2011-02-04 02:53:50Z ponderator $
	 */
    public abstract static class ModifiableInputSourceAbstract implements IModifiableInputSource {

        public byte[] readAll() {
            InputStream input = null;
            try {
                return IOUtil.read(input = this.getInputStream(), null, false);
            } catch (IOException e) {
                throw ThrowableManagerRegistry.caught(e);
            } finally {
                if (input != null) try {
                    input.close();
                } catch (IOException e) {
                    ThrowableManagerRegistry.caught(e);
                }
            }
        }

        public String readAsText() {
            return new String(this.readAll());
        }

        public Reader getReader() throws IOException {
            return new InputStreamReader(getInputStream());
        }

        public Writer getWriter() throws IOException {
            return new OutputStreamWriter(getOutputStream());
        }

        public void writeAll(byte[] contents) {
            try {
                IOUtil.transfer(new ByteArrayInputStream(contents), getOutputStream());
            } catch (IOException e) {
                throw ThrowableManagerRegistry.caught(e);
            }
        }

        public void writeAsText(String textContents) {
            this.writeAll(textContents.getBytes());
        }

        public abstract String toString();
    }

    /**
	 * This will wrap a {@link File} as a {@link IModifiableInputSource} and make sure
	 * certain operations do not cause IO leaks. 
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: IModifiableInputSource.java 2736 2011-02-04 02:53:50Z ponderator $
	 */
    public static class FileInputSource extends ModifiableInputSourceAbstract {

        protected final File file;

        public FileInputSource(File file) {
            this.file = file;
        }

        public String getName() {
            return file.getAbsolutePath();
        }

        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        public OutputStream getOutputStream() throws IOException {
            return new FileOutputStream(file);
        }

        public long getLastModified() {
            return file.lastModified();
        }

        public boolean isReadOnly() {
            return !file.canWrite();
        }

        @Override
        public String toString() {
            return file.toString();
        }
    }

    /**
	 * This will wrap a {@link URL} as a {@link IModifiableInputSource} and make sure
	 * certain operations do not cause IO leaks. 
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: IModifiableInputSource.java 2736 2011-02-04 02:53:50Z ponderator $
	 */
    public static class URLInputSource extends ModifiableInputSourceAbstract {

        protected final URL url;

        protected Boolean readOnly = null;

        public URLInputSource(URL url) {
            this.url = url;
        }

        public String getName() {
            return url.toExternalForm();
        }

        public InputStream getInputStream() throws IOException {
            return url.openStream();
        }

        public OutputStream getOutputStream() throws IOException {
            return url.openConnection().getOutputStream();
        }

        public long getLastModified() {
            return URLUtil.getLastModified(url);
        }

        public boolean isReadOnly() {
            if (readOnly == null) {
                try {
                    OutputStream stream = url.openConnection().getOutputStream();
                    readOnly = Boolean.FALSE;
                    stream.close();
                } catch (UnknownServiceException e) {
                    readOnly = Boolean.TRUE;
                } catch (IOException e) {
                }
            }
            return readOnly;
        }

        @Override
        public String toString() {
            return url.toExternalForm();
        }
    }

    /**
	 * For in memory input sources.
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: IModifiableInputSource.java 2736 2011-02-04 02:53:50Z ponderator $
	 */
    public static class MemoryInputSource extends ModifiableInputSourceAbstract {

        protected String name;

        protected long modified = System.currentTimeMillis();

        protected byte[] contents;

        protected boolean readOnly = false;

        ;

        /**
		 * @return The in-memory contents of this input source.
		 */
        public byte[] getContents() {
            return contents;
        }

        public MemoryInputSource setContents(byte[] contents) {
            this.contents = contents;
            this.recordModification();
            return this;
        }

        public String getName() {
            return name;
        }

        public MemoryInputSource setName(String name) {
            this.name = name;
            this.recordModification();
            return this;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(contents == null ? new byte[0] : contents);
        }

        public OutputStream getOutputStream() throws IOException {
            return new OutputStream() {

                protected boolean open = true;

                protected final ByteArrayOutputStream out = new ByteArrayOutputStream();

                @Override
                public void write(int b) throws IOException {
                    if (!open) throw new IOException("Resource has been closed");
                    out.write(b);
                }

                @Override
                public void close() throws IOException {
                    if (open) open = false; else throw new IOException("Resource already closed");
                }

                @Override
                public void flush() throws IOException {
                    if (!open) throw new IOException("Resource has been closed");
                    setContents(out.toByteArray());
                }
            };
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public long getLastModified() {
            return this.modified;
        }

        @Override
        public String toString() {
            return "MemoryFile[" + name + "]";
        }

        /**
		 * Record modifications to this input source, reflected by {@link #getLastModified()}.
		 */
        protected void recordModification() {
            this.modified = System.currentTimeMillis();
        }
    }
}
