package org.dbe.servent;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import org.dbe.servent.http.ServentRequest;
import org.dbe.servent.http.ServentResponse;
import org.dbe.servent.service.ServiceWrapper;
import org.sun.dbe.tools.MultipartFormData;
import sun.misc.BASE64Decoder;

/**
 * 
 * @author bob
 */
public class AdminHandlerImpl extends GenericServentHandler implements AdminHandler {

    public String getName() {
        return "Administration Handler";
    }

    private ServentComponentImpl servent = null;

    public void initialize(ServentContext context) {
        super.initialize(context);
        servent = new ServentComponentImpl(context);
    }

    public void handle(String command, String pathParams, ServentRequest request, ServentResponse response) throws ServentException, IOException {
        logger.debug(" >> Administration Call: " + command);
        String autorization = request.getHeader("Authorization");
        String adminPassword = context.getConfig().getAttribute("adminPassword");
        if (adminPassword == null) {
            adminPassword = "ec0system";
        }
        String userAndPassword = "root:" + adminPassword;
        if (autorization == null) {
            response.setStatus(ServentResponse.SC_UNAUTHORIZED);
            response.addHeader("WWW-Authenticate", "Basic realm=\"DBE administration site\"");
            return;
        }
        BASE64Decoder decoder = new BASE64Decoder();
        String decoded = new String(decoder.decodeBuffer(autorization.substring("Basic ".length())));
        if (!userAndPassword.equals(decoded)) {
            response.setStatus(ServentResponse.SC_UNAUTHORIZED);
            response.addHeader("WWW-Authenticate", "Basic realm=\"DBE administration site\"");
            return;
        }
        try {
            if (REGISTER_COMMAND.equals(command)) {
                doRegister(request, response);
            } else if (UPLOAD_COMMAND.equals(command)) {
                doUpload(request, response);
            } else if (LIST_COMMAND.equals(command)) {
                doList(response);
            } else if (SHUTDOWN_COMMAND.equals(command)) {
                doStop(request, response);
            } else if (command.startsWith(DEREGISTER_COMMAND)) {
                String application = new String(command.substring(DEREGISTER_COMMAND.length()));
                doUnregister(application, response);
            } else {
                logger.debug(" Command " + command + " has been ignored");
            }
        } catch (Throwable e) {
            response.setStatus(ServentResponse.SC_INTERNAL_SERVER_ERROR);
            StringBuffer sb = startSB();
            sb.append("Server Side Exception: " + e.getMessage());
            endSB(sb);
            response.getOutputStream().write((sb.toString()).getBytes());
            logger.error("Error while handling request ", e);
        } finally {
            response.getOutputStream().close();
        }
    }

    /**
     * Registers one application using the deployer class
     * 
     * @param buffer
     *            jar file to register (dbe structure)
     * @param response
     *            response
     * @throws IOException
     * @throws IOException
     *             io exception
     * @throws ServerServentException
     *             any other exception
     * @throws FileNotFoundException
     *             file not found
     */
    private void doRegister(ServentRequest request, ServentResponse response) throws ServiceException, IOException {
        byte[] body = request.getBody();
        if ((body == null) || (body.length == 0)) {
            logger.warn("Request body cannot be null");
            response.sendError("Request body cannot be null");
        }
        String userUri = new String(body);
        logger.info("Deploying application " + userUri);
        URL url = new URL(userUri);
        InputStream is = url.openStream();
        servent.deploy(is);
        is.close();
        StringBuffer sb = startSB();
        sb.append("Service Registered");
        endSB(sb);
        response.getOutputStream().write(sb.toString().getBytes());
    }

