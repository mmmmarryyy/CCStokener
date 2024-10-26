package redrocket.vfs;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * <p>
 * Note that HTTP entries cannot be directories - they are always single files.
 * </p>
 */
public class HttpEntry extends VFSEntry {

    public HttpEntry(String url) throws IOException {
        this(new URL(url));
    }

    public HttpEntry(URL url) {
        super(url.getFile().split("/")[url.getFile().split("/").length - 1]);
        this.url = url;
    }

    private URL url = null;

    private InputStream inputStream = null;

    /**
	 * Close any open input and output streams if there are any.
	 *
	 * It's safe to call this method many times and do it when no streams are open.
	 *
	 * A new stream can be opened again after call to this method so it's good idea
	 * to call it as soon as no more reading or writing is scheduled for a while.
	 */
    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
        inputStream = null;
    }

    @Override
    public InputStream read() throws IOException {
        inputStream = url.openStream();
        return new BufferedInputStream(inputStream);
    }

    @Override
    public String toString() {
        return "[" + this.getClass().getName() + " name=" + getName() + "]";
    }
}
