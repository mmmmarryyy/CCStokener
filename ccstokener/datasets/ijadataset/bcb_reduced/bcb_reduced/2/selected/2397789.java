package vi;

import java.io.IOException;
import java.net.URL;

/**
 * Representing document on the web
 * 
 * 
 * @author Michal Laclavik
 * @author Miroslav Kacera
 *
 */
public class Doc {

    URL url = null;

    String title = null;

    String body = null;

    Link[] links = null;

    String mime = null;

    int depth;

    String alt = null;

    /**
	 * no-arg constructor should be provided when using JUNG
	 */
    public Doc() {
    }

    /**
	 * @param url
	 * @param title
	 * @param body
	 */
    public Doc(URL url, String title, String body) {
        super();
        this.url = url;
        this.title = title;
        this.body = body;
    }

    /**
	 * @param url
	 * @param title
	 * @param body
	 * @param links
	 */
    public Doc(URL url, String title, String body, Link[] links) {
        super();
        this.url = url;
        this.title = title;
        this.body = body;
        this.links = links;
    }

    /**
	 * @param url
	 * @param title
	 * @param depth
	 * @throws IOException
	 */
    public Doc(URL url, String title, int depth) throws IOException {
        this.setUrl(url);
        this.setTitle(title);
        this.setMime(getMIMEtype(this.url));
        this.depth = depth;
    }

    /**
	 * @param url
	 * @return mime type
	 * @throws IOException
	 */
    String getMIMEtype(URL url) throws IOException {
        return url.openConnection().getContentType();
    }

    /**
	 * @param mime
	 */
    public void setMime(String mime) {
        this.mime = mime;
    }

    /**
	 * @return mime
	 */
    public String getMime() {
        return mime;
    }

    /**
	 * @return depth
	 */
    public int getDepth() {
        return depth;
    }

    /**
	 * @return the url
	 */
    public URL getUrl() {
        return url;
    }

    /**
	 * @param url the url to set
	 */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
	 * @return the title
	 */
    public String getTitle() {
        return title;
    }

    /**
	 * @param title the title to set
	 */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
	 * @return the body
	 */
    public String getBody() {
        return body;
    }

    /**
	 * @param body the body to set
	 */
    public void setBody(String body) {
        this.body = body;
    }

    /**
	 * @return the links
	 */
    public Link[] getLinks() {
        return links;
    }

    /**
	 * @param links the links to set
	 */
    public void setLinks(Link[] links) {
        this.links = links;
    }

    @Override
    public String toString() {
        String result = this.title + ": " + this.url.toString();
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Doc other = (Doc) obj;
        if (url == null) {
            if (other.url != null) return false;
        } else if (!url.equals(other.url)) return false;
        return true;
    }

    /**
	 * @param attributeValue
	 */
    public void setALT(String attributeValue) {
        this.alt = attributeValue;
    }

    /**
	 * @return image's ALT
	 */
    public String getALT() {
        return this.alt;
    }
}
