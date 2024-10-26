package com.slychief.javalastfm;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Schindler
 */
public class URLGrabber {

    /**
     *
     * @param url
     * @return
     *
     * @throws IOException
     * @throws MalformedURLException
     */
    public static InputStream getDocumentAsInputStream(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        return getDocumentAsInputStream(u);
    }

    /**
     *
     * @param url
     * @return
     *
     * @throws IOException
     */
    public static InputStream getDocumentAsInputStream(URL url) throws IOException {
        InputStream in = url.openStream();
        return in;
    }

    /**
     * Method description
     *
     *
     * @param url
     *
     * @return
     */
    public static Document getDocument(URL url) {
        Document doc = null;
        try {
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(getInputStream(url));
        } catch (IOException ex) {
            Logger.getLogger(URLGrabber.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JDOMException ex) {
            Logger.getLogger(URLGrabber.class.getName()).log(Level.SEVERE, null, ex);
        }
        return doc;
    }

    /**
     * Method description
     *
     *
     *
     * @param httpget
     *
     * @return
     *
     * @throws IOException
     */
    public static String getDocumentAsString(HttpGet httpget) throws IOException {
        StringBuffer result = new StringBuffer();
        BufferedReader reader = getDocumentAsReader(httpget);
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    /**
     * Method description
     *
     *
     * @param httpget
     *
     * @return
     *
     * @throws IOException
     */
    public static BufferedReader getDocumentAsReader(HttpGet httpget) throws IOException {
        InputStream instream = getInputStream(httpget);
        return new BufferedReader(new InputStreamReader(instream, "UTF8"));
    }

    private static InputStream getInputStream(URL url) throws IOException {
        HttpGet httpget = new HttpGet(url.toExternalForm());
        return getInputStream(httpget);
    }

    private static InputStream getInputStream(HttpGet httpget) throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }
}
