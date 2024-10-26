import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import java.io.*;
import java.util.*;
import java.text.DateFormat;
import com.quiotix.html.parser.HtmlParser;
import com.quiotix.html.parser.HtmlDocument;

public class PageGrabber extends Thread {

    ThreadManager m_manager = null;

    PageStats m_stats = null;

    public PageGrabber(ThreadManager manager) {
        m_manager = manager;
        setPriority(MIN_PRIORITY);
    }

    public void reset(PageStats stats) {
        m_stats = stats;
    }

    public PageStats getStats() {
        return m_stats;
    }

    public void run() {
        m_stats.setRunning();
        URL url = m_stats.url;
        if (url != null) {
            try {
                URLConnection connection = url.openConnection();
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection = (HttpURLConnection) connection;
                    handleHTTPConnection(httpConnection, m_stats);
                } else {
                    System.out.println("Unknown URL Connection Type " + url);
                }
            } catch (java.io.IOException ioe) {
                m_stats.setStatus(m_stats.IOError);
                m_stats.setErrorString("Error making or reading from connection" + ioe.toString());
            }
        }
        m_stats.setDone();
        m_manager.threadFinished(this);
    }

    protected void handleHTTPConnection(HttpURLConnection connection, PageStats stats) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            stats.setStatus(stats.HTTPError);
            stats.setErrorString(connection.getResponseMessage());
        } else {
            stats.contentType = connection.getContentType();
            long lastModified = connection.getLastModified();
            if (lastModified > 0) {
                stats.date = new Date(lastModified);
            }
            CountingBufferedInputStream is = new CountingBufferedInputStream(connection.getInputStream());
            if (stats.contentType != null) {
                if (stats.contentType.equals("text/html")) {
                    handleHTML(is, stats);
                } else {
                }
                stats.size = connection.getContentLength();
            }
        }
        connection.disconnect();
    }

    protected void handleHTML(CountingBufferedInputStream is, PageStats stats) {
        HtmlParser parser = new HtmlParser(is);
        try {
            HtmlDocument doc = parser.HtmlDocument();
            doc.accept(new HtmlLinkGrabber(stats));
        } catch (com.quiotix.html.parser.ParseException e) {
            stats.setStatus(stats.HTMLParseError);
            stats.setErrorString(e.toString());
        } catch (com.quiotix.html.parser.TokenMgrError err) {
            stats.setStatus(stats.HTMLParseError);
            stats.setErrorString(err.toString());
        }
    }

    protected void handleUnparsedData(CountingBufferedInputStream is, PageStats stats) throws IOException {
        byte buffer[] = new byte[10000];
        while (is.read(buffer) != -1) {
        }
    }

    public String toString() {
        return m_stats.toString();
    }
}
