package org.eiichiro.jazzmaster.examples.petstore.controller.actions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eiichiro.jazzmaster.examples.petstore.controller.ControllerAction;
import org.eiichiro.jazzmaster.examples.petstore.controller.ControllerServlet;
import org.eiichiro.jazzmaster.examples.petstore.util.PetstoreUtil;

/**
 * This action class provides the default handling of the controller
 * @author Inderjeet Singh
 */
public class DefaultControllerAction implements ControllerAction {

    private static String CACHE = "controller_cache";

    private static String CACHE_TIMES = "controller_cache_times";

    private final ServletContext context;

    public DefaultControllerAction(ServletContext context) {
        this.context = context;
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String command = request.getParameter("command");
        if ("content".equals(command)) {
            String target = request.getParameter("target");
            if (target != null) target = target.trim();
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            StringBuffer content = getResource(target, true, true);
            out.write(content.toString());
            out.close();
        }
    }

    @SuppressWarnings("unchecked")
    public StringBuffer getResource(String resource, boolean fromWeb, boolean cacheContent) {
        try {
            URL url = fromWeb ? context.getResource(resource) : ControllerServlet.class.getResource(resource);
            URLConnection con = url.openConnection();
            if (cacheContent) {
                HashMap<String, StringBuffer> cache = (HashMap<String, StringBuffer>) context.getAttribute(CACHE);
                HashMap<String, Long> cacheTimes = (HashMap<String, Long>) context.getAttribute(CACHE_TIMES);
                if (cache == null) {
                    cache = new HashMap<String, StringBuffer>();
                    cacheTimes = new HashMap<String, Long>();
                    context.setAttribute(CACHE, cache);
                    context.setAttribute(CACHE_TIMES, cacheTimes);
                }
                long lastModified = con.getLastModified();
                long cacheModified = 0;
                if (cacheTimes.get(resource) != null) {
                    cacheModified = ((Long) cacheTimes.get(resource)).longValue();
                }
                if (cacheModified < lastModified) {
                    StringBuffer buffer = getResource(con.getInputStream());
                    synchronized (cacheTimes) {
                        cacheTimes.put(resource, Long.valueOf(lastModified));
                    }
                    synchronized (cache) {
                        cache.put(resource, buffer);
                    }
                    return buffer;
                } else {
                    return (StringBuffer) cache.get(resource);
                }
            } else {
                return getResource(con.getInputStream());
            }
        } catch (Exception e) {
            PetstoreUtil.getLogger().log(Level.SEVERE, "ControllerServlet:loadResource error: Could not load", resource + " - " + e.toString());
        }
        return null;
    }

    private StringBuffer getResource(InputStream stream) {
        StringBuffer buffer = new StringBuffer();
        try {
            BufferedReader bufReader = new BufferedReader(new InputStreamReader(stream));
            String curLine;
            while (null != (curLine = bufReader.readLine())) {
                buffer.append(curLine + "\n");
            }
        } catch (IOException e) {
            PetstoreUtil.getLogger().log(Level.SEVERE, "ControllerServlet:loadResource from stream error", e);
        }
        return buffer;
    }
}
