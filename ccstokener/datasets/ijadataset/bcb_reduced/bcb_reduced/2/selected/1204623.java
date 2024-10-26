package uk.ac.ebi.rhea.webapp.pub.util;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.ac.ebi.rhea.webapp.pub.RheaConfig;

/**
 * Simple servlet which reads an external sitemap file and writes it as is to
 * the response. This is needed in order to have the sitemap decoupled from
 * the war file, as the sitemap specs require it to be under the same base
 * URL as the indexed URLs.
 */
public class SitemapServlet extends HttpServlet {

    private Logger LOGGER = Logger.getLogger(SitemapServlet.class);

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/gzip");
        LOGGER.info("Getting URL connection to sitemap...");
        ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        RheaConfig rheaConfig = (RheaConfig) context.getBean("rheaConfig");
        ;
        URL url = new URL(rheaConfig.getSitemapUrl());
        URLConnection con = url.openConnection(java.net.Proxy.NO_PROXY);
        LOGGER.info("Connecting to sitemap...");
        LOGGER.info("Connected");
        InputStream is = null;
        try {
            is = con.getInputStream();
            int r = -1;
            LOGGER.debug("Starting to read sitemap...");
            while ((r = is.read()) != -1) {
                response.getOutputStream().write(r);
            }
            response.getOutputStream().flush();
            response.flushBuffer();
            LOGGER.debug("... Read and served");
        } finally {
            if (is != null) is.close();
        }
    }
}
