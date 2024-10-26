package net.sourceforge.retriever.collector.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.retriever.collector.handler.Document;

class HTTPResource extends Resource {

    private Document resourceData;

    HTTPResource(final URL url) {
        super(url);
    }

    @Override
    public Document getData() throws FileNotFoundException, ConnectException, IOException {
        if (this.resourceData != null) return this.resourceData;
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) super.getURL().openConnection();
            urlConnection.setConnectTimeout(30000);
            urlConnection.connect();
            if (HttpURLConnection.HTTP_NOT_FOUND == urlConnection.getResponseCode()) {
                throw new FileNotFoundException("The resource " + super.getURL().toString() + " couldn't be found.");
            }
            if (HttpURLConnection.HTTP_OK != urlConnection.getResponseCode()) {
                throw new ConnectException("Can't connect to " + super.getURL().toString() + "\nStatus code: " + urlConnection.getResponseCode());
            }
            this.resourceData = (Document) urlConnection.getContent();
            return this.resourceData;
        } finally {
            if (urlConnection != null) ((HttpURLConnection) urlConnection).disconnect();
        }
    }

    @Override
    public Iterator<Resource> childrenResources() throws FileNotFoundException {
        final List<Resource> resources = new ArrayList<Resource>();
        try {
            final Iterator<String> linksIterator = this.getData().linksIterator();
            while (linksIterator.hasNext()) {
                resources.add(new HTTPResource(new URL(linksIterator.next())));
            }
        } catch (final FileNotFoundException e) {
            throw e;
        } catch (final Exception e) {
        }
        return resources.iterator();
    }

    @Override
    public boolean canCollect() {
        try {
            this.getData();
            return true;
        } catch (final Exception e) {
            return false;
        }
    }
}
