package annone.client.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import annone.util.Safe;
import annone.util.Tools;

public class Resource extends MyHttpServlet {

    private static final long serialVersionUID = 7843261824031016052L;

    private String getResourcePath(HttpServletRequest req) {
        String resourcePath = req.getPathInfo();
        if (Safe.isEmpty(resourcePath)) return resourcePath; else return resourcePath.substring(1);
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        String resourcePath = getResourcePath(req);
        URL url = getClass().getResource(resourcePath);
        if (url == null) return -1;
        try {
            long lastModified = url.openConnection().getLastModified();
            return (lastModified == 0) ? -1 : lastModified;
        } catch (Exception xp) {
            return -1;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String resourcePath = getResourcePath(req);
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        URLConnection conn = url.openConnection();
        resp.setContentType(conn.getContentType());
        int contentLength = conn.getContentLength();
        if (contentLength >= 0) resp.setContentLength(contentLength);
        InputStream in = conn.getInputStream();
        ServletOutputStream out = resp.getOutputStream();
        Tools.transfer(in, out);
        in.close();
        out.close();
    }
}
