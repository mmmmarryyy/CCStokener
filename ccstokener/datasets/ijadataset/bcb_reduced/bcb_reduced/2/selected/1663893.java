package net.sf.webwarp.modules.partner.partner.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;
import net.sf.webwarp.modules.partner.partner.MediaContent;
import org.apache.log4j.Logger;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Proxy;

/**
 * Media content that is stored as an external URL.
 * 
 * @author atr
 */
@Entity
@Proxy(lazy = false)
public class UrlMediaContent extends MediaContent {

    private static final long serialVersionUID = -1331834957711565866L;

    private static Logger log = Logger.getLogger(UrlMediaContent.class);

    private URL urlInstance;

    private String url;

    /**
	 * Get the URL.
	 * 
	 * @return
	 */
    @Transient
    public URL getUrl() {
        if (urlInstance == null) {
            try {
                urlInstance = new URL(url);
            } catch (Exception e) {
                log.error("Invalid url: " + url);
            }
        }
        return urlInstance;
    }

    /**
	 * Set the as an url instance
	 * 
	 * @param url
	 */
    public void setUrl(URL urlInstance) {
        this.urlInstance = urlInstance;
        if (urlInstance != null) {
            this.url = urlInstance.toExternalForm();
        } else {
            this.url = null;
        }
    }

    /**
	 * Get the url in String form.
	 * 
	 * @return
	 */
    @SuppressWarnings("unused")
    @Column(name = "url", length = 2000)
    public String getUrlString() {
        return url;
    }

    /**
	 * Set the internal URL
	 * 
	 * @param urlIntern
	 * @throws Exception
	 */
    @SuppressWarnings("unused")
    public void setUrlString(String url) {
        this.url = url;
    }

    /**
	 * Tries to read the data from the URL into a byte[].
	 * 
	 * @return The URL contents or null if the url was not set or invalid.
	 * @see net.sf.webwarp.partner.partner.impl.AMediaContent#getData()
	 */
    @Transient
    public byte[] getData() {
        InputStream is = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            is = urlInstance.openStream();
            byte[] bytes = new byte[1024 * 20];
            while (is.available() > 0) {
                int bytesRead = is.read(bytes);
                bos.write(bytes, 0, bytesRead);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Error reading URL: " + url, e);
            return null;
        } finally {
            if (is != null) try {
                is.close();
            } catch (Exception e) {
                log.debug("Error closing URL input stream: " + url, e);
            }
        }
    }
}
