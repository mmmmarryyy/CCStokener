package net.sf.chellow.monad;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.UriPathElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class Monad extends HttpServlet implements Urlable {

    private static String contextPath;

    private static Urlable urlableRoot;

    public static String getContextPath() {
        return contextPath;
    }

    public static Urlable getUrlableRoot() {
        return urlableRoot;
    }

    private static final MonadUri URI;

    static {
        try {
            URI = new MonadUri("/");
        } catch (UserException e) {
            throw new RuntimeException(e);
        } catch (ProgrammerException e) {
            throw new RuntimeException(e);
        }
    }

    private String realmName;

    private static final String CONFIG_PREFIX = "/WEB-INF/default-config";

    private static final String DEBUG_FILE_NAME = "debug";

    protected Class<Invocation> invocationClass;

    private static ServletContext context;

    private static File CONFIG_DIR = null;

    private String templateDirName;

    protected static Logger logger = Logger.getLogger("uk.org.tlocke.theelected");

    public static ServletContext getContext() {
        return context;
    }

    public static void setContext(ServletContext context) {
        Monad.context = context;
    }

    public static File getConfigDir() {
        return CONFIG_DIR;
    }

    public static String getConfigPrefix() {
        return CONFIG_PREFIX;
    }

    public Monad() {
        urlableRoot = this;
        invocationClass = getInvocationClass();
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new MonadFormatter());
        logger.addHandler(consoleHandler);
    }

    protected Class<Invocation> getInvocationClass() {
        return Invocation.class;
    }

    public String getRealmName() {
        return realmName;
    }

    protected void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    protected void setTemplateDirName(String templateDirName) {
        this.templateDirName = templateDirName;
    }

    public String getTemplateDirName() {
        return templateDirName;
    }

    protected void setHibernateUtil(Hiber hibernateUtil) {
    }

    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
        context = conf.getServletContext();
    }

    protected abstract void checkPermissions(Invocation inv) throws ProgrammerException, UserException;

    @SuppressWarnings("unchecked")
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        contextPath = req.getContextPath();
        Invocation inv = null;
        try {
            try {
                Class[] classArray = new Class[] { HttpServletRequest.class, HttpServletResponse.class, Monad.class };
                inv = (Invocation) invocationClass.getConstructor(classArray).newInstance(new Object[] { req, res, this });
                String pathInfo = req.getPathInfo();
                if (pathInfo != null && !pathInfo.endsWith("/")) {
                    try {
                        throw UserException.newMovedPermanently(new URI(contextPath + req.getPathInfo() + "/"));
                    } catch (URISyntaxException e) {
                        throw UserException.newBadRequest(e.getMessage());
                    }
                }
                checkPermissions(inv);
                Urlable urlable = inv.dereferenceUrl();
                if (urlable == null) {
                    inv.returnStatic(getServletConfig().getServletContext(), pathInfo);
                } else {
                    String method = req.getMethod();
                    if (method.equals("GET")) {
                        urlable.httpGet(inv);
                    } else if (method.equals("POST")) {
                        urlable.httpPost(inv);
                    } else if (method.equals("DELETE")) {
                        urlable.httpDelete(inv);
                    }
                }
            } catch (UserException e) {
                Document doc = e.getDocument();
                Element sourceElement = doc.getDocumentElement();
                VFMessage message = e.getVFMessage();
                switch(e.getStatusCode()) {
                    case HttpServletResponse.SC_FORBIDDEN:
                        inv.sendForbidden();
                        break;
                    case HttpServletResponse.SC_MOVED_PERMANENTLY:
                        inv.sendMovedPermanently(e.getLocationHeader());
                        break;
                    case HttpServletResponse.SC_NOT_FOUND:
                        inv.sendNotFound();
                        break;
                    case HttpServletResponse.SC_OK:
                        if (message != null) {
                            sourceElement.appendChild(e.getVFMessage().toXML(doc));
                        }
                        inv.sendOk(doc);
                        break;
                    case HttpServletResponse.SC_UNAUTHORIZED:
                        inv.sendUnauthorized();
                        break;
                    case 418:
                        if (message != null) {
                            sourceElement.appendChild(message.toXML(doc));
                        }
                        inv.sendInvalidParameter(doc);
                        break;
                    case HttpServletResponse.SC_BAD_REQUEST:
                        inv.sendBadRequest(e.getMessage());
                        break;
                }
            }
        } catch (Throwable e) {
            try {
                new InternetAddress("tlocke@tlocke.org.uk");
            } catch (AddressException ae) {
            }
            logger.logp(Level.SEVERE, "uk.org.tlocke.monad.Monad", "service", "Can't process request", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, (e instanceof UserException) ? e.getMessage() : "There has been an error with our software. The " + "administrator has been informed, and the problem will " + "be put right as soon as possible.");
        } finally {
            Hiber.rollBack();
            Hiber.close();
        }
    }

    public static void setConfigDir(File configDir) {
        Monad.CONFIG_DIR = configDir;
    }

    public static URL getConfigResource(MonadUri uri) throws ProgrammerException, UserException {
        URL url = getConfigFile(uri);
        if (url == null) {
            url = getConfigUrl(uri);
        }
        return url;
    }

    public static URL getConfigFile(MonadUri uri) throws ProgrammerException, UserException {
        URL url = null;
        try {
            MonadUri uriNew = getConfigFile(new MonadUri("/"), uri.toString().substring(1).split("/"), 0);
            if (uriNew != null) {
                url = new File(CONFIG_DIR.toString() + File.separator + uriNew.toString()).toURI().toURL();
            }
            return url;
        } catch (MalformedURLException e) {
            throw new ProgrammerException(e);
        }
    }

    public static URL getConfigUrl(MonadUri uri) throws ProgrammerException, UserException {
        URL url = null;
        try {
            MonadUri newUri = getConfigUrl(new MonadUri("/"), uri.toString().substring(1).split("/"), 0);
            if (newUri != null) {
                url = Monad.getContext().getResource(CONFIG_PREFIX + newUri.toString());
            }
        } catch (MalformedURLException e) {
            throw new ProgrammerException(e);
        }
        return url;
    }

    public static MonadUri getConfigUrl(MonadUri uri, String[] elements, int position) throws ProgrammerException, UserException {
        List<String> urlElements = getConfigUrlElements(uri);
        MonadUri newUri = null;
        if (urlElements.contains(elements[position] + (position == elements.length - 1 ? "" : "/"))) {
            newUri = uri.resolve(elements[position]);
            if (position < elements.length - 1) {
                newUri = newUri.append("/");
                newUri = getConfigUrl(newUri, elements, position + 1);
            }
        }
        if (newUri == null && urlElements.contains("default" + (position == elements.length - 1 ? "" : "/"))) {
            if (position < elements.length - 1) {
                newUri = uri.append("default/");
                newUri = getConfigUrl(newUri, elements, position + 1);
            }
        }
        return newUri;
    }

    public static MonadUri getConfigFile(MonadUri uri, String[] elements, int position) throws ProgrammerException, UserException {
        List<String> fileElements = getConfigFileElements(uri);
        MonadUri newUri = null;
        if (fileElements.contains(elements[position])) {
            newUri = uri.resolve(elements[position]);
            if (position < elements.length - 1) {
                newUri = newUri.append("/");
                newUri = getConfigFile(newUri, elements, position + 1);
            }
        }
        if (newUri == null && fileElements.contains("default")) {
            newUri = uri.append("default/");
            if (position < elements.length - 1) {
                newUri = getConfigFile(newUri, elements, position + 1);
            }
        }
        return newUri;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getConfigFileElements(MonadUri uri) throws ProgrammerException, UserException {
        List<String> urlElements = new ArrayList<String>();
        if (Monad.getConfigDir() != null) {
            File urlsPath = new File(Monad.getConfigDir().toString() + uri.toString().replace("/", File.separator));
            String[] files = urlsPath.list();
            if (files != null) {
                for (String file : files) {
                    urlElements.add(file);
                }
            }
        }
        return urlElements;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getConfigUrlElements(MonadUri uri) {
        List<String> urlElements = new ArrayList<String>();
        String resourcePath = Monad.getConfigPrefix() + uri.toString();
        Set<String> paths = Monad.getContext().getResourcePaths(resourcePath);
        if (paths != null) {
            for (String path : paths) {
                if (!urlElements.contains(path)) {
                    urlElements.add(path.substring(resourcePath.length()));
                }
            }
        }
        return urlElements;
    }

    public static InputStream getConfigIs(String path, String name) throws ProgrammerException, DesignerException, UserException {
        InputStream is = null;
        try {
            URL url = getConfigResource(new MonadUri(path).append(name));
            if (url != null) {
                is = url.openStream();
            }
        } catch (IOException e) {
            throw new ProgrammerException(e);
        }
        return is;
    }

    @SuppressWarnings("unchecked")
    public static void returnStream(Document doc, String templatePath, String templateName, Result result) throws DesignerException, ProgrammerException, DeployerException, UserException {
        TransformerFactory tf = TransformerFactory.newInstance();
        InputStream templateIs = null;
        InputStream debugIs = null;
        Transformer transformer;
        tf.setURIResolver(new ElectedURIResolver());
        if (templatePath != null && templateName != null) {
            templateIs = getConfigIs(templatePath, templateName);
            if (templateIs == null) {
                throw new DesignerException("The resource '" + templatePath + " : " + templateName + "' is needed but does not exist.");
            }
            debugIs = getConfigIs(templatePath, DEBUG_FILE_NAME);
        }
        try {
            if (debugIs != null) {
                StringWriter sr = new StringWriter();
                transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new DOMSource(doc), new StreamResult(sr));
                logger.logp(Level.INFO, "uk.org.tlocke.monad.Monad", "returnStream", sr.toString());
            }
            transformer = templateIs == null ? tf.newTransformer() : tf.newTransformer(new StreamSource(templateIs));
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), result);
        } catch (TransformerConfigurationException e) {
            Throwable throwable = e.getCause();
            throw UserException.newInvalidParameter("Problem transforming template '" + templatePath + " : " + templateName + " " + e.getMessageAndLocation() + e.getMessage() + e.getLocationAsString() + " " + e.getLocator() + (throwable == null ? "" : " Problem type : " + throwable.getClass().getName() + " Message: " + throwable.getMessage()));
        } catch (TransformerException e) {
            throw UserException.newInvalidParameter("Problem transforming template '" + templatePath + " : " + templateName + "'. " + e.getMessageAndLocation() + " " + " Problem type : " + e.getCause().getClass().getName() + " Message: " + e.getException().getMessage() + "Stack trace: " + MonadUtils.getStackTrace(e.getException()) + e);
        }
    }

    private static class ElectedURIResolver implements URIResolver {

        public Source resolve(String href, String base) {
            Source source = null;
            try {
                source = new StreamSource(getConfigIs(base, href));
            } catch (ProgrammerException e) {
                throw new RuntimeException(e);
            } catch (DesignerException e) {
                throw new RuntimeException(e);
            } catch (UserException e) {
                throw new RuntimeException(e);
            }
            return source;
        }
    }

    public static Urlable dereferenceUri(URI uri) throws UserException, ProgrammerException {
        Urlable urlable = urlableRoot;
        String pathInfo = uri.getPath();
        if (pathInfo.length() > 1) {
            pathInfo = pathInfo.substring(1);
        }
        for (String element : pathInfo.split("/")) {
            urlable = urlable.getChild(new UriPathElement(element));
            if (urlable == null) {
                break;
            }
        }
        return urlable;
    }

    public MonadUri getUri() throws ProgrammerException {
        return URI;
    }
}
