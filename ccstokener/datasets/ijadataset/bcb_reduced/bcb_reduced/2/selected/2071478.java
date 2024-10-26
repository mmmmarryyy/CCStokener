package com.beimin.evedata.hsqldb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class HibSQLFile extends AbstractSQLFile {

    public HibSQLFile(File sqlFile) throws MalformedURLException {
        this(sqlFile.toURI().toURL());
    }

    public HibSQLFile(URL sqlURL) {
        super(sqlURL);
    }

    public void run(SessionFactory sessionFactory) throws IOException {
        Session session = sessionFactory.openSession();
        try {
            run(session);
        } finally {
            session.close();
        }
    }

    public void run(Session session) throws IOException, UnsupportedEncodingException {
        URLConnection urlConn = sqlURL.openConnection();
        urlConn.setUseCaches(false);
        InputStream is = urlConn.getInputStream();
        try {
            run(session, is);
        } finally {
            is.close();
        }
    }

    private void run(Session session, InputStream inputStream) throws UnsupportedEncodingException {
        StringIterator iterator = createIterator(inputStream);
        run(iterator, session);
    }

    private void run(StringIterator sqlIterator, Session session) {
        while (sqlIterator.hasNext()) {
            sql = sqlIterator.next();
            statementCount++;
            session.createSQLQuery(sql).executeUpdate();
        }
    }
}
