package com.hlcl.rql.as;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Die Klasse beschreibt einen Client zum RedDot Content Management Server.
 * 
 * @author LEJAFR
 */
public class CmsClient {

    private static final String LDAP_SERVER_NAME_KEY = "LdapServerName";

    private static final String LDAP_SERVER_PORT_KEY = "LdapServerPort";

    private static final String MAIL_SMTP_CONNECTIONTIMEOUT = "mail.smtp.connectiontimeout";

    private static final String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";

    private static final String STATISTIC_MAIL_FROM_ADDRESS = "lejafr@hlag.com";

    /**
	 * BURMEBJ002A Diese Methode verschickt eine E-Mail mit Attachements an mehrere Empfï¿½nger. Ein groï¿½er Teil dieser Methode wurde
	 * von der ursprï¿½nglichen Methode sendMail(String, String[], String, String, File[]) ï¿½bernommen. Lediglich der letzte Parameter
	 * fileNames wurde ergï¿½nzt.
	 * 
	 * Geï¿½ndert durch: Name Datum: MM-JJ Beschreibung: kurze Erlï¿½uterung der ï¿½nderung
	 * 
	 * @param from
	 *            ein String mit der E-Mail-Adresse des Absenders
	 * @param to
	 *            ein Array von Strings mit den E-Mail-Adressen der Empfï¿½nger
	 * @param subject
	 *            ein String mit dem Subject der E-Mail
	 * @param msgText
	 *            ein String mit dem Text der E-Mail
	 * @param attachements
	 *            ein Array von Files, die attached werden sollen
	 * @param fileNames
	 *            Ein Array mit den fileNames, die fï¿½r die Attachments in der Mail vergeben werden sollen. Dabei wird
	 *            folgendermaï¿½en vorgegangen: Fï¿½r das i. Attachement wird geprï¿½ft, ob der i. Eintrag des fileNames-Array von null
	 *            verschieden ist. Wenn ja, wird dieser fileName verwendet, ansonsten der ursprï¿½ngliche File-Name des Attachements.
	 *            (BURMEBJ002A)
	 * @exception javax.mail.MessagingException,
	 *                wenn beim Versenden der E-Mail ein Fehler auftritt
	 */
    public static void sendMail(String from, String[] to, String subject, String msgText, java.io.File[] attachements, String[] fileNames) throws MessagingException {
        String smtpHost = "";
        PropertyResourceBundle bundle = (PropertyResourceBundle) PropertyResourceBundle.getBundle("com.hlcl.rql.as.SMTPHost");
        smtpHost = bundle.getString("smtpHost").trim();
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        setOptionalProperty(MAIL_SMTP_TIMEOUT, bundle, props);
        setOptionalProperty(MAIL_SMTP_CONNECTIONTIMEOUT, bundle, props);
        Session session = Session.getDefaultInstance(props, null);
        Message msg = new MimeMessage(session);
        InternetAddress fromAddress = new InternetAddress(from);
        msg.setFrom(fromAddress);
        int max = to.length;
        InternetAddress[] toAddress = new InternetAddress[max];
        for (int i = 0; i < max; i++) {
            toAddress[i] = new InternetAddress(to[i]);
        }
        msg.setRecipients(Message.RecipientType.TO, toAddress);
        msg.setSubject(subject);
        if (attachements == null) {
            msg.setContent(msgText, "text/plain; charset=ISO-8859-1");
        } else {
            MimeMultipart mp = new MimeMultipart();
            MimeBodyPart text = new MimeBodyPart();
            text.setDisposition(Part.INLINE);
            text.setContent(msgText, "text/plain; charset=ISO-8859-1");
            mp.addBodyPart(text);
            int numberOfFileNames = 0;
            if (fileNames != null) {
                numberOfFileNames = fileNames.length;
            }
            for (int i = 0; i < attachements.length; i++) {
                MimeBodyPart file_part = new MimeBodyPart();
                java.io.File file = attachements[i];
                FileDataSource fds = new FileDataSource(file);
                DataHandler dh = new DataHandler(fds);
                String fileName = file.getName();
                if (i < numberOfFileNames && fileNames[i] != null) {
                    fileName = fileNames[i];
                }
                file_part.setFileName(fileName);
                file_part.setDisposition(Part.ATTACHMENT);
                file_part.setDescription("Attached file: " + file.getName());
                file_part.setDataHandler(dh);
                mp.addBodyPart(file_part);
            }
            msg.setContent(mp);
        }
        Transport.send(msg);
    }

    private static void setOptionalProperty(String key, ResourceBundle bundle, Properties props) {
        try {
            String value = bundle.getString(key);
            if (value != null) {
                value = value.trim();
                if (value.length() > 0) {
                    props.put(key, value);
                }
            }
        } catch (MissingResourceException mre) {
        }
    }

    private RQLNodeList allLocalesNodeListCache;

    private RQLNodeList allPluginsNodeListCache;

    private RQLNodeList allUserInterfaceLanguagesNodeListCache;

    private RQLNodeList allUsersNodeListCache;

    private User connectedUser;

    private Project currentProject;

    private DirContext ldapContext;

    private String logonGuid;

    private RQLNodeList projectsNodeListCache;

    private URL cmsServerConnectionUrl;

    /**
	 * Erzeugt einen CmsServer, indem ein neuer User am CMS angemeldet wird.
	 * 
	 * @param passwordAuthentication
	 * @throws UserAlreadyLoggedInException
	 */
    public CmsClient(PasswordAuthentication passwordAuthentication) throws RQLException {
        super();
        cmsServerConnectionUrl = null;
        login(passwordAuthentication);
    }

    /**
	 * Erzeugt einen CmsServer, indem ein neuer User am CMS angemeldet wird.
	 * 
	 * @param passwordAuthentication
	 * @throws UserAlreadyLoggedInException
	 */
    public CmsClient(PasswordAuthentication passwordAuthentication, URL cmsServerConnectionUrl) throws RQLException {
        super();
        this.cmsServerConnectionUrl = cmsServerConnectionUrl;
        login(passwordAuthentication);
    }

    /**
	 * Erzeugt einen CmsClient fï¿½r die gegebene logonGuid.
	 * 
	 * @param logonGuid
	 *            Anmelde-GUID des angemeldeten Nutzers
	 */
    public CmsClient(String logonGuid) throws RQLException {
        super();
        this.logonGuid = logonGuid;
        cmsServerConnectionUrl = null;
        connectedUser = null;
    }

