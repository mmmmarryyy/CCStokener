package org.mcisb.web.servlet;

import java.io.*;
import java.net.*;
import javax.servlet.http.*;
import org.mcisb.util.io.*;

/**
 *
 * @author Neil Swainston
 */
public class XQueryServlet extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
	 * 
	 */
    public static final String HOST = "host";

    /**
	 * 
	 */
    public static final String PORT = "port";

    /**
	 * 
	 */
    public static final String DB = "db";

    /**
	 * 
	 */
    public static final String XQUERY = "xquery";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final String CONTENT_TYPE = "text/xml";
        resp.setContentType(CONTENT_TYPE);
        final URL url = new URL("http", req.getParameter(HOST), Integer.parseInt(req.getParameter(PORT)), "/exist/rest/db/" + req.getParameter(DB) + "?_query=" + req.getParameter(XQUERY));
        new StreamReader(url.openStream(), resp.getOutputStream()).read();
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }
}
