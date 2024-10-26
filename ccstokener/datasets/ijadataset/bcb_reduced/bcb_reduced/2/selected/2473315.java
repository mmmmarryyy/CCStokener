package irc;

import java.io.*;
import java.applet.*;
import java.net.*;

/**
 * File handling from applet.
 */
public class AppletFileHandler implements FileHandler {

    private Applet _app;

    /**
	 * Create a new AppletFileHandler, using the given Applet.
	 * @param app the applet to use.
	 */
    public AppletFileHandler(Applet app) {
        _app = app;
    }

    public InputStream getInputStream(String fileName) {
        try {
            URL url = new URL(_app.getCodeBase(), fileName);
            return url.openStream();
        } catch (Exception ex) {
            return null;
        }
    }
}
