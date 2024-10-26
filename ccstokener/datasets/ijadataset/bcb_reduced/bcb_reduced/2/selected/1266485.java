package org.tolven.web.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import org.tolven.core.TolvenPropertiesLocal;

public abstract class TolvenServlet extends HttpServlet {

    protected static final String RESOURCE_OVERRIDE = "tolven.web.resources";

    private static final int BUFFER_SIZE = 4096;

    @EJB
    private TolvenPropertiesLocal propertiesBean;

    public TolvenPropertiesLocal getPropertiesBean() {
        return propertiesBean;
    }

    protected void copyStream(InputStream istream, OutputStream ostream) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(istream, BUFFER_SIZE);
        byte buffer[] = new byte[2048];
        int len = buffer.length;
        while (true) {
            len = bis.read(buffer);
            if (len == -1) break;
            ostream.write(buffer, 0, len);
        }
    }

    /**
     * Combine the contextPath with the specified path. For this purpose, we
     * remove the leading "/" to make the supplied path relative to the context path.
     * @param ContextPath From "tolven.web.resources" property
     * @param path Must begin with "/"
     * @return
     * @throws MalformedURLException 
     */
    protected static InputStream tryURL(String contextPath, String path) {
        try {
            URL ctx = new URL(contextPath);
            URL url = new URL(ctx, path.substring(1));
            InputStream is = url.openStream();
            return is;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Open a resource from a branded location, general resource override location, or a normal location within war.
     * @param resourceName
     * @param localAddr
     * @param servletContext
     * @return An input stream if open successfully.
     */
    protected InputStream openResourceAsStream(String resourceName, String localAddr, ServletContext servletContext) {
        InputStream stream = null;
        String path = getPropertiesBean().getProperty(RESOURCE_OVERRIDE + "." + localAddr);
        if (path != null) {
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            stream = tryURL(path, resourceName);
        }
        if (stream == null) {
            path = getPropertiesBean().getProperty(RESOURCE_OVERRIDE);
            if (path != null) {
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                stream = tryURL(path, resourceName);
            }
        }
        if (stream == null) {
            stream = servletContext.getResourceAsStream(resourceName);
        }
        return stream;
    }
}
