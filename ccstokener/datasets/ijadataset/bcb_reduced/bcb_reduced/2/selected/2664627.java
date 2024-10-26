package edu.petrnalevka.relaxed.servlet;

import edu.petrnalevka.relaxed.JARVSchemaValidator;
import edu.petrnalevka.relaxed.SchematronSaxonTransformer;
import edu.petrnalevka.relaxed.ValidatorCollector;
import edu.petrnalevka.relaxed.error.CollectorErrorHandler;
import edu.petrnalevka.relaxed.error.SeverityLevel;
import edu.petrnalevka.relaxed.mapping.OptionBean;
import edu.petrnalevka.relaxed.servlet.util.RequestConst;
import edu.petrnalevka.relaxed.source.NumberedResultFactory;
import edu.petrnalevka.relaxed.storage.property.FilterProperties;
import edu.petrnalevka.relaxed.util.Configuration;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.oreilly.servlet.MultipartWrapper;

/**
 * This servlet is used for web-based validation.
 */
public class ValidatationServlet extends HttpServlet {

    ValidatorCollector validator;

    String validatorPage;

    String validatorPageXML;

    /**
     * To maximize the performace, the entire validation enviroment is created during servlet initialization.
     */
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        validatorPage = servletConfig.getServletContext().getInitParameter("validator.page");
        validatorPageXML = servletConfig.getServletContext().getInitParameter("validator.page.xml");
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
        if (validator == null) {
            try {
                Set validatorSet = new HashSet();
                validatorSet.add(new JARVSchemaValidator(Configuration.getInstance().getProjectProperty("schema.namespace")));
                validatorSet.add(new SchematronSaxonTransformer(Configuration.getInstance().getSchematronTransformerURL(), Configuration.getInstance().getSchemaExtractorURL()));
                validator = new ValidatorCollector(validatorSet);
            } catch (Exception e) {
                new ServletException(e);
            }
        }
        RequestDispatcher dispatcher = request.getRequestDispatcher(validatorPage);
        RequestDispatcher dispatcherXML = request.getRequestDispatcher(validatorPageXML);
        String option = null;
        option = request.getParameter(RequestConst.OPTION_PARAMETER);
        if (option == null) {
            if (option == null) {
                Collection options = (Collection) request.getAttribute(RequestConst.OPTION_LIST_ATTRIBUTE);
                if (!options.iterator().hasNext()) option = "xhtml"; else option = ((OptionBean) options.iterator().next()).getName();
            }
        }
        InputStream in = null;
        if (request instanceof MultipartWrapper) {
            try {
                File file = ((MultipartWrapper) request).getFile(RequestConst.FILE_PARAMETER);
                if (file != null) {
                    in = new FileInputStream(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
                request.setAttribute(RequestConst.MESSAGE_ATTRIBUTE, "   Unable to handle the uploaded file.");
                dispatcher.forward(request, response);
                return;
            }
        }
        if (in == null) {
            String urlString = request.getParameter(RequestConst.URL_PARAMETER);
            if (RequestConst.REFERER_PARAMETER_VALUE.equals(urlString)) urlString = request.getHeader("referer");
            URL url = null;
            try {
                url = new URL(urlString);
                if (url.getProtocol().toLowerCase().equals("file")) throw new IOException("Protocol 'file' is not allowed.");
            } catch (IOException e) {
                request.setAttribute(RequestConst.MESSAGE_ATTRIBUTE, "Unable to validate '" + urlString + "': " + e.getMessage());
                dispatcher.forward(request, response);
                return;
            }
            try {
                URLConnection connection = url.openConnection();
                connection.setAllowUserInteraction(false);
                in = connection.getInputStream();
            } catch (Exception e) {
                request.setAttribute(RequestConst.MESSAGE_ATTRIBUTE, "   Unable to connect to " + urlString);
                dispatcher.forward(request, response);
                return;
            }
        }
        CollectorErrorHandler errorHandler = new CollectorErrorHandler(SeverityLevel.INFO);
        try {
            FilterProperties filterProperties = new FilterProperties();
            filterProperties.load(request);
            Map source = NumberedResultFactory.create(validator.validate(option, in, filterProperties, errorHandler));
            request.setAttribute(RequestConst.SOURCE_CODE, source);
        } catch (Exception e) {
            e.printStackTrace();
            errorHandler.reportMessage(e.getClass() + " " + e.getMessage(), SeverityLevel.FATAL_ERROR);
        }
        request.setAttribute(RequestConst.RESULT_ATTRIBUTE, errorHandler.getResult());
        if (errorHandler.isValid()) {
            request.setAttribute(RequestConst.MESSAGE_ATTRIBUTE, "Congratulation, your document is valid. Relax.");
        } else {
            request.setAttribute(RequestConst.MESSAGE_ATTRIBUTE, "Your document is invalid.");
        }
        if (request.getParameter(RequestConst.XML_OUTPUT) != null) dispatcherXML.forward(request, response); else dispatcher.forward(request, response);
    }

    public void destroy() {
        super.destroy();
        validator.destroy();
        validator = null;
    }
}
