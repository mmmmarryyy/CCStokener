package net.sf.sail.core.net;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * @author aunger
 *
 */
public class SailResourceLoader {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(SailResourceLoader.class.getName());

    private URL url;

    private long lastModified = 0;

    private String eTag;

    private int contentLength = -1;

    private boolean promptRetryQuit;

    private URLConnection connection;

    private InputStream urlInputStream;

    private int tryCount;

    private int responseCode = -1;

    private static int ATTEMPTS_BEFORE_PROMPTING = 2;

    private static boolean silentMode = Boolean.getBoolean("sail.rloader.silent");

    protected SailResourceLoader(URL url, boolean promptRetryQuit) {
        this.url = url;
        this.promptRetryQuit = promptRetryQuit;
    }

    public URL getURL() {
        return url;
    }

    /**
	 * Get an input stream from a URL.  The simple way to do this is just:
	 * url.openConnection().getInputStream()
	 * 
	 * This method does more than that.  It sets some request properties indicating
	 * it wants an xml file, and it can handle gzip encoding.
	 * 
	 * Then it checks the response code.
	 * 
	 * Finally it checks if the returned contentEncoding is "gzip", in which case
	 * it uses a GZIPInputStream to uncompress the content.
	 * 
	 * FIXME It currently throws an ResourceLoadError if there is a problem.  That is a hold over
	 * from when this was a RequiredResourcedLoader.  If the resource isn't required it should
	 * throw something nicer.  Or the concept of required should be rethought a bit.
	 * 
	 * @param resourceUrl
	 * @return
	 */
    public InputStream getInputStream() throws ResourceLoadException {
        logger.info("loading: " + url.toString() + ", Thread: " + Thread.currentThread().getName());
        tryCount = 0;
        while (true) {
            HttpURLConnection httpConnection = null;
            try {
                connection = url.openConnection();
            } catch (IOException e) {
                throw new ResourceLoadException("Error opening connection", this, e, false);
            }
            connection.setRequestProperty("Accept", "application/xml");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            try {
                connection.connect();
            } catch (IOException e) {
                failed(e, "Error connecting", false);
                continue;
            }
            if (connection instanceof HttpURLConnection) {
                httpConnection = (HttpURLConnection) connection;
                try {
                    responseCode = httpConnection.getResponseCode();
                } catch (IOException e) {
                    failed(e, "Error getting response code", true);
                    continue;
                }
                if ((responseCode / 100) != 2) {
                    failed(null, "Non 2XX response code: " + responseCode, true);
                    continue;
                }
            }
            try {
                urlInputStream = connection.getInputStream();
            } catch (IOException e) {
                failed(e, "Error opening input stream", true);
                continue;
            }
            String encoding = connection.getContentEncoding();
            if (encoding != null && encoding.toLowerCase().equals("gzip")) {
                try {
                    urlInputStream = new GZIPInputStream(urlInputStream);
                } catch (IOException e) {
                    failed(e, "Error ungzipping", true);
                    continue;
                }
            }
            lastModified = connection.getLastModified();
            contentLength = connection.getContentLength();
            eTag = connection.getHeaderField("ETag");
            logger.finer("RequiredResourceLoader - Done.");
            return urlInputStream;
        }
    }

    private void failed(IOException e, String message, boolean connectionOpen) throws ResourceLoadException {
        tryCount++;
        logger.info("Failed attempt: " + message + ". Count: " + tryCount);
        if (tryCount >= ATTEMPTS_BEFORE_PROMPTING && !userRequestsRetry(e, message)) {
            throw new ResourceLoadException(message, this, e, connectionOpen);
        }
    }

    private boolean userRequestsRetry(final IOException e, final String message) {
        if (silentMode || !promptRetryQuit) {
            return false;
        }
        String[] options = new String[] { "Retry", "Quit", "Details" };
        final JOptionPane optionsPane = new JOptionPane("There was an error downloading one or more required resources.\nPlease ensure you are connected to the Internet and retry,\n or select quit and launch the project again.", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        final JDialog dialog = new JDialog((JFrame) null, "Download Error", true);
        dialog.setContentPane(optionsPane);
        optionsPane.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e1) {
                String prop = e1.getPropertyName();
                if (dialog.isVisible() && (e1.getSource() == optionsPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                    if (((String) e1.getNewValue()).equalsIgnoreCase("Details")) {
                        showErrorDetails(e);
                    } else {
                        dialog.setVisible(false);
                    }
                }
            }

            private void showErrorDetails(IOException e) {
                JTextArea textArea = new JTextArea(5, 10);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                String details = "Error downloading resource from " + url + "\n\n";
                details += "================\n\n";
                details += "Error:\n";
                details += message + "\n";
                if (e != null) {
                    details += e.getLocalizedMessage();
                }
                textArea.setText(details);
                JScrollPane scroll = new JScrollPane(textArea);
                textArea.setCaretPosition(0);
                dialog.getContentPane().add(scroll, BorderLayout.SOUTH);
                dialog.pack();
            }
        });
        dialog.pack();
        dialog.setVisible(true);
        String choice = (String) optionsPane.getValue();
        return choice.equalsIgnoreCase("Retry");
    }

    public long getLastModified() {
        return lastModified;
    }

    public int getContentLength() {
        return contentLength;
    }

    /**
	 * Returns -1 if there is no reponse code available.
	 * @return
	 */
    public int getHttpResponseCode() {
        return responseCode;
    }

    public void writeResourceErrorDetails(PrintWriter writer, boolean printBody) {
        HttpURLConnection httpConnection = null;
        if (connection instanceof HttpURLConnection) {
            httpConnection = (HttpURLConnection) connection;
        }
        if (httpConnection != null) {
            try {
                writer.println("Response code: " + httpConnection.getResponseCode());
            } catch (IOException ioe) {
            }
        }
        if (urlInputStream != null) {
            try {
                writer.println("available bytes in input stream: " + urlInputStream.available());
            } catch (IOException e1) {
            }
        }
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        if (headerFields != null) {
            for (Entry<String, List<String>> entry : headerFields.entrySet()) {
                writer.println(entry.getKey() + ": " + entry.getValue());
            }
        }
        if (!printBody) {
            return;
        }
        writer.println("==== error body ====");
        String encoding = connection.getContentEncoding();
        if (httpConnection != null) {
            InputStream errorStream = httpConnection.getErrorStream();
            if (errorStream != null) {
                try {
                    StreamUtil.writeFromStream(writer, errorStream, encoding);
                    return;
                } catch (IOException e) {
                    writer.println("Exception getting error body (errorStream): " + e);
                }
            }
        }
        if (urlInputStream == null) {
            try {
                urlInputStream = connection.getInputStream();
                StreamUtil.writeFromStream(writer, urlInputStream, encoding);
            } catch (Exception e) {
                writer.println("Exception getting error body (inputStream): " + e);
            }
        }
    }

    public String getETag() {
        return eTag;
    }
}