    /**
     * Upload a file and deploy it
     * 
     * @param request
     * @param response
     * @throws ServiceException
     * @throws IOException
     */
    private void doUpload(ServentRequest request, ServentResponse response) throws ServiceException, IOException {
        String multipartHeader = request.getContentType();
        if ((multipartHeader == null) || (!multipartHeader.startsWith("multipart/form-data"))) {
            logger.error("Content-type was not multipart/form-data");
            response.setStatus(ServentResponse.SC_BAD_REQUEST);
            response.getOutputStream().write("Client Side Exception: Content-type was not multipart/form-data".getBytes());
        }
        byte[] boundary = MultipartFormData.getBoundary(multipartHeader);
        upload(request.getBody(), boundary, response);
    }

    /**
     * Registers one application using the deployer class
     * 
     * @param buffer
     *            jar file to register (dbe structure)
     * @param boundary
     *            separation in a POST http request
     * @param response
     *            response
     * @throws org.dbe.servent.ServiceException
     * @throws IOException
     * @throws IOException
     *             io exception
     * @throws ServerServentException
     *             any other exception
     * @throws FileNotFoundException
     *             file not found
     */
    private void upload(byte[] buffer, byte[] boundary, ServentResponse response) throws ServiceException, IOException {
        MultipartFormData mfd = new MultipartFormData(new ByteArrayInputStream(buffer), boundary);
        Collection parts = mfd.getParts();
        InputStream is = null;
        for (Iterator iter = parts.iterator(); iter.hasNext(); ) {
            MultipartFormData.Part part = (MultipartFormData.Part) iter.next();
            is = new ByteArrayInputStream(part.getContents());
            break;
        }
        servent.deploy(is);
        is.close();
        StringBuffer sb = startSB();
        sb.append("Service Registered");
        endSB(sb);
        response.getOutputStream().write(sb.toString().getBytes());
    }

    /**
     * Stop the servent
     * 
     * @param request
     * @param response
     * @throws ServiceException
     * @throws IOException
     */
    private void doStop(ServentRequest request, ServentResponse response) throws ServiceException, IOException {
        StringBuffer sb = startSB();
        sb.append("Servent Stoped!");
        endSB(sb);
        response.getOutputStream().write(sb.toString().getBytes());
        System.exit(0);
    }

    /**
     * Unregister a service
     * 
     * @param application
     *            endpoint
     * @param response
     *            response
     * @throws IOException
     */
    private void doUnregister(String application, ServentResponse response) throws IOException {
        try {
            context.getComponentManager().getDeployer().undeploy(application);
        } catch (ServerServentException e) {
            logger.warn("Cannot unregistered the aplication " + application);
            response.sendError("Cannot unregistered the aplication " + application);
        }
        try {
            response.getOutputStream().write("Service Unregistered".getBytes());
        } catch (IOException e) {
            logger.error("Errow while writing response to the client. Service " + application + " was Unregistered");
        }
    }

    /**
     * Get a list of the services registered
     * 
     * @param response
     *            response
     * @throws IOException
     *             communication exception
     */
    private void doList(ServentResponse response) throws IOException {
        StringBuffer sb = startSB();
        sb.append("<b>Services</b><hr />");
        for (Iterator it = servent.getServices().iterator(); it.hasNext(); ) {
            ServiceWrapper wrapper = (ServiceWrapper) it.next();
            sb.append("<a href=\"");
            sb.append(context.getConfig().getPublicURL() + "/" + wrapper.getId());
            sb.append("\">");
            sb.append(wrapper.getName());
            sb.append("</a>");
            sb.append("<br />");
        }
        sb.append("<br /><br /><br /><b>Filters</b><hr />");
        for (Iterator it = servent.getFilters().iterator(); it.hasNext(); ) {
            ServiceWrapper wrapper = (ServiceWrapper) it.next();
            sb.append(wrapper.getName());
            sb.append("<br />");
        }
        endSB(sb);
        response.getOutputStream().write(sb.toString().getBytes());
        response.getOutputStream().flush();
    }

    private StringBuffer startSB() {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>");
        sb.append("<head><title>ServENT administrationt</title></head>");
        sb.append("<body>");
        return sb;
    }

    private void endSB(StringBuffer sb) {
        sb.append("</body>");
        sb.append("</html>");
    }
}
