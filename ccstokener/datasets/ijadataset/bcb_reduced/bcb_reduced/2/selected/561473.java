package edu.petrnalevka.relaxed.servlet;

import edu.petrnalevka.relaxed.ValidatorCollector;
import edu.petrnalevka.relaxed.error.EmptyErrorHandler;
import edu.petrnalevka.relaxed.servlet.util.RequestConst;
import edu.petrnalevka.relaxed.storage.MemoryReuseableDocumentStorage;
import edu.petrnalevka.relaxed.storage.property.FilterProperties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This servlet is used for web-based validation.
 */
public class ConvServlet extends HttpServlet {

    ValidatorCollector validator;

    String validatorPage;

    /**
       * To maximize the performace, the entire validation enviroment is created during servlet initialization.
       */
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
    }

    /**
       * Forwards to doGet() method
       */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
       * Executes the validation and stores results in the request to be shown by a jsp page.
       */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String urlString = request.getParameter(RequestConst.URL_PARAMETER);
        ServletOutputStream out = response.getOutputStream();
        response.setContentType("text/plain");
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (IOException e) {
            out.println("Error: '" + urlString + "' is not a valid URL.");
            return;
        }
        InputStream in = null;
        try {
            URLConnection connection = url.openConnection();
            connection.setAllowUserInteraction(false);
            in = connection.getInputStream();
        } catch (IOException e) {
            out.println("Error: Unable to connect to '" + urlString + "'.");
            return;
        }
        try {
            MemoryReuseableDocumentStorage storage = new MemoryReuseableDocumentStorage(new FilterProperties(), new EmptyErrorHandler());
            storage.store(in);
            BufferedReader reader = new BufferedReader(storage.getReader());
            String line = "";
            int lineNumber = 0;
            while (true) {
                lineNumber++;
                line = reader.readLine();
                if (line == null) break;
                out.println(lineNumber + " \t " + line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("Error: '" + e.getMessage() + "'.");
        }
        return;
    }
}
