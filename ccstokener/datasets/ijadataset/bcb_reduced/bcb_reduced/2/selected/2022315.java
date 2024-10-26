package org.exist.xquery.modules.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.exist.EXistException;
import org.exist.security.Subject;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.xmldb.XmldbURI;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public class DBFile implements PartSource {

    private XmldbURI uri;

    private URLConnection connection = null;

    public DBFile(String uri) {
        this.uri = XmldbURI.create(uri);
    }

    private URLConnection getConnection() throws IOException {
        if (connection == null) {
            BrokerPool database = null;
            DBBroker broker = null;
            try {
                database = BrokerPool.getInstance();
                broker = database.get(null);
                Subject subject = broker.getSubject();
                URL url = new URL("xmldb:exist://jsessionid:" + subject.getSessionId() + "@" + uri.toString());
                connection = url.openConnection();
            } catch (IllegalArgumentException e) {
                throw new IOException(e);
            } catch (MalformedURLException e) {
                throw new IOException(e);
            } catch (EXistException e) {
                throw new IOException(e);
            } finally {
                if (database != null) database.release(broker);
            }
        }
        return connection;
    }

    @Override
    public InputStream createInputStream() throws IOException {
        return getConnection().getInputStream();
    }

    @Override
    public String getFileName() {
        return uri.lastSegment().toString();
    }

    @Override
    public long getLength() {
        try {
            return getConnection().getContentLength();
        } catch (IOException e) {
            return 0;
        }
    }
}
