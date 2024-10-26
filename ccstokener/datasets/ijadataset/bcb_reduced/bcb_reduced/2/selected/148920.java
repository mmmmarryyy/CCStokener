package net.sourceforge.ecm.validator;

import fedora.common.Constants;
import fedora.server.Server;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ServerInitializationException;
import fedora.server.management.ManagementModule;
import fedora.server.proxy.AbstractInvocationHandler;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Arrays;

/**
 * This invocationhandler is simple. If hooks the modifyObject method. When the state is set to Active, the validator
 * is invoked. A server exception with the problems is thrown, and the state is not changed.
 */
public class FedoraModifyObjectHook extends AbstractInvocationHandler {

    /** Logger for this class. */
    private static Logger LOG = Logger.getLogger(FedoraModifyObjectHook.class.getName());

    public static final String HOOKEDMETHOD = "modifyObject";

    private boolean initialised = false;

    private String username;

    private String password;

    private String webservicelocation;

    public void init() {
        if (initialised) {
            return;
        }
        Server s_server;
        try {
            s_server = Server.getInstance(new File(Constants.FEDORA_HOME), false);
        } catch (ServerInitializationException e) {
            LOG.error("Unable to get access to the server instance, the " + "Validator will not be started.", e);
            return;
        } catch (ModuleInitializationException e) {
            LOG.error("Unable to get access to the server instance, the " + "Validator will not be started.", e);
            return;
        }
        ManagementModule m_manager = (ManagementModule) s_server.getModule("fedora.server.management.Management");
        if (m_manager == null) {
            LOG.error("Unable to get Management module from server, the " + "Validator will not start up.");
            return;
        }
        webservicelocation = m_manager.getParameter("validator.webservice.location");
        if (webservicelocation == null) {
            webservicelocation = "http://localhost:8080/ecm/validate/";
            LOG.info("No validator.webservice.location specified, using default location: " + webservicelocation);
        }
        username = m_manager.getParameter("validator.webservice.fedora.username");
        if (username == null) {
            username = "fedoraAdmin";
            LOG.info("No validator.webservice.fedora.username specified, using default username: " + username);
        }
        password = m_manager.getParameter("validator.webservice.fedora.password");
        if (password == null) {
            password = "fedoraAdminPass";
            LOG.info("No validator.webservice.fedora.password specified, using default password: " + password);
        }
        initialised = true;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable, IOException {
        LOG.debug("Entering method invoke in FedoraModifyObjectHook with arguments: method='" + method.getName() + "' and arguments: " + Arrays.toString(args));
        if (!initialised) {
            init();
        }
        if (!HOOKEDMETHOD.equals(method.getName())) {
            return callMethod(method, args);
        }
        LOG.info("We are hooking method " + method.getName());
        String state = (String) args[2];
        if (!(state != null && state.startsWith("A"))) {
            return callMethod(method, args);
        }
        String pid = args[1].toString();
        LOG.info("The method was called with the pid " + pid);
        Document result = validate(pid);
        Element docelement = result.getDocumentElement();
        if (isValid(docelement)) {
            return callMethod(method, args);
        } else {
            String problems = reportProblems(docelement);
            throw new ValidationFailedException(null, problems, null, null, null);
        }
    }

    private String reportProblems(Element docelement) throws ValidationFailedException {
        String report = "<validationErrors>\n";
        NodeList problems = null;
        try {
            problems = xpathQuery(docelement, "/validation/problems/problem");
        } catch (XPathExpressionException e) {
            LOG.warn("the validator crashed", e);
            throw new ValidationFailedException("exceptions", "The.validator.crashed", null, null, null);
        }
        for (int i = 0; i < problems.getLength(); i++) {
            String error = problems.item(i).getFirstChild().getNodeValue();
            report += "<error>" + error + "</error>\n";
        }
        report += "</validationErrors>";
        return report;
    }

    private boolean isValid(Element docelement) throws ValidationFailedException {
        NodeList validnodes = null;
        try {
            validnodes = xpathQuery(docelement, "/validation/@valid");
        } catch (XPathExpressionException e) {
            LOG.warn("the validator crashed", e);
            throw new ValidationFailedException("exceptions", "The.validator.crashed", null, null, null);
        }
        if (validnodes.getLength() == 1) {
            String value = validnodes.item(0).getNodeValue();
            return "true".equalsIgnoreCase(value);
        } else {
            LOG.warn("the validator crashed");
            throw new ValidationFailedException("exceptions", "The.validator.crashed", null, null, null);
        }
    }

    private Document validate(String pid) throws IOException, ValidationFailedException {
        final String login = username;
        final String password = this.password;
        Authenticator.setDefault(new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(login, password.toCharArray());
            }
        });
        URL validatorurl = new URL(webservicelocation + pid);
        InputStream reply = null;
        reply = validatorurl.openStream();
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = fac.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOG.warn("the validator crashed", e);
            throw new ValidationFailedException("exceptions", "The.validator.crashed", null, null, null);
        }
        Document result = null;
        try {
            result = builder.parse(reply);
        } catch (SAXException e) {
            LOG.warn("the validator crashed", e);
            throw new ValidationFailedException("exceptions", "The.validator.crashed", null, null, null);
        }
        return result;
    }

    /**
     * Helper method for doing an XPath query using DOMS namespaces.
     * {@see NameSpaceConstants#DOMS_NAMESPACE_CONTEXT}.
     *
     * @param node            The node to start XPath query on.
     * @param xpathExpression The XPath expression, using default DOMS
     *                        namespace prefixes.
     * @return The result, as a node list.
     *
     * @throws javax.xml.xpath.XPathExpressionException On trouble parsing or evaluating the
     *                                  expression.
     */
    public NodeList xpathQuery(Node node, String xpathExpression) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (NodeList) xPath.evaluate(xpathExpression, node, XPathConstants.NODESET);
    }

    private Object callMethod(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable thr = e.getTargetException();
            LOG.debug("Rethrowing this exception, and loosing the stacktrace", thr);
            throw thr;
        }
    }
}
