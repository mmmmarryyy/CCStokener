package com.softwaresmithy.metadata.impl;

import android.net.Uri;
import android.util.Log;
import com.softwaresmithy.BookJB;
import com.softwaresmithy.metadata.MetadataProvider;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.xpath.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GoogleBooks implements MetadataProvider {

    private static XPathExpression titleXpath;

    private static XPathExpression authorXpath;

    private static XPathExpression volumeIdXpath;

    private static XPathExpression thumbXpath;

    private static XPathExpression entityXpath;

    private HttpClient client;

    public GoogleBooks() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new GoogleBooksNamespaceContext());
        try {
            entityXpath = xpath.compile("/atom:feed/atom:entry[1]");
            titleXpath = xpath.compile("dc:title");
            authorXpath = xpath.compile("dc:creator");
            volumeIdXpath = xpath.compile("dc:identifier[1]/text()");
            thumbXpath = xpath.compile("atom:link[@rel=\"http://schemas.google.com/books/2008/thumbnail\"]/@href");
        } catch (XPathException e) {
            Log.e(this.getClass().getName(), "Something bad happened building xpath constants", e);
        }
        client = new DefaultHttpClient();
    }

    @Override
    public BookJB getInfo(String isbn) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("q", isbn));
        try {
            URI uri = URIUtils.createURI("http", "books.google.com", -1, "/books/feeds/volumes", URLEncodedUtils.format(params, "UTF-8"), null);
            HttpResponse resp = client.execute(new HttpGet(uri));
            Node bookNode = (Node) entityXpath.evaluate(new InputSource(resp.getEntity().getContent()), XPathConstants.NODE);
            NodeList titleNodes = (NodeList) titleXpath.evaluate(bookNode, XPathConstants.NODESET);
            String title = concatNodes(titleNodes, ": ");
            NodeList authorNodes = (NodeList) authorXpath.evaluate(bookNode, XPathConstants.NODESET);
            String author = concatNodes(authorNodes, ", ");
            String volumeId = (String) volumeIdXpath.evaluate(bookNode, XPathConstants.STRING);
            Log.d(this.getClass().getName(), "volumeId: " + volumeId);
            String thumbUrl = (String) thumbXpath.evaluate(bookNode, XPathConstants.STRING);
            thumbUrl = thumbUrl.replaceFirst("&edge(=[^&]*)?(?=&|$)|^edge(=[^&]*)?(&|$)", "");
            BookJB retVal = new BookJB(isbn, volumeId, title, author);
            retVal.setThumbUrl(thumbUrl);
            return retVal;
        } catch (Exception e) {
            Log.e(this.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }

    private String concatNodes(NodeList nodes, String sep) {
        StringBuilder str = new StringBuilder("");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node titleNode = nodes.item(i);
            if (str.length() > 0) {
                str.append(sep);
            }
            str.append(titleNode.getTextContent());
        }
        return str.toString();
    }

    public HttpClient getClient() {
        return client;
    }

    public void setClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public Uri getBookInfoPage(BookJB book) {
        return Uri.parse("http://books.google.com/books?id=" + book.getVolumeId());
    }

    @Override
    public String getProviderName() {
        return "Google Books";
    }
}
