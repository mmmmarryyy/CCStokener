package webusic;

import java.io.*;
import java.net.*;

/**
 * A single download able to retrieve an url.
 */
public class Download {

    private byte[] buf;

    private boolean open;

    private boolean ended;

    private boolean started;

    private long written;

    private URL url;

    private InputStream in;

    private BufferedOutputStream out;

    /**
	 * Creates a download.
	 * 
	 * @param url The source url.
	 * @param out Where to write the data.
	 */
    public Download(URL url, BufferedOutputStream out) {
        assert url != null;
        assert out != null;
        this.out = out;
        this.url = url;
        written = 0;
        open = false;
        ended = false;
        started = false;
    }

    /**
	 * (Re)start a download from its current position.
	 * 
	 * @throws An exception if the file cannot be downloaded.
	 */
    public void start() throws IllegalStateException {
        try {
            in = url.openStream();
            in.skip(written);
        } catch (IOException e) {
            throw new IllegalStateException(e.getLocalizedMessage());
        }
        open = true;
        started = true;
    }

    /**
	 * Stop the download without freeing the resources.
	 */
    public void pause() {
        if (!open) {
            return;
        }
        write();
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
        }
        open = false;
    }

    /**
	 * Write the available amount of data into the output stream and return
	 * the amount of written data. If the download is paused, do nothing.
	 * 
	 * @return The amount of written data (in bytes).
	 */
    public long write() {
        if (!open) {
            return 0;
        }
        int downloaded = 0;
        try {
            if (buf == null || buf.length < in.available()) {
                buf = new byte[in.available()];
            }
            downloaded = in.read(buf);
            if (downloaded != -1) {
                out.write(buf);
                written += downloaded;
            } else {
                ended = true;
            }
        } catch (IOException e) {
            return 0;
        }
        return downloaded;
    }

    /**
	 * Free all the resources cleanly.
	 */
    public void dispose() {
        open = false;
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
        }
        ;
        try {
            out.flush();
        } catch (IOException e) {
        }
        try {
            out.close();
        } catch (IOException e) {
        }
        ;
    }

    /**
	 * Returns true if the download is finished.
	 */
    public boolean isFinnished() {
        return ended;
    }

    /**
	 * Returns true if the download is initialized (started at least one time).
	 */
    public boolean isInitialized() {
        return started;
    }
}
