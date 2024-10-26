package info.metlos.jcdc.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author metlos
 * 
 * @version $Id: AbstractURLConnectionTracker.java 38 2007-01-16 21:41:53Z
 *          metlos $
 */
public abstract class AbstractURLConnectionTracker<T> extends AbstractProgressTracker<T> {

    private URLConnection connection;

    private AbstractURLConnectionTracker(URLConnection connection) {
        super(connection.getContentLength());
        this.connection = connection;
    }

    /**
	 * @param url
	 *            the url to connect to.
	 * @throws IOException
	 *             if attempt to open a connection to given url fails.
	 */
    public AbstractURLConnectionTracker(URL url) throws IOException {
        this(url.openConnection());
    }

    /**
	 * Starts the download. This method reads the HTTP response and informs the
	 * listeners about the progress
	 * 
	 * @see info.metlos.jcdc.util.IProgressTracker#run()
	 */
    public void run() {
        try {
            connection.connect();
            setMaximum(connection.getContentLength());
            initializeForDownload();
            byte[] buffer = new byte[2048];
            InputStream dta = connection.getInputStream();
            int cnt = 0;
            int total = 0;
            while ((cnt = dta.read(buffer)) != -1) {
                handleIncomingData(buffer, cnt);
                notifyListeners(total += cnt);
            }
            finalizeAfterDownload();
        } catch (IOException e) {
            setMaximum(0);
            handleInterruptedDownload();
            notifyListeners(getMaximum());
        }
    }

    /**
	 * @return the connection
	 */
    protected URLConnection getConnection() {
        return connection;
    }

    /**
	 * This method is called during the {@link #run()} method after the
	 * connection was opened and maximum was set ({@link #setMaximum(int)}) but
	 * before any data was read.
	 */
    protected abstract void initializeForDownload();

    /**
	 * This method is called once all the data was successfully read in
	 * {@link #run()} method.
	 */
    protected abstract void finalizeAfterDownload();

    /**
	 * This method is called in case of some IOException during the download in
	 * {@link #run()}. It is called after the maximum was set back to 0, but
	 * before the listeners are notified.
	 */
    protected abstract void handleInterruptedDownload();

    /**
	 * This method is called during the {@link #run()} each time some data is
	 * read from the connection just before the listeners are notified.
	 * 
	 * @param buffer
	 * @param cnt
	 *            how many bytes were read into the buffer
	 */
    protected abstract void handleIncomingData(byte[] buffer, int cnt);
}
