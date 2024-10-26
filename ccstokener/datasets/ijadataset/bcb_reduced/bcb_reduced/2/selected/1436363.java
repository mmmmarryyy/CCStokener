package info.kmm.retriever.collector;

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

    /**
	 * Adds a resource as a child.
	 * 
	 * @param resource The resource to be added as a child.
	 */
    public abstract void add(Resource resource);

    /**
	 * Returns an Iterator of children resources.
	 * 
	 * @return An Iterator of children resources.
	 */
    public abstract Iterator<Resource> iterator();

    /**
	 * Tells the collector if the resource can be collected.
	 * 
	 * It's useful when dealing with directories, for instance. Most likely
	 * users will not want to collect them.
	 * 
	 * @return If the resource can be collected.
	 */
    public abstract boolean canCollect();

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

    protected void setUrl(final URL url) throws IOException {
        this.url = url;
    }

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
