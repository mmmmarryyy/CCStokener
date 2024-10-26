package net.sf.sageplugins.webserver.groovy.servlets;

import groovy.servlet.GroovyServlet;
import groovy.util.ResourceException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public final class SageGroovyServlet extends GroovyServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public URLConnection getResourceConnection(String name) throws ResourceException {
        try {
            URLConnection c = super.getResourceConnection(name);
            return c;
        } catch (ResourceException e) {
            while (name.startsWith(ServletHelpers.SCRIPT_DIR.getAbsolutePath())) name = name.substring(ServletHelpers.SCRIPT_DIR.getAbsolutePath().length());
            name = name.replaceAll("\\\\", "/");
            if (name.startsWith("/")) name = name.substring(1);
            File script = new File(ServletHelpers.SCRIPT_DIR, name);
            if (script.canRead()) {
                try {
                    URL url = new URL("file", "", script.getAbsolutePath());
                    return url.openConnection();
                } catch (IOException x) {
                    throw new ResourceException("IOError", x);
                }
            } else throw new ResourceException(String.format("Script not found! [%s]", script.getAbsolutePath()));
        }
    }
}
