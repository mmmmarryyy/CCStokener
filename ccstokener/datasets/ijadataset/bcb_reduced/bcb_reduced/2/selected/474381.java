package x.java.io;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import x.java.nio.channels.FileChannel;

/**
 * @author qiangli
 *
 */
public class FileOutputStream extends java.io.FileOutputStream {

    protected OutputStream os = null;

    protected x.java.io.File file = null;

    protected boolean append = false;

    /**
	 * @param file
	 * @throws FileNotFoundException
	 */
    public FileOutputStream(java.io.File file) throws FileNotFoundException {
        this(file, false);
    }

    /**
	 * @param name
	 * @throws FileNotFoundException
	 */
    public FileOutputStream(String name) throws FileNotFoundException {
        this(new File(name), false);
    }

    public FileOutputStream(java.io.File file, boolean append) throws FileNotFoundException {
        super(new FileDescriptor());
        this.file = (x.java.io.File) file;
        this.append = append;
        this.file.checkWritable();
        if (!this.file.exists()) {
            try {
                this.file.createNewFile();
            } catch (IOException e) {
                throw new FileNotFoundException(e.getMessage());
            }
        }
        try {
            URL url = this.file.toURL();
            String oldContent = null;
            if (append) {
                oldContent = (String) url.getContent();
            }
            this.os = url.openConnection().getOutputStream();
            if (append && oldContent != null && oldContent.length() > 0) {
                this.os.write(oldContent.getBytes());
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException(this.file.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        this(new File(name), append);
    }

    public void close() throws IOException {
        os.close();
    }

    public java.nio.channels.FileChannel getChannel() {
        try {
            java.io.File cache = java.io.File.createTempFile("vfs", ".channel");
            return new FileChannel(file, new java.io.FileOutputStream(cache).getChannel());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
    }

    public void write(byte[] b) throws IOException {
        os.write(b);
    }

    public void write(int b) throws IOException {
        os.write(b);
    }

    public void flush() throws IOException {
        os.flush();
    }

    public boolean isAppend() {
        return this.append;
    }

    public java.io.File getFile() {
        return this.file;
    }
}
