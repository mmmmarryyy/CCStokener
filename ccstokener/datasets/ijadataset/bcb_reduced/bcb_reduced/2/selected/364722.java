package com.atosorigin.nl.jspring2008.buzzword.servlets;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

/**
 * @author a108600
 *
 */
public class DataProviderServlet extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 6958384224380743048L;

    private static final String CONTENT_ID = "contentId";

    private static final String CONTENT_TYPE = "contentType";

    private static final String BASE_URL = "dataProviderBaseUrl";

    enum ContentType {

        IMAGE, AUDIO, VIDEO
    }

    /**
	 * 
	 */
    public DataProviderServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String contentId = req.getParameter(CONTENT_ID);
        String contentType = req.getParameter(CONTENT_TYPE);
        if (contentId == null || contentType == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Content id or content type not specified");
            return;
        }
        try {
            switch(ContentType.valueOf(contentType)) {
                case IMAGE:
                    resp.setContentType("image/jpeg");
                    break;
                case AUDIO:
                    resp.setContentType("audio/mp3");
                    break;
                case VIDEO:
                    resp.setContentType("video/mpeg");
                    break;
                default:
                    throw new IllegalStateException("Invalid content type specified");
            }
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid content type specified");
            return;
        }
        String baseUrl = this.getServletContext().getInitParameter(BASE_URL);
        URL url = new URL(baseUrl + "/" + contentType.toLowerCase() + "/" + contentId);
        URLConnection conn = url.openConnection();
        resp.setContentLength(conn.getContentLength());
        IOUtils.copy(conn.getInputStream(), resp.getOutputStream());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
