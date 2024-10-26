package x.java.io;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author qiangli
 *
 */
public class FileWriter extends java.io.FileWriter {

    protected OutputStreamWriter osw = null;

    protected x.java.io.File file = null;

    /**
	 * @param file
	 * @throws IOException
	 */
    public FileWriter(java.io.File file) throws IOException {
        this(file, false);
    }

    /**
	 * @param fileName
	 * @throws IOException
	 */
    public FileWriter(String fileName) throws IOException {
        this(new File(fileName), false);
    }

    public FileWriter(java.io.File file, boolean append) throws IOException {
        super(new FileDescriptor());
        this.file = (x.java.io.File) file;
        this.file.checkWritable();
        if (!this.file.exists()) {
            this.file.createNewFile();
        }
        try {
            URL url = file.toURL();
            String oldContent = null;
            if (append) {
                oldContent = (String) url.getContent();
            }
            this.osw = new OutputStreamWriter(url.openConnection().getOutputStream());
            if (append && oldContent != null && oldContent.length() > 0) {
                this.osw.write(oldContent);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new FileNotFoundException(file.toString());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public FileWriter(String fileName, boolean append) throws IOException {
        this(new File(fileName), append);
    }

    public void close() throws IOException {
        osw.close();
    }

    public void flush() throws IOException {
        osw.flush();
    }

    public String getEncoding() {
        return osw.getEncoding();
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        osw.write(cbuf, off, len);
    }

    public void write(int c) throws IOException {
        osw.write(c);
    }

    public void write(String str, int off, int len) throws IOException {
        osw.write(str, off, len);
    }

    public void write(char[] cbuf) throws IOException {
        osw.write(cbuf);
    }

    public void write(String str) throws IOException {
        osw.write(str);
    }

    public java.io.File getFile() {
        return this.file;
    }
}
