package net.infordata.ifw2.web.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.infordata.ifw2.util.URLFile;
import net.infordata.ifw2.web.util.JSDummyParser.Macro;

/**
 * 
 * @author valentino.proietti
 */
public class JSServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(JSServlet.class);

    private static final Logger JSLOGGER = LoggerFactory.getLogger("net.infordata.ifw2.js.Client");

    private URLFile ivSource;

    private volatile long ivLastModified;

    private String ivJS;

    private final boolean ivPreserveComments = false;

    private final MacroProcessor ivMacroProcessor = new MacroProcessor();

    private String ivContextPath;

    /**
   * fix: WAS uses wsjar protocol instead of jar, convert it to the expected protocol. 
   */
    static URL forceJarProtocol(URL url) throws MalformedURLException {
        if (!"jar".equals(url.getPath())) {
            if (LOGGER.isInfoEnabled()) LOGGER.info("Translating url: " + url);
            url = new URL("jar:" + url.getPath());
            if (LOGGER.isInfoEnabled()) LOGGER.info("to: " + url);
        }
        return url;
    }

    /** Initialize global variables*/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String fileName = config.getInitParameter("url");
        String resource = config.getInitParameter("resource");
        String warResource = config.getInitParameter("warResource");
        if (fileName != null && (resource != null || warResource != null)) throw new ServletException("Specify only a file-name, a resource or a webResource.");
        try {
            URL url = (resource != null) ? JSServlet.class.getResource(resource) : (fileName != null) ? new URL(fileName) : (warResource != null) ? getServletContext().getResource(warResource) : null;
            if (url == null) throw new ServletException("Specify file name or resource");
            if (resource != null) {
                url = forceJarProtocol(url);
            }
            ivSource = new URLFile(url);
            if (!ivSource.exists()) throw new ServletException("File does't exist: " + url);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        } catch (IOException ex) {
            throw new ServletException(ex);
        }
    }

    /** Clean up resources*/
    @Override
    public void destroy() {
    }

    /** Process the HTTP Get request*/
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (ivLastModified != ivSource.lastModified()) {
            synchronized (this) {
                ivContextPath = request.getContextPath() == null ? "" : request.getContextPath();
                LOGGER.info("reloading ... " + ivSource.getURL());
                Reader is = new BufferedReader(new InputStreamReader(ivSource.getInputStream()));
                ivJS = JSDummyParser.parse(is, ivMacroProcessor, ivPreserveComments, JSLOGGER.isDebugEnabled());
                is.close();
                ivLastModified = ivSource.lastModified();
            }
        }
        response.setHeader("Cache-Control", "max-age=" + (60 * 30) + ", must-revalidate");
        response.setContentType("text/javascript");
        response.setCharacterEncoding("ISO-8859-1");
        response.getWriter().print(ivJS);
        response.flushBuffer();
    }

    /** Process the HTTP Post request*/
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    private class MacroProcessor implements JSDummyParser.MacroProcessor {

        @Override
        public String processMacro(Macro macro) {
            if ("include".equals(macro.getId())) {
                String fileName = macro.getParams()[0];
                try {
                    URL url = new URL(ivSource.getURL(), fileName);
                    Reader is = new BufferedReader(new InputStreamReader(url.openStream()));
                    String res = JSDummyParser.parse(is, ivMacroProcessor, ivPreserveComments, JSLOGGER.isDebugEnabled());
                    is.close();
                    return res;
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else if ("logLevel".equals(macro.getId())) {
                return JSLOGGER.isDebugEnabled() ? "7" : JSLOGGER.isInfoEnabled() ? "6" : JSLOGGER.isWarnEnabled() ? "4" : JSLOGGER.isErrorEnabled() ? "3" : "0";
            } else if ("contextPath".equals(macro.getId())) {
                return "'" + ivContextPath + "'";
            } else if ("initParameter".equals(macro.getId())) {
                String propName = macro.getParams()[0];
                String propValue = (propName == null) ? null : getServletConfig().getInitParameter(propName);
                return propValue == null ? "null" : "'" + propValue + "'";
            }
            return null;
        }
    }
}
