package org.xorm.datastore.xml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * Wraps a JDOM Document and allows transactional access.
 *
 * @author Wes Biggs
 */
public class JDOMDocumentHolder implements DocumentHolder {

    private URL url;

    private Document document;

    public JDOMDocumentHolder() {
    }

    public void setURL(URL url) {
        this.url = url;
        try {
            document = new SAXBuilder().build(url);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a cloned copy of the document.
     */
    public Object checkout() {
        return document.clone();
    }

    /**
     * Accepts the changes from the document.  If possible, rewrites
     * the content.  Changes are synchronized but not checked; if two
     * concurrent threads make different changes, the last one to call
     * checkin() will win.  This effectively gives the process a
     * transaction isolation level equivalent to TRANSACTION_READ_COMMITTED.
     */
    public void checkin(Object _document) {
        this.document = (Document) _document;
        synchronized (url) {
            OutputStream outputStream = null;
            try {
                if ("file".equals(url.getProtocol())) {
                    outputStream = new FileOutputStream(url.getFile());
                } else {
                    URLConnection connection = url.openConnection();
                    connection.setDoOutput(true);
                    outputStream = connection.getOutputStream();
                }
                new XMLOutputter("  ", true).output(this.document, outputStream);
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
