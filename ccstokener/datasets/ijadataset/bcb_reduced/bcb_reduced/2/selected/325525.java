package org.nms.spider.helpers.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.nms.spider.beans.IElement;
import org.nms.spider.beans.impl.TypedElement;
import org.nms.spider.helpers.AbstractProcessor;
import org.nms.spider.helpers.IProcessorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Url processor.
 * <p>
 * Processes a URL and stores the HTML code in the resulting element.
 * </p>
 * <p>
 * Configurable : user agent for the connection and connection time out (in ms).
 * </p>
 * @author daviz
 *
 */
public class UrlProcessorImpl extends AbstractProcessor implements IProcessorHelper {

    private static final Logger log = LoggerFactory.getLogger(UrlProcessorImpl.class);

    /**
	 * User agent for url connection.
	 * <p>
	 * Default : Mozilla/4.0.
	 * </p>
	 */
    private String userAgent = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)";

    /**
	 * Time out for a url connection. In milliseconds.
	 * <p>
	 * Default : 10 secs (10000 ms)
	 * </p>
	 */
    private int connectionTimeOut = 10000;

    @SuppressWarnings("rawtypes")
    @Override
    public List<IElement> process(List<IElement> elements) {
        ArrayList<IElement> result = new ArrayList<IElement>();
        for (IElement el : elements) {
            String urlString = (String) el.getElement();
            log.info("Preprocessing url : " + urlString);
            TypedElement te = new TypedElement();
            te.setId(urlString);
            te.setType("htmlcontent");
            try {
                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                Set<String> keys = connection.getRequestProperties().keySet();
                for (String key : keys) {
                    log.debug("Key {} value {} ", key, connection.getRequestProperties().get(key));
                }
                connection.setAllowUserInteraction(false);
                connection.setReadTimeout(connectionTimeOut);
                connection.setDoOutput(true);
                connection.addRequestProperty("User-Agent", userAgent);
                if ((connection.getContentType() != null) && !connection.getContentType().toLowerCase().startsWith("text/")) {
                }
                InputStream is = connection.getInputStream();
                Reader r = new InputStreamReader(is);
                CharBuffer cbuff = CharBuffer.allocate(1000);
                StringBuffer sb = new StringBuffer();
                while (r.read(cbuff) != -1) {
                    log.trace("CBUFF-" + cbuff.length());
                    for (int i = 0; i < 1000 - cbuff.length(); i++) {
                        sb.append(cbuff.get(i));
                    }
                    sb.append(cbuff.toString());
                    cbuff = CharBuffer.allocate(1000);
                }
                log.trace("SB:" + sb.toString());
                te.setElement(sb.toString());
                result.add(te);
            } catch (Exception e) {
                log.error("Error trying to process the URL.", e);
            }
        }
        return result;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getConnectionTimeOut() {
        return connectionTimeOut;
    }

    public void setConnectionTimeOut(int connectionTimeOut) {
        this.connectionTimeOut = connectionTimeOut;
    }
}
