package org.evertree.breakfast.component;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import org.evertree.breakfast.Component;
import org.evertree.breakfast.Parameter;
import org.evertree.breakfast.Provider;
import org.evertree.breakfast.UnsetParameterException;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class SyndFeedSource extends Component implements Provider {

    private Parameter url = new Parameter("url", String.class);

    private Parameter authenticator = new Parameter("authenticator", BasicAuthenticator.class);

    private SyndFeed feed;

    public void setUrl(Object url) {
        this.url.setValue(url);
    }

    public void setAuthenticator(Object authenticator) {
        this.authenticator.setValue(authenticator);
    }

    @Override
    public void execute() throws Exception {
        if (url.isNull()) {
            throw new UnsetParameterException("url");
        }
        if (!authenticator.isNull()) {
            Authenticator.setDefault(getActualAuthenticator());
        }
        SyndFeedInput feedInput;
        URLConnection conn = new URL((String) url.getValue()).openConnection();
        conn.addRequestProperty("User-Agent", "Mozilla/4.76");
        feedInput = new SyndFeedInput();
        feed = feedInput.build(new XmlReader(conn));
    }

    @Override
    public Object provide() {
        return feed;
    }

    private Authenticator getActualAuthenticator() {
        final BasicAuthenticator basicAuthenticator = (BasicAuthenticator) authenticator.getValue();
        return new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(basicAuthenticator.getUsername(), basicAuthenticator.getPassword().toCharArray());
            }
        };
    }
}
