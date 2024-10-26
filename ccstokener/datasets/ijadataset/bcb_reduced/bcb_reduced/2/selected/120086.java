package net.sf.jasperreports.jsf.test.matchers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

/**
 *
 * @author aalonsodominguez
 */
public class ExistsURL extends BaseMatcher<URL> {

    @Factory
    public static Matcher<URL> existsURL() {
        return new ExistsURL();
    }

    public boolean matches(Object item) {
        if (item == null) return false;
        URL url;
        if (item instanceof URL) {
            url = (URL) item;
        } else if (item instanceof String) {
            String urlAsText = (String) item;
            try {
                url = new URL(urlAsText);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(urlAsText, e);
            }
        } else {
            throw new IllegalArgumentException("illegal item type: " + item.getClass().getName());
        }
        try {
            URLConnection conn = url.openConnection();
            conn.getInputStream();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public void describeTo(Description description) {
        description.appendText("exists url");
    }
}
