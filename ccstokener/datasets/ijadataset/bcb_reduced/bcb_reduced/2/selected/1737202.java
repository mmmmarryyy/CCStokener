package shenbo.fetionlib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import shenbo.fetionlib.action.FetionAction;
import shenbo.fetionlib.response.FetionResponse;
import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Wraps around a SSL socket
 */
public class FetionConnection {

    private static Logger logger = LoggerFactory.getLogger(FetionConnection.class);

    static {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        } };
        try {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            System.out.println("Error setting SSL Socket Factory" + e);
        }
    }

    private static final String DEFAULT_FETION_URL = "nav.fetion.com.cn";

    private String fetionUrl;

    public FetionConnection() {
        this.fetionUrl = DEFAULT_FETION_URL;
    }

    public FetionConnection(String fetionUrl) {
        this.fetionUrl = fetionUrl;
    }

    public <T extends FetionResponse> T executeAction(FetionAction<T> fetionAction) throws IOException {
        URL url = new URL(fetionAction.getProtocol().name().toLowerCase() + "://" + fetionUrl + fetionAction.getRequestData());
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buffer = new byte[10240];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int read = 0;
        while ((read = in.read(buffer)) > 0) {
            bout.write(buffer, 0, read);
        }
        return fetionAction.processResponse(parseRawResponse(bout.toByteArray()));
    }

    private Document parseRawResponse(byte[] bytes) throws IOException {
        try {
            logger.debug("RawResponse: {}", new String(bytes));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(bytes));
        } catch (ParserConfigurationException e) {
            throw new IOException("Error instantiaion document parser", e);
        } catch (SAXException e) {
            throw new IOException("Error parsing server response", e);
        }
    }
}
