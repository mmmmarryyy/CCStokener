package softwarekompetenz.core;

import java.applet.Applet;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;
import softwarekompetenz.workbench.RuntimeContext;

public class AppletRuntimeContext implements RuntimeContext {

    private Applet m_applet = null;

    public AppletRuntimeContext(Applet applet) {
        m_applet = applet;
    }

    public Image getImage(String filename) {
        try {
            URL url = new URL(m_applet.getCodeBase(), filename);
            return m_applet.getImage(url);
        } catch (IOException iox) {
            System.out.println("ERROR: " + iox.getMessage() + " filename:" + filename);
        }
        return null;
    }

    public InputStream getDataInputStream(String filename) {
        try {
            URL url = new URL(m_applet.getCodeBase(), filename);
            InputStream instream = url.openStream();
            return instream;
        } catch (IOException iox) {
            System.out.println("ERROR: " + iox.getMessage());
        }
        return null;
    }

    public String getProperty(String key) {
        return m_applet.getParameter(key);
    }

    public void showDocument(String urlString, String target) {
        URL url;
        try {
            url = new URL(urlString);
            System.out.println("Sende URL an Browser: " + urlString);
            m_applet.getAppletContext().showDocument(url, target);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