    /**
	 * Erzeugt einen CmsClient fï¿½r die gegebene logonGuid. Gleichzeitig wird der angemeldete Benutzer initialisiert.
	 * 
	 * @param logonGuid
	 *            Anmelde-GUID des angemeldeten Nutzers
	 * @param connectedUserGuid
	 *            GUID des angemeldeten Nutzers
	 */
    public CmsClient(String logonGuid, String connectedUserGuid) throws RQLException {
        super();
        this.logonGuid = logonGuid;
        cmsServerConnectionUrl = null;
        connectedUser = new User(this, connectedUserGuid);
    }

    /**
	 * Erzeugt einen CmsClient fï¿½r die gegebene logonGuid und die gegebene URL zur CMS Server hlclRemoteRQL.asp.
	 * 
	 * @param logonGuid
	 *            Anmelde-GUID des angemeldeten Nutzers
	 */
    public CmsClient(String logonGuid, URL cmsServerConnectionUrl) throws RQLException {
        super();
        this.logonGuid = logonGuid;
        this.cmsServerConnectionUrl = cmsServerConnectionUrl;
        connectedUser = null;
    }

    /**
	 * Diese Methode liefert den ï¿½bergebenen Node mit all seinen Nachkommen in einen RQLNode mit den entsprechenden Nachkommen um.
	 * Der RQL-Node, der dem ï¿½bergebenen Node entspricht, wird zurï¿½ckgegeben.
	 */
    private RQLNode buildTree(Node root) {
        if (root == null) return null;
        RQLNode rqlRoot = null;
        if (root.getNodeType() == Node.ELEMENT_NODE) {
            rqlRoot = new RQLTagNode(root.getNodeName());
            NamedNodeMap nnm = root.getAttributes();
            for (int i = 0; i < (nnm != null ? nnm.getLength() : 0); i++) {
                Node attr = nnm.item(i);
                if (attr.getNodeType() == Node.ATTRIBUTE_NODE) {
                    ((RQLTagNode) rqlRoot).addAttribute(attr.getNodeName(), attr.getNodeValue());
                }
            }
            NodeList children = root.getChildNodes();
            for (int i = 0; i < (children != null ? children.getLength() : 0); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE || child.getNodeType() == Node.TEXT_NODE) {
                    RQLNode rqlChild = buildTree(child);
                    ((RQLTagNode) rqlRoot).addChild(rqlChild);
                }
            }
        } else if (root.getNodeType() == Node.TEXT_NODE) {
            rqlRoot = new RQLTextNode(root.getNodeValue());
        }
        return rqlRoot;
    }

    /**
	 * Erzeugt ein Userobjekt aus dem gegebenen Usernode.
	 * 
	 * @param node
	 *            node des tags <user ...
	 */
    User buildUser(RQLNode node) {
        User user = null;
        String loginGuidOrNull = node.getAttribute("loginguid");
        if (loginGuidOrNull == null) {
            user = new User(this, node.getAttribute("name"), node.getAttribute("guid"), node.getAttribute("id"), node.getAttribute("fullname"), node.getAttribute("email"));
        } else {
            user = new User(this, node.getAttribute("name"), node.getAttribute("guid"), node.getAttribute("id"), node.getAttribute("fullname"), node.getAttribute("email"), loginGuidOrNull);
        }
        return user;
    }

    /**
	 * Wandelt den RQLNode fï¿½r eine user interface language in ein Object.
	 */
    private UserInterfaceLanguage buildUserInterfaceLanguage(RQLNode languageNode) {
        return new UserInterfaceLanguage(this, languageNode.getAttribute("rfclanguageid"), languageNode.getAttribute("id"), languageNode.getAttribute("country"), languageNode.getAttribute("language"));
    }

    /**
	 * Sendet einen RQL request an das CMS und gibt die geparste Antwort zurï¿½ck.
	 * <p>
	 * Leitet den Aufruf an den RQLHelper weiter.
	 * 
	 * @param rqlRequest
	 *            String
	 * @return RQLNode
	 * @throws RQLException
	 * @see RQLHelper RQLNode
	 */
    public RQLNode callCms(String rqlRequest) throws RQLException {
        return callCmsPrimitive(rqlRequest);
    }

    /**
	 * Diese Methode fï¿½hr eine RQL-Anfrage mit der ï¿½bergebenen rqlQuery an das CMS aus. Das Ergebnis wird in Form eines RQLNode ("<IODATA>...</IODATA>")
	 * zurï¿½ckgegeben. Wenn es zu Problemen kommt, wird eine RQLException geworfen.
	 * 
	 * @param rqlQuery
	 *            String: s.o.
	 * @return RQLNode: s.o.
	 * @throws RQLException:
	 *             s.o.
	 */
    private RQLNode callCmsPrimitive(String rqlQuery) throws RQLException {
        InputStream is = null;
        RQLNode root = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringElementContentWhitespace(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            is = getCMSResultAsStream(rqlQuery);
            InputSource source = new InputSource(is);
            source.setEncoding(getResponseReaderEncoding());
            root = buildTree(db.parse(source).getDocumentElement());
        } catch (ParserConfigurationException pce) {
            throw new RQLException("RQLHelper.callCMS()", pce);
        } catch (SAXException se) {
            throw new RQLException("RQLHelper.callCMS()", se);
        } catch (IOException ioe) {
            throw new RQLException("RQLHelper.callCMS()", ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                }
            }
        }
        if ("ERROR".equals(root.getName())) {
            throw new RQLException(root.getText(), null);
        }
        return root;
    }

    /**
	 * Sendet einen RQL request an das CMS und gibt die ungeparste Antwort zurï¿½ck. Leitet den Aufruf an den RQLHelper weiter.
	 * 
	 * @param rqlRequest
	 *            String
	 * @return XML String
	 * @throws RQLException
	 * @see RQLHelper RQLNode
	 */
    public String callCmsWithoutParsing(String rqlRequest) throws RQLException {
        return callCmsWithoutParsingPrimitive(rqlRequest);
    }

    /**
	 * Diese Methode fï¿½hr eine RQL-Anfrage mit der ï¿½bergebenen rqlQuery an das CMS aus. Das Ergebnis wird als String
	 * zurï¿½ckgegeben. Es wird davon ausgegangen, daï¿½ die 1. Zeile von der Form <?xml ... ?> ist. In diesem Fall wird diese erste
	 * Zeile weggelassen. Andernfalls kommt es zu einer Exception.
	 * 
	 * @param rqlQuery
	 *            String: s.o.
	 * @return String: s.o.
	 * @throws RQLException:
	 *             s.o.
	 */
    private String callCmsWithoutParsingPrimitive(String rqlQuery) throws RQLException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getCMSResultAsStream(rqlQuery), getResponseReaderEncoding()));
            StringBuffer firstLine = new StringBuffer();
            StringBuffer content = new StringBuffer();
            byte state = 0;
            int next = -1;
            while ((next = br.read()) != -1) {
                if (state == 0) {
                    if (next == '\n' || next == '\r') state = 1; else firstLine.append((char) next);
                } else if (state == 1) {
                    if (next != '\n' && next != '\r') state = 2;
                }
                if (state == 2) {
                    content.append((char) next);
                }
            }
            String firstLineString = firstLine.toString();
            if (!(firstLineString.startsWith("<?xml") && firstLineString.endsWith("?>"))) throw new RQLException("RQLHelper.callCMSWithoutParsing(): first line not valid: " + firstLineString);
            return content.toString();
        } catch (IOException ioe) {
            throw new RQLException("RQLHelper.callCMSWithoutParsing()", ioe);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
	 * Wechselt das aktuelle Projekt.
	 * 
	 * @param projectGuid
	 *            Guid des neuen Projektes
	 * @return Project
	 */
    public Project changeCurrentProjectByGuid(String projectGuid) throws RQLException {
        return getProjectByGuid(projectGuid);
    }

    /**
	 * Wechselt das aktuelle Projekt.
	 * 
	 * @param projectName
	 *            Name des neuen Projektes, z.b. hip.hlcl.com
	 * @return Project
	 */
    public Project changeCurrentProjectByName(String projectName) throws RQLException {
        return getProjectByName(projectName);
    }

    /**
	 * Schlieï¿½t den angeforderten LDAP directory service context wieder.
	 * 
	 * @see #openLdapContext()
	 */
    public void closeLdapContext() throws RQLException {
        try {
            ldapContext.close();
        } catch (NamingException ex) {
            throw new RQLException("Could not close LDAP context", ex);
        }
    }

    /**
	 * Liefert alle Mailadressen aller User zurï¿½ck, die fï¿½r die gegebenen Projekte zugelassen sind.
	 * <p>
	 * Diese Abfrage kann nur ein Administrator ausfï¿½hren. Wiederholungen werden durch ein Set vermieden. User ohne Mailadresse
	 * werden ausgelassen.
	 * 
	 * @param sessionKey
	 *            aktueller Session Key
	 * @param projectGuids
	 *            Strings mit den GUIDs der Projekte
	 */
    public java.util.List<String> collectUserMailAddressesForProjects(String sessionKey, String[] projectGuids) throws RQLException {
        Project oldProject = currentProject;
        SortedSet<String> addresses = new TreeSet<String>();
        Iterator<User> iter = null;
        for (int i = 0; i < projectGuids.length; i++) {
            String projectGuid = projectGuids[i];
            Project project = getProject(sessionKey, projectGuid);
            java.util.List<User> projectUsers = project.getAllUsers();
            iter = projectUsers.iterator();
            while (iter.hasNext()) {
                User user = (User) iter.next();
                String address = user.getEmailAddress().trim();
                if (address.length() != 0) {
                    addresses.add(address.toLowerCase());
                }
            }
        }
        currentProject = oldProject;
        return new ArrayList<String>(addresses);
    }

    /**
	 * Meldet diesen Client vom CMS ab. Danach kann dieses Object nicht mehr benutzt werden.
	 */
    public void disconnect() throws RQLException {
        logout(getLogonGuid());
        logonGuid = null;
        projectsNodeListCache = null;
        allUsersNodeListCache = null;
        connectedUser = null;
    }

    /**
	 * Sperrt alle Projekte dieses Servers und meldet alle aktiven Benutzer (auï¿½er dem, der das Script gestartet hat) ab.
	 */
    public void enterOutage(String outageMessage, boolean isTest) throws RQLException {
        java.util.List<Project> projects = isTest ? getTestProjects() : getAllProjects();
        for (int i = 0; i < projects.size(); i++) {
            Project project = (Project) projects.get(i);
            project.lock(outageMessage);
            project.logoutActiveUsers();
        }
    }

    /**
	 * Entsperrt alle Projekt dieses CMSServers.
	 */
    public void exitOutage(boolean isTest) throws RQLException {
        java.util.List<Project> projects = isTest ? getTestProjects() : getAllProjects();
        for (int i = 0; i < projects.size(); i++) {
            Project project = (Project) projects.get(i);
            project.unlock();
        }
    }

    /**
	 * Lifert den RQLNode fï¿½r das gegebene Projekt oder null zurï¿½ck. Null signalisiert, dass dieses Projekt nicht fï¿½r den User
	 * zugelassen ist, oder die GUID falsch ist.
	 * 
	 * @param projectGuid
	 *            GUID des Projectes, an den sich der User anmelden will.
	 * @return <code>RQLNode</code> or null
	 */
    private RQLNode findUserProjectNodeByGuid(String projectGuid) throws RQLException {
        RQLNodeList nodes = getProjectsNodeList(getConnectedUser());
        RQLNode node = null;
        for (int i = 0; i < nodes.size(); i++) {
            node = nodes.get(i);
            if (node.getAttribute("guid").equals(projectGuid)) {
                return node;
            }
        }
        return null;
    }

    /**
	 * Liefert den RQLNode fï¿½r das gegebene Projekt oder null zurï¿½ck. Null signalisiert, dass dieses Projekt nicht fï¿½r den User
	 * zugelassen ist, oder die GUID falsch ist.
	 * 
	 * @param projectGuid
	 *            GUID des Projectes, an den sich der User anmelden will.
	 * @return <code>RQLNode</code> or null
	 */
    private RQLNode findUserProjectNodeByName(String projectName) throws RQLException {
        RQLNodeList nodes = getProjectsNodeList(getConnectedUser());
        RQLNode node = null;
        for (int i = 0; i < nodes.size(); i++) {
            node = nodes.get(i);
            if (node.getAttribute("name").equals(projectName)) {
                return node;
            }
        }
        return null;
    }

    /**
	 * Liefert alle gerade am CMS angemeldeten Benutzer.
	 */
    public java.util.List<User> getAllActiveUsers() throws RQLException {
        java.util.List<User> activeUsers = new ArrayList<User>();
        java.util.List<Project> projects = getAllProjects();
        for (int i = 0; i < projects.size(); i++) {
            Project p = (Project) projects.get(i);
            activeUsers.addAll(p.getActiveUsers());
        }
        return activeUsers;
    }

    /**
	 * Liefert alle auf diesem RD Server eingerichteten Plugins (aktive und inaktive) unabhï¿½ngig von der Projektzuweisung.
	 */
    public java.util.List<Plugin> getAllPlugins() throws RQLException {
        RQLNodeList nodes = getPluginsNodeList();
        java.util.List<Plugin> plugins = new ArrayList<Plugin>();
        if (nodes == null) {
            return plugins;
        }
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                RQLNode node = nodes.get(i);
                plugins.add(buildPlugin(node));
            }
        }
        return plugins;
    }

    /**
	 * Liefert alle Projekt auf diesem CMS Server, unabhï¿½ngig von den Rechten des angemeldeten Users.
	 */
    public java.util.List<Project> getAllProjects() throws RQLException {
        return wrapProjectNodes(getProjectsNodeList());
    }

    /**
	 * Liefert alle User zurï¿½ck, die auf diesem CMS Server konfiguriert sind. Diese Abfrage kann nur ein Administrator ausfï¿½hren.
	 */
    public java.util.List<User> getAllUsers() throws RQLException {
        return wrapUserNodes(getAllUsersNodeList());
    }

    /**
	 * Liefert die RQLNodeList aller User zurï¿½ck, die auf diesem CMS Server konfiguriert sind. Diese Abfrage kann nur ein
	 * Administrator ausfï¿½hren.
	 */
    private RQLNodeList getAllUsersNodeList() throws RQLException {
        if (allUsersNodeListCache == null) {
            String rqlRequest = "<IODATA loginguid='" + getLogonGuid() + "'>" + "  <ADMINISTRATION>" + "   <USERS action='list'/>" + "  </ADMINISTRATION>" + "</IODATA>";
            RQLNode rqlResponse = callCms(rqlRequest);
            allUsersNodeListCache = rqlResponse.getNodes("USER");
        }
        return allUsersNodeListCache;
    }

    /**
	 * Diese Methode schickt die Ã¼bergebene rqlQuery an das CMS und liefert das Ergebnis als InputStream zurï¿½ck. Die aufrufende
	 * Methode muï¿½ sich darum kï¿½mmern, den Stream wieder zu schlieï¿½en.
	 * 
	 * @param rqlQuery:
	 *            s.o.
	 * @return InputStream: s.o.
	 * @throws RQLException
	 */
    private InputStream getCMSResultAsStream(String rqlQuery) throws RQLException {
        OutputStreamWriter osr = null;
        try {
            URL url = getCmsServerConnectionUrl();
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            osr = new OutputStreamWriter(conn.getOutputStream(), getRequestWriterEncoding());
            osr.write(rqlQuery);
            osr.flush();
            return conn.getInputStream();
        } catch (IOException ioe) {
            throw new RQLException("IO Exception reading result from server", ioe);
        } finally {
            if (osr != null) {
                try {
                    osr.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
	 * Liefert die URL zu dem CMS Server zurï¿½ck, mit der sich dieser CmsClient verbindet.
	 * <p>
	 * Wird er nicht im Konstruktor gesetzt, wird er per default aus rql_fw.properties gelesen.
	 * 
	 * @throws RQLException
	 */
    public URL getCmsServerConnectionUrl() throws RQLException {
        if (cmsServerConnectionUrl == null) {
            setCmsServerConnectionUrl(ResourceBundle.getBundle("com.hlcl.rql.as.rql_fw").getString("cmsServerConnectionUrl"));
        }
        return cmsServerConnectionUrl;
    }

    /**
	 * Returns the encoding which is used to read the response from the bridge asp.
	 */
    private String getResponseReaderEncoding() throws RQLException {
        return ResourceBundle.getBundle("com.hlcl.rql.as.rql_fw").getString("responseReaderEncoding");
    }

    /**
	 * Returns the encoding which is used to write the request to the bridge asp.
	 */
    private String getRequestWriterEncoding() throws RQLException {
        return ResourceBundle.getBundle("com.hlcl.rql.as.rql_fw").getString("requestWriterEncoding");
    }

    /**
	 * Liefert den angemeldeten Benutzer, falls vorhanden.
	 * <p>
	 * Liegt keine User GUID vor, wird versucht diese aus dem sessionKey zu ermitteln.
	 * <p>
	 * Dazu muss vorher ein Projekt gewï¿½hlt worden sein.
	 */
    public User getConnectedUser() throws RQLException {
        if (connectedUser != null) {
            return connectedUser;
        } else if (currentProject == null) {
            throw new MissingGuidException("The GUID for the connected user is missing. You created this CMS client by logon GUID only and did not select a project via session key, therefore no user GUID is available. Select a project via the session key, create a client by user name and password or create this CMS client with user GUID.");
        } else {
            return new User(this, getConnectedUserGuid(getCurrentProject().getSessionKey()));
        }
    }

    /**
	 * Liefert die Locale des angemeldeten Benutzers zurï¿½ck.
	 */
    public Locale getConnectedUserLocale() throws RQLException {
        return getConnectedUser().getLocale();
    }

    /**
	 * Liefert die EmailAddresse des angemeldeten Benutzers zurï¿½ck.
	 */
    public String getConnectedUserEmailAddress() throws RQLException {
        return getConnectedUser().getEmailAddress();
    }

    /**
	 * Liefert den angemeldeten Benutzer ï¿½ber eine beliebige Seite des aktuellen Projektes. Skurilerweise liefert das CMS die GUID
	 * des angemeldeten Benuter ï¿½ber die Seite zurï¿½ck. Und die meisten Scripte beziehen sich auf eine Seite, diese ist also
	 * vorhanden. Das funktioniert auch bereits in V5.6!
	 */
    public User getConnectedUser(Page page) throws RQLException {
        connectedUser = new User(this, page.getConnectedUserGuid());
        return connectedUser;
    }

    /**
	 * Liefert den angemeldeten Benutzer fï¿½r den gegebenen session key. Erst ab V6!.
	 */
    public User getConnectedUser(String sessionKey) throws RQLException {
        connectedUser = new User(this, getConnectedUserGuid(sessionKey));
        return connectedUser;
    }

    /**
	 * Liefert die GUID des angemeldeten Benutzers aus dem SessionKey des aktuellen Projektes.
	 */
    private String getConnectedUserGuid(String sessionKey) throws RQLException {
        return getConnectedUserNode(sessionKey).getAttribute("guid");
    }

    /**
	 * Liefert den RQLNode fï¿½r den angemeldeten Benutzer aus dem SessionKey des aktuellen Projektes.
	 */
    private RQLNode getConnectedUserNode(String sessionKey) throws RQLException {
        String rqlRequest = "<IODATA loginguid='" + getLogonGuid() + "'>" + "  <PROJECT sessionkey='" + sessionKey + "'>" + "   <USER action='sessioninfo'/>" + "  </PROJECT>" + "</IODATA>";
        RQLNode rqlResponse = callCms(rqlRequest);
        return rqlResponse.getNode("USER");
    }

    /**
	 * Liefert das aktuell gewï¿½hlte Projekt, falls vorher ein Projekt geholt wurde; sonst null.
	 * 
	 * @see <code>getProject</code>
	 */
    public Project getCurrentProject() {
        return currentProject;
    }

    /**
	 * Liefert die GUID des aktuell gewï¿½hlten Projektes, falls vorher ein Projekt geholt wurde; sonst null.
	 * 
	 * @see <code>getProject</code>
	 */
    public String getCurrentProjectGuid() {
        return currentProject.getProjectGuid();
    }

    /**
	 * Liefert die Locale fï¿½r die gegebene locale ID (z.b. Germany = 1031) zurï¿½ck.
	 * 
	 * @throws ElementNotFoundException
	 *             if locale cannot be found
	 */
    public Locale getLocaleByLcid(String localeId) throws RQLException {
        RQLNodeList localeNodeList = getLocaleNodeList();
        for (int i = 0; i < localeNodeList.size(); i++) {
            RQLNode localeNode = localeNodeList.get(i);
            String lcid = localeNode.getAttribute("lcid");
            if (lcid.equals(localeId)) {
                return new Locale(this, lcid, localeNode.getAttribute("id"), localeNode.getAttribute("country"), localeNode.getAttribute("language"));
            }
        }
        throw new ElementNotFoundException("The locale for the given locale ID " + localeId + " cannot be found.");
    }

    /**
	 * Liefert die RQLNodeList fï¿½r alle Locale des RD Servers.
	 */
    private RQLNodeList getLocaleNodeList() throws RQLException {
        if (allLocalesNodeListCache == null) {
            String rqlRequest = "<IODATA loginguid='" + getLogonGuid() + "'>" + "  <LANGUAGE action='list'/>" + "</IODATA>";
            RQLNode rqlResponse = callCms(rqlRequest);
            allLocalesNodeListCache = rqlResponse.getNodes("LIST");
        }
        return allLocalesNodeListCache;
    }

    /**
	 * Liefert die RedDot logon GUID.
	 */
    public String getLogonGuid() {
        return logonGuid;
    }

    /**
	 * Liefert das Plugin mit dem gegebenen Namen oder null, falls keines gefunden werden kann.
	 * <p>
	 * Check with equals().
	 */
    public Plugin getPluginByName(String pluginName) throws RQLException {
        RQLNodeList nodes = getPluginsNodeList();
        if (nodes == null) {
            return null;
        }
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                RQLNode node = nodes.get(i);
                if (node.getAttribute("name").equals(pluginName)) {
                    return buildPlugin(node);
                }
                ;
            }
        }
        return null;
    }

    /**
	 * Liefert alle Plugins, deren Name mit dem gegebenen Prefix beginnt oder eine leere Liste, falls keines gefunden werden kann.
	 * <p>
	 * Check with startsWith().
	 */
    public Set<Plugin> getPluginsByNamePrefix(String pluginNamePrefix) throws RQLException {
        RQLNodeList nodes = getPluginsNodeList();
        if (nodes == null) {
            return null;
        }
        HashSet<Plugin> result = new HashSet<Plugin>();
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                RQLNode node = nodes.get(i);
                if (node.getAttribute("name").startsWith(pluginNamePrefix)) {
                    result.add(buildPlugin(node));
                }
                ;
            }
        }
        return result;
    }

    /**
	 * Builds a plugin object from given node.
	 */
    Plugin buildPlugin(RQLNode node) {
        return new Plugin(this, node.getAttribute("guid"), node.getAttribute("active"), node.getAttribute("name"));
    }

    /**
	 * Liefert die RQLNode List fï¿½r alle Plugins vom RD Server.
	 */
    private RQLNodeList getPluginsNodeList() throws RQLException {
        if (allPluginsNodeListCache == null) {
            String rqlRequest = "<IODATA loginguid='" + getLogonGuid() + "'>" + "  <PLUGINS action='list'  byproject='0'/>" + "</IODATA>";
            RQLNode rqlResponse = callCms(rqlRequest);
            allPluginsNodeListCache = rqlResponse.getNodes("PLUGIN");
        }
        return allPluginsNodeListCache;
    }

    /**
	 * Creates a new plugin from the the Plugins XML file, which has to be a local path on the CMS server itself. Returns the imported
	 * plugin. Same functionality as @link {@link #addPlugin(String)}.
	 */
    public Plugin importPlugin(String definitionXmlPathOnCmsServer) throws RQLException {
        return addPlugin(definitionXmlPathOnCmsServer);
    }

    /**
	 * Creates a new plugin from the the Plugins XML file, which has to be a local path on the CMS server itself. Returns the imported
	 * plugin. Same functionality as {@link #importPlugin(String)}.
	 */
    public Plugin addPlugin(String definitionXmlPathOnCmsServer) throws RQLException {
        String rqlRequest = "<IODATA loginguid='" + getLogonGuid() + "'>" + "  <PLUGINS action='import' source='" + definitionXmlPathOnCmsServer + "'/>" + "</IODATA>";
        RQLNode rqlResponse = callCms(rqlRequest);
        allPluginsNodeListCache = null;
        return getPluginByName(rqlResponse.getNode("INFO").getAttribute("caption"));
    }

    /**
	 * Erzeugt ein Project aus dem gegebenen sessionKey. Die GUID des Projektes wird ermittelt.
	 * <p>
	 * Das aktuell gewï¿½hlte Projekt wird festgehalten.
	 * 
	 * @param sessionKey
	 *            aktueller Sessionkey
	 * @return Project
	 */
    public Project getProject(String sessionKey) throws RQLException {
        return getProject(sessionKey, getProjectGuid(sessionKey));
    }

    /**
	 * Erzeugt ein Project aus dem gegebenen sessionKey und der GUID des Projektes.
	 * <p>
	 * Das aktuell gewï¿½hlte Projekt wird festgehalten.
	 * 
	 * @param sessionKey
	 *            aktueller Sessionkey
	 * @param projectGuid
	 *            Guid des Projektes
	 * @return Project
	 */
    public Project getProject(String sessionKey, String projectGuid) throws RQLException {
        if (currentProject == null || !currentProject.getProjectGuid().equals(projectGuid)) {
            if (projectGuid == null) {
                projectGuid = getProjectGuid(sessionKey);
            }
            setCurrentProject(new Project(this, sessionKey, projectGuid));
        }
        return currentProject;
    }

    /**
	 * Erzeugt ein Project aus der gegebenen GUID des Projektes. Das aktuell gewï¿½hlte Projekt wird festgehalten.
	 * 
	 * @param projectGuid
	 *            Guid des Projektes
	 * @return Project
	 */
    public Project getProjectByGuid(String projectGuid) throws RQLException {
        if (currentProject == null || !currentProject.getProjectGuid().equals(projectGuid)) {
            RQLNode projectNodeOrNull = findUserProjectNodeByGuid(projectGuid);
            if (projectNodeOrNull == null) {
                throw new ProjectNotFoundException("Project with GUID " + projectGuid + " could not be found, or the user could not access this project.");
            }
            setCurrentProject(new Project(this, projectGuid));
            currentProject.validate();
        }
        return currentProject;
    }

    /**
	 * Erzeugt ein Project mit dem gegebenen Namen. Der Benutzer muss auf dieses Projekt berechtigt sind. Das aktuell gewï¿½hlte
	 * Projekt wird festgehalten.
	 * 
	 * @param projectName
	 *            Name des Projektes, z.b. hip.hlcl.com
	 * @return Project
	 */
    public Project getProjectByName(String projectName) throws RQLException {
        if (currentProject == null || !currentProject.getName().equals(projectName)) {
            RQLNode projectNodeOrNull = findUserProjectNodeByName(projectName);
            if (projectNodeOrNull == null) {
                throw new ProjectNotFoundException("Project with name " + projectName + " could not be found, or the user could not access this project.");
            }
            setCurrentProject(new Project(this, projectNodeOrNull.getAttribute("guid")));
            currentProject.validate();
        }
        return currentProject;
    }

    /**
	 * Liefert die GUID des aktuellen Projektes aus dem session key.
	 */
    private String getProjectGuid(String sessionKey) throws RQLException {
        return getConnectedUserNode(sessionKey).getAttribute("projectguid");
    }

    /**
	 * Liefert die RQLNodeList mit den Projekten auf diesem CMS Server.
	 * 
	 * @return <code>RQLNodeList</code>
	 */
    private RQLNodeList getProjectsNodeList() throws RQLException {
        if (projectsNodeListCache == null) {
            String rqlRequest = "<IODATA loginguid='" + getLogonGuid() + "'>" + " <ADMINISTRATION>" + "   <PROJECTS action='list'/>" + " </ADMINISTRATION>" + "</IODATA>";
            RQLNode rqlResponse = callCms(rqlRequest);
            projectsNodeListCache = rqlResponse.getNodes("PROJECT");
        }
        return projectsNodeListCache;
    }

    /**
	 * Liefert die RQLNodeList mit allen Projekt-Nodes des gegebenen Users zurï¿½ck.
	 * 
	 * @return <code>RQLNodeList</code>
	 */
    RQLNodeList getProjectsNodeList(User user) throws RQLException {
        String rqlRequest = "<IODATA loginguid='" + getLogonGuid() + "'>" + " <ADMINISTRATION>" + "  <USER guid='" + user.getUserGuid() + "'>" + "   <PROJECTS action='list'/>" + "  </USER>" + " </ADMINISTRATION>" + "</IODATA>";
        RQLNode rqlResponse = callCms(rqlRequest);
        return rqlResponse.getNodes("PROJECT");
    }

    /**
	 * Liefert eine List mit Projekte fï¿½r den Test.
	 */
    public java.util.List<Project> getTestProjects() {
        java.util.List<Project> tps = new ArrayList<Project>(1);
        tps.add(new Project(this, "EA851692656044EEB27D9C482C7F0878"));
        tps.add(new Project(this, "E62CF0C8E4EC4D018C3E392C42A12161"));
        return tps;
    }

    /**
	 * Liefert ein paar User zurï¿½ck, an die testweise ein mail versendet werden kann.
	 */
    public java.util.List<User> getTestUsers() {
        java.util.List<User> testUsers = new ArrayList<User>();
        testUsers.add(new User(this, "lejafr4", "198C466E5362482EBBD0AEE77BF141C3", "user id", "Frank Leja", "lejafr@hlcl.com"));
        return testUsers;
    }

    /**
	 * Liefert den User mit dem gegebenen Namen zurï¿½ck.
	 * <p>
	 * Nur mit Administratorrechten benutzbar.
	 */
    public User getUserByName(String userName) throws RQLException {
        RQLNodeList usersList = getAllUsersNodeList();
        RQLNode userNode = null;
        for (int i = 0; i < usersList.size(); i++) {
            userNode = usersList.get(i);
            if (userNode.getAttribute("name").equals(userName)) {
                return buildUser(userNode);
            }
        }
        throw new ElementNotFoundException("User with name " + userName + " is not configured at the CMS server.");
    }

    /**
	 * Liefert die UserInterfaceLanguage fï¿½r die gegebene language ID (z.B. DEU, ENG) zurï¿½ck.
	 * 
	 * @throws ElementNotFoundException
	 *             if language cannot be found
	 */
    public UserInterfaceLanguage getUserInterfaceLanguageByLanguageId(String languageId) throws RQLException {
        RQLNodeList languageNodeList = getUserInterfaceLanguageNodeList();
        for (int i = 0; i < languageNodeList.size(); i++) {
            RQLNode languageNode = languageNodeList.get(i);
            String id = languageNode.getAttribute("id");
            if (id.equals(languageId)) {
                return buildUserInterfaceLanguage(languageNode);
            }
        }
        throw new ElementNotFoundException("The user interface language for the given language ID " + languageId + " cannot be found.");
    }

    /**
	 * Liefert die UserInterfaceLanguage fï¿½r die gegebene RFC language ID (z.B. de-de, fr-ca) zurï¿½ck.
	 * 
	 * @throws ElementNotFoundException
	 *             if language cannot be found
	 */
    public UserInterfaceLanguage getUserInterfaceLanguageByRfcId(String rfcLanguageId) throws RQLException {
        RQLNodeList languageNodeList = getUserInterfaceLanguageNodeList();
        for (int i = 0; i < languageNodeList.size(); i++) {
            RQLNode languageNode = languageNodeList.get(i);
            String id = languageNode.getAttribute("rfclanguageid");
            if (id.equals(rfcLanguageId)) {
                return buildUserInterfaceLanguage(languageNode);
            }
        }
        throw new ElementNotFoundException("The user interface language for the given RFC language ID " + rfcLanguageId + " cannot be found.");
    }

    /**
	 * Liefert die RQLNodeList fï¿½r alle Oberflï¿½chensprachen des RD Servers.
	 */
    private RQLNodeList getUserInterfaceLanguageNodeList() throws RQLException {
        if (allUserInterfaceLanguagesNodeListCache == null) {
            String rqlRequest = "<IODATA loginguid='" + getLogonGuid() + "'>" + "  <DIALOG action='listlanguages'/>" + "</IODATA>";
            RQLNode rqlResponse = callCms(rqlRequest);
            allUserInterfaceLanguagesNodeListCache = rqlResponse.getNodes("LIST");
        }
        return allUserInterfaceLanguagesNodeListCache;
    }

    /**
	 * Liefert alle UserInterfaceLanguages zurï¿½ck.
	 */
    public java.util.List<UserInterfaceLanguage> getUserInterfaceLanguages() throws RQLException {
        RQLNodeList languageNodeList = getUserInterfaceLanguageNodeList();
        java.util.List<UserInterfaceLanguage> result = new ArrayList<UserInterfaceLanguage>(languageNodeList.size());
        for (int i = 0; i < languageNodeList.size(); i++) {
            RQLNode languageNode = languageNodeList.get(i);
            result.add(buildUserInterfaceLanguage(languageNode));
        }
        return result;
    }

    /**
	 * Do all steps needed to login from given user name and password.
	 * 
	 * @param passwordAuthentication
	 * @throws UserAlreadyLoggedInException
	 * @throws RQLException
	 * @throws UnknownUserOrWrongPasswordException
	 */
    private void login(PasswordAuthentication passwordAuthentication) throws UserAlreadyLoggedInException, RQLException, UnknownUserOrWrongPasswordException {
        String userName = passwordAuthentication.getUserName();
        String password = passwordAuthentication.getPassword();
        RQLNode rqlResponse = null;
        try {
            String rqlRequest = "<IODATA>" + " <ADMINISTRATION action='login' name='" + userName + "' password='" + password + "'/>" + "</IODATA>";
            rqlResponse = callCms(rqlRequest);
        } catch (RQLException rqle) {
            if (rqle.getMessage().indexOf("#RDError101") > 0) {
                throw new UserAlreadyLoggedInException("The user with name " + userName + " is already logged in. Please logout this user and try again.");
            }
            throw rqle;
        }
        logonGuid = rqlResponse.getNode("LOGIN").getAttribute("guid");
        if (logonGuid == null || logonGuid.length() == 0) {
            throw new UnknownUserOrWrongPasswordException("The user with name " + userName + " is unknown or the given password is not correct.");
        }
        connectedUser = new User(this, rqlResponse.getNode("USER").getAttribute("guid"), logonGuid);
    }

    /**
	 * Meldet den User mit der gegebenen logon GUID vom CMS ab.
	 */
    void logout(String logonGuid) throws RQLException {
        String rqlRequest = "<IODATA loginguid='" + logonGuid + "'>" + "  <ADMINISTRATION>" + "   <LOGOUT guid='" + logonGuid + "'/>" + "  </ADMINISTRATION>" + "</IODATA>";
        callCms(rqlRequest);
    }

    /**
	 * Liefert einen LDAP directory service context zurï¿½ck. Muss mit closeLdapContext() geschlossen werden.
	 * 
	 * @see #closeLdapContext()
	 */
    public DirContext openLdapContext() throws RQLException {
        PropertyResourceBundle bundle = (PropertyResourceBundle) PropertyResourceBundle.getBundle("com.hlcl.rql.as.rql_fw");
        String ldapServerName = bundle.getString(LDAP_SERVER_NAME_KEY).trim();
        String ldapServerPort = bundle.getString(LDAP_SERVER_PORT_KEY).trim();
        Hashtable<String, String> environment = new Hashtable<String, String>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, "ldap://" + ldapServerName + ":" + ldapServerPort);
        try {
            ldapContext = new InitialDirContext(environment);
        } catch (NamingException ex) {
            throw new RQLException("Could not create LDAP context", ex);
        }
        return ldapContext;
    }

    /**
	 * Sendet eine Mail an alle gegebenen User.
	 */
    public void sendMail(java.util.List<User> users, String from, String subject, String message) throws RQLException {
        String[] to = new String[users.size()];
        Iterator<User> it = users.iterator();
        int i = 0;
        User user = null;
        while (it.hasNext()) {
            user = (User) it.next();
            to[i++] = user.getEmailAddress();
        }
        sendMail(from, to, subject, message);
    }

    /**
	 * Sendet eine Mail an einen Empfï¿½nger.
	 */
    public void sendMail(String from, String to, String subject, String message) throws RQLException {
        String[] toAddresses = new String[1];
        toAddresses[0] = to;
        sendMail(from, toAddresses, subject, message);
    }

    /**
	 * Sends an e-mail to the given list of addresses.
	 */
    public void sendMail(String from, String toAddresses, String delimiter, String subject, String message) throws RQLException {
        sendMail(from, StringHelper.split(toAddresses, delimiter), subject, message);
    }

    /**
	 * Anbindung an den DS MailService.
	 */
    public void sendMail(String from, String[] toAddresses, String subject, String message) throws RQLException {
        try {
            CmsClient.sendMail(from, toAddresses, subject, message, null, null);
        } catch (MessagingException me) {
            throw new RQLException("The e-mail '" + subject + "' could not be send.", me);
        }
    }

    /**
	 * Sendet eine Mail an alle eingerichteten User.
	 */
    public void sendMailToAllUsers(String from, String subject, String message, boolean isTest) throws RQLException {
        sendMail(isTest ? getTestUsers() : getAllUsers(), from, subject, message);
    }

    /**
	 * Sendet eine Mail mit Statistikinformationen (Dauer, Zeitpunkt...) im CSV Format an statisticReceiver.
	 * <p>
	 * 
	 * @param statisticReceiver
	 *            Zieladresse
	 * @param sourceId
	 *            ID des Scriptes für das die Informationen sind
	 * @param start
	 *            Startzeitpunkt in 1/1000 s
	 * @param end
	 *            Endzeitpunkt in 1/1000s
	 * @param additionalHeader
	 *            zusätzliche Headerfelder (mit ; getrennt)
	 * @param additionalData
	 *            zusätzliche Datenfelder (mit ; getrennt)
	 * @throws RQLException
	 */
    public void sendStatisticMail(String statisticReceiver, String sourceId, long start, long end, String additionalHeader, String additionalData) throws RQLException {
        StringBuffer buffer = new StringBuffer();
        buffer.append("sourceId;date;time;duration in s;" + additionalHeader + "\n");
        buffer.append(sourceId + ";");
        Date d = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        buffer.append(df.format(d) + ";");
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
        buffer.append(tf.format(d) + ";");
        buffer.append((end - start) / 1000 + ";");
        buffer.append(additionalData + "\n");
        sendMail(STATISTIC_MAIL_FROM_ADDRESS, statisticReceiver, sourceId + " statistic", buffer.toString());
    }

    /**
	 * @param cmsServerConnectionUrl
	 *            the cmsServerConnectionUrl to set
	 */
    private void setCmsServerConnectionUrl(String cmsServerConnectionUrl) throws RQLException {
        try {
            this.cmsServerConnectionUrl = new URL(cmsServerConnectionUrl);
        } catch (MalformedURLException ex) {
            throw new RQLException("URL of CMS server connection " + cmsServerConnectionUrl + " is not valid. Has to be similar to http://reddot.hlcl.com/cms/hlclRemoteRQL.asp", ex);
        }
    }

    /**
	 * Ändert den Cache für das Project auf das gegebenen Project. Ein eventuell vorhandenes altes Projekt-Objekt wird unbenutzbar
	 * gemacht.
	 */
    private void setCurrentProject(Project project) {
        if (currentProject != null) {
            currentProject.invalidate();
        }
        currentProject = project;
    }

    /**
	 * Hält die Ausführung für die gegebenen Sekunden an.
	 */
    public void wait(int seconds) throws RQLException {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            throw new RQLException("Waiting for " + seconds + " seconds were interrupted.", ie);
        }
    }

    /**
	 * Hält die Ausführung für die gegebenen Sekunden an.
	 */
    public void wait(String seconds) throws RQLException {
        wait(Integer.parseInt(seconds));
    }

    /**
	 * Erzeugt für alle gegebenen Projekt-Nodes Projekte und liefert sie gesammelt in einer Liste zurück.
	 */
    java.util.List<Project> wrapProjectNodes(RQLNodeList projectsNodeList) throws RQLException {
        java.util.List<Project> projects = new ArrayList<Project>(projectsNodeList.size());
        for (int i = 0; i < projectsNodeList.size(); i++) {
            RQLNode node = projectsNodeList.get(i);
            projects.add(new Project(this, node.getAttribute("guid")));
        }
        return projects;
    }

    /**
	 * Wandelt alle gegebenen user nodes in eine Liste mit User-Objekten um.
	 * 
	 * @param userNodeList
	 *            liste der umzuwandelden user nodes
	 */
    private java.util.List<User> wrapUserNodes(RQLNodeList userNodeList) {
        RQLNode node = null;
        java.util.List<User> users = new ArrayList<User>();
        if (userNodeList != null) {
            for (int i = 0; i < userNodeList.size(); i++) {
                node = userNodeList.get(i);
                users.add(buildUser(node));
            }
        }
        return users;
    }

    /**
	 * Setzt alle Plugins, die namePart im Namen haben, auf active=true. Returns all changed plugins.
	 */
    public java.util.List<Plugin> enablePluginsByNameContains(String namePart, boolean ignoreCase) throws RQLException {
        java.util.List<Plugin> result = new ArrayList<Plugin>();
        for (Plugin plugin : getPluginsByNameContains(namePart, ignoreCase)) {
            plugin.setIsActive(true);
            result.add(plugin);
        }
        return result;
    }

    /**
	 * Löscht alle Plugins, die namePart im Namen haben. Returns the number of deleted plug-ins. 
	 */
    public int deletePluginsByNameContains(String namePart, boolean ignoreCase) throws RQLException {
        List<Plugin> plugins = getPluginsByNameContains(namePart, ignoreCase);
        for (Plugin plugin : plugins) {
            plugin.delete();
        }
        return plugins.size();
    }

    /**
	 * Setzt alle Plugins, die namePart im Namen haben, auf active=false. Returns all changed plugins.
	 */
    public java.util.List<Plugin> disablePluginsByNameContains(String namePart, boolean ignoreCase) throws RQLException {
        java.util.List<Plugin> result = new ArrayList<Plugin>();
        for (Plugin plugin : getPluginsByNameContains(namePart, ignoreCase)) {
            plugin.setIsActive(false);
            result.add(plugin);
        }
        return result;
    }

    /**
	 * Returns all plug-ins which name contains given namePart. Check with contains; case depending on given ignoreCase.
	 */
    public java.util.List<Plugin> getPluginsByNameContains(String namePart, boolean ignoreCase) throws RQLException {
        java.util.List<Plugin> result = new ArrayList<Plugin>();
        for (Plugin plugin : getAllPlugins()) {
            if (StringHelper.contains(plugin.getName(), namePart, ignoreCase)) {
                result.add(plugin);
            }
        }
        return result;
    }

    /**
	 * Returns all active plug-ins which name contains given namePart. Check with contains; case depending on given ignoreCase.
	 */
    public java.util.List<Plugin> getActivePluginsByNameContains(String namePart, boolean ignoreCase) throws RQLException {
        java.util.List<Plugin> result = new ArrayList<Plugin>();
        for (Plugin plugin : getPluginsByNameContains(namePart, ignoreCase)) {
            if (plugin.isActive()) {
                result.add(plugin);
            }
        }
        return result;
    }
}
