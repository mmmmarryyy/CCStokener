package net.kodra.supereasy.traffic.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
**/
public class QueueWorkerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(QueueWorkerServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        LOGGER.log(Level.INFO, request.toString());
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        LOGGER.log(Level.INFO, request.toString());
        Enumeration<String> parameterNameEnumeration = request.getParameterNames();
        while (parameterNameEnumeration.hasMoreElements()) {
            String parameterName = parameterNameEnumeration.nextElement();
            LOGGER.log(Level.INFO, parameterName + " = " + request.getParameter(parameterName));
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private static byte[] fetchImage(String urlString) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            URL url = new URL(urlString);
            InputStream is = url.openStream();
            for (int c = is.read(); c != -1; c = is.read()) baos.write(c);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }
}
