package de.huxhorn.whistler.services;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.codehaus.stax2.XMLStreamReader2;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class RealurlUrlUnshortener implements UrlUnshortener {

    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RealurlUrlUnshortener.class);

    public String unshorten(String url) {
        XMLStreamReader2 xmlStreamReader = null;
        try {
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("url", url));
            BasicHttpParams params = new BasicHttpParams();
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            URI uri = URIUtils.createURI("http", "realurl.org", -1, "/api/v1/getrealurl.php", URLEncodedUtils.format(qparams, "UTF-8"), null);
            HttpGet httpget = new HttpGet(uri);
            if (logger.isDebugEnabled()) logger.debug("HttpGet.uri={}", httpget.getURI());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                xmlStreamReader = (XMLStreamReader2) WstxInputFactory.newInstance().createXMLStreamReader(instream);
                while (xmlStreamReader.hasNext()) {
                    int type = xmlStreamReader.next();
                    if (type == XMLStreamConstants.START_ELEMENT) {
                        if ("real".equals(xmlStreamReader.getName().getLocalPart())) {
                            return xmlStreamReader.getElementText();
                        }
                    }
                }
            }
        } catch (IOException ex) {
            if (logger.isWarnEnabled()) logger.warn("Exception!", ex);
        } catch (URISyntaxException ex) {
            if (logger.isWarnEnabled()) logger.warn("Exception!", ex);
        } catch (XMLStreamException ex) {
            if (logger.isWarnEnabled()) logger.warn("Exception!", ex);
        } finally {
            if (xmlStreamReader != null) {
                try {
                    xmlStreamReader.closeCompletely();
                } catch (XMLStreamException e) {
                }
            }
        }
        return null;
    }
}
