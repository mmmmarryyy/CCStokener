package net.sourceforge.javautil.web.server.application.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.javautil.common.IOUtil;

/**
 * The default servlet for resource handling.
 * 
 * @author ponder
 * @author $Author: ponderator $
 * @version $Id: DefaultServlet.java 1157 2009-10-23 17:02:21Z ponderator $
 */
public class DefaultServlet extends HttpServlet {

    protected final Logger log = LoggerFactory.getLogger(DefaultServlet.class.getName());

    protected final SimpleDateFormat httpDate = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss zzz", Locale.US);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.serviceRequest(req, resp, true);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.serviceRequest(req, resp, false);
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        try {
            ServletContext ctx = this.getServletContext();
            URL url = ctx.getResource(req.getServletPath());
            if (url == null) return -1;
            return url.openConnection().getLastModified();
        } catch (IOException e) {
            return -1;
        }
    }

    /**
	 * Resolve the request URL and map it to a local resource.
	 * 
	 * @param req The request
	 * @param resp The response
	 * @param content True if content should be generated, otherwise just the header
	 * @throws ServletException 
	 * @throws IOException
	 */
    protected void serviceRequest(HttpServletRequest req, HttpServletResponse resp, boolean content) throws ServletException, IOException {
        ServletContext ctx = this.getServletContext();
        InputStream is = null;
        try {
            is = ctx.getResourceAsStream(req.getServletPath());
            if (is == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                String type = ctx.getMimeType(req.getServletPath());
                if (type == null) type = "text/plain";
                byte[] data = IOUtil.read(is);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentLength(data.length);
                resp.setContentType(type);
                resp.setHeader("Last-Modified", httpDate.format(new Date(this.getLastModified(req))));
                if (content) resp.getOutputStream().write(data);
            }
        } catch (RuntimeException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
        }
    }
}
