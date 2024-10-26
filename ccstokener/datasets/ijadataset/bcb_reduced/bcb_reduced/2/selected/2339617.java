package net.sourceforge.retriever.collector;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

/**
 * Represents a resource, like a web page, a data base register,
 * or a file in the disk.
 */
public abstract class Resource {

    private URL url;

    private URLConnection urlConnectionToResource;

    abstract Iterator<Resource> iterator();

    abstract boolean canCollect();

    /**
	 * Returns an Object containing the content of the resource.
	 * 
	 * For more details on this method, please refer to the javadoc
	 * of the class java.net.URLConnection#getContent().
	 *
	 * @see java.net.URLConnection#getContent()
	 * @return An Object containing the content of the resource.
	 * @throws IOException Throw if the method can't get the content of
	 *         the resource.
	 */
    public Object getContent() throws IOException {
        try {
            this.urlConnectionToResource = url.openConnection();
            this.urlConnectionToResource.connect();
            return this.urlConnectionToResource.getContent();
        } finally {
            this.release();
        }
    }

    /**
	 * Returns a String representation of the url to the resource.
	 * 
	 * @return A String representation of the url to the resource.
	 */
    public String getURL() {
        return this.url.toString();
    }

    /**
	 * Sets a URL.
	 * 
	 * @param url The URL to be settled.
	 */
    protected void setUrl(final URL url) {
        this.url = url;
    }

    /**
	 * Gets an URLConnection instance from the URL previously settled.
	 * 
	 * @return An URLConnection instance from the URL previously settled.
	 */
    protected URLConnection getUrlConnectionToResource() {
        return this.urlConnectionToResource;
    }

    /**
	 * If there is any need for the resource to be released after the connect()
	 * method is called, the subclass that represents a specific resource must
	 * redefine this method.
	 */
    protected void release() {
    }
}
