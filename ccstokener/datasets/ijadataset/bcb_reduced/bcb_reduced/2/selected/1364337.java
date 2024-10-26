package desktop;

import org.wings.SLabel;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.net.URL;
import java.net.URLConnection;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author hengels
 */
public class RSSPortlet extends Bird {

    private String feed;

    private String user;

    private String password;

    public RSSPortlet(String name, String feed) {
        this(name, feed, null, null);
    }

    public RSSPortlet(String name, String feed, String user, String password) {
        setTitle(name);
        this.feed = feed;
        this.user = user;
        this.password = password;
        getContentPane().add(new SLabel("<html>" + getNews()));
    }

    String getNews() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            TransformerFactory tFactory = TransformerFactory.newInstance();
            String ctx = getSession().getServletContext().getRealPath("") + System.getProperty("file.separator");
            Source xslSource = new StreamSource(new URL("file", "", ctx + "rss.xsl").openStream());
            Transformer transformer = tFactory.newTransformer(xslSource);
            copy(openFeed(), System.out);
            Source xmlSource = new StreamSource(openFeed());
            transformer.transform(xmlSource, new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    private void copy(InputStream in, PrintStream out) throws IOException {
        byte[] buffer = new byte[256];
        int len;
        while ((len = in.read(buffer)) > -1) out.write(buffer, 0, len);
    }

    private InputStream openFeed() throws IOException {
        URL url = new URL(feed);
        URLConnection connection = url.openConnection();
        connection.setDoInput(true);
        if (user != null) {
            String userPassword = user + ":" + password;
            String encoding = new sun.misc.BASE64Encoder().encode(userPassword.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encoding);
        }
        return connection.getInputStream();
    }
}
