package code.connection;

import code.diff.Diff;
import main.Config;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class provides functions to do API queries.
 *
 * @author ireas
 */
public final class API {

    /**
     * The logger for error / info output on command line.
     */
    private static Logger logger = Logger.getLogger(API.class);

    /**
     * DocumentBuilder for parsing the results.
     */
    private static DocumentBuilder db;

    /**
     * Path to the API script (api.php).
     */
    private static String path;

    /**
     * The cookies we retrieve from the API.
     */
    private static String cookies = new String();

    /**
     * The constructor does nothing and is private, as this is
     * a static utility class that shouldn't be instanced.
     */
    private API() {
    }

    /**
     * Does the login on the server.
     *
     * @param username username
     * @param pass password
     *
     * @return result of the login action
     */
    public static LoginResult login(final String username, final String pass) {
        LoginResult ret = LoginResult.UNKNOWN_ERROR;
        logger.info("Logging in as " + username + ".");
        if (!initXMLParser()) {
            return ret;
        }
        Map<String, String> arg = new HashMap<String, String>();
        arg.put("action", "login");
        arg.put("lgname", username);
        arg.put("lgpassword", pass);
        String result = apiQuery(arg);
        InputSource src = new InputSource(new StringReader(result));
        Document doc;
        try {
            doc = db.parse(src);
        } catch (SAXException e) {
            logger.error("Couldn't parse XML result: " + e.getMessage());
            return ret;
        } catch (IOException e) {
            logger.error("Couldn't parse XML result: " + e.getMessage());
            return ret;
        }
        doc.getDocumentElement().normalize();
        NodeList logs = doc.getElementsByTagName("login");
        if (logs.getLength() > 0) {
            Node n = logs.item(0);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                if (e.hasAttribute("result")) {
                    String res = e.getAttribute("result");
                    if (res.equals("Success")) {
                        ret = LoginResult.SUCCESS;
                    } else if (res.equals("EmptyPass")) {
                        ret = LoginResult.NO_PASS;
                    } else if (res.equals("Illegal") || res.equals("NotExists")) {
                        ret = LoginResult.WRONG_USER;
                    } else if (res.equals("WrongPass") || res.equals("WrongPluginPass")) {
                        ret = LoginResult.WRONG_PASS;
                    } else if (res.equals("Blocked")) {
                        ret = LoginResult.USER_BLOCKED;
                    } else if (res.equals("Throttled")) {
                        ret = LoginResult.THROTTLED;
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Init the DocumentBuilder so that we're able to
     * parse a XML result.
     *
     * @return success of initialization
     */
    private static boolean initXMLParser() {
        if (db == null) {
            try {
                db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                logger.error("Couldn't create XML parser: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * Does the login action.
     */
    public static void logout() {
        logger.info("Logging out.");
        Map<String, String> arg = new HashMap<String, String>();
        arg.put("action", "logout");
        apiQuery(arg);
    }

    /**
     * Get a rollback token.
     *
     * @param page page name
     *
     * @return rollback token; empty if not successfull
     */
    private static String getRollbackToken(final String page) {
        String token = new String();
        if (!initXMLParser()) {
            return token;
        }
        Map<String, String> args = new HashMap<String, String>();
        args.put("action", "query");
        args.put("prop", "revisions");
        args.put("rvtoken", "rollback");
        args.put("titles", page);
        String result = apiQuery(args);
        InputSource src = new InputSource(new StringReader(result));
        Document doc;
        try {
            doc = db.parse(src);
        } catch (SAXException e) {
            logger.error("Couldn't parse XML result: " + e.getMessage());
            return token;
        } catch (IOException e) {
            logger.error("Couldn't parse XML result: " + e.getMessage());
            return token;
        }
        doc.getDocumentElement().normalize();
        NodeList errors = doc.getElementsByTagName("error");
        if (errors.getLength() == 0) {
            NodeList revs = doc.getElementsByTagName("rev");
            if (revs.getLength() == 0) {
                logger.error("Received invalid XML: no rev tag!");
            } else {
                Node p = revs.item(0);
                if (p.getNodeType() == Node.ELEMENT_NODE) {
                    Element revE = (Element) p;
                    if (revE.hasAttribute("rollbacktoken")) {
                        token = revE.getAttribute("rollbacktoken");
                        logger.info("Successfully retreived rollback token for " + page + ": " + token);
                    } else {
                        logger.warn("Couldn't retreive rollback token - " + "not authorized?");
                    }
                }
            }
        } else {
            logger.error("An error occured trying to retreive a " + "rollback token!");
        }
        return token;
    }

    /**
     * Rollback an edit.
     *
     * @param d edit to rollback
     *
     * @return success of the rollback
     */
    public static RollbackResult rollback(final Diff d) {
        String title = d.getPagename();
        String user = d.getEditor();
        return rollback(title, user);
    }

    /**
     * Rollback an edit.
     *
     * @param title page to rollback
     * @param user user to rollback
     *
     * @return success of the rollback
     */
    public static RollbackResult rollback(final String title, final String user) {
        RollbackResult result = RollbackResult.UNKNOWN_ERROR;
        String token = getRollbackToken(title);
        if (token == null || token.isEmpty()) {
            result = RollbackResult.TOKEN_ERROR;
        } else {
            Map<String, String> args = new HashMap<String, String>();
            args.put("action", "rollback");
            args.put("title", title);
            args.put("user", user);
            args.put("token", token);
            String res = apiQuery(args);
            InputSource src = new InputSource(new StringReader(res));
            Document doc;
            try {
                doc = db.parse(src);
            } catch (SAXException e) {
                logger.error("Couldn't parse XML result: " + e.getMessage());
                return result;
            } catch (IOException e) {
                logger.error("Couldn't parse XML result: " + e.getMessage());
                return result;
            }
            doc.getDocumentElement().normalize();
            NodeList rollbacks = doc.getElementsByTagName("rollback");
            if (rollbacks.getLength() > 0) {
                result = RollbackResult.SUCCESS;
            } else {
                NodeList errors = doc.getElementsByTagName("error");
                if (errors.getLength() > 0) {
                    Node e = errors.item(0);
                    if (e.getNodeType() == Node.ELEMENT_NODE) {
                        Element err = (Element) e;
                        String c = err.getAttribute("code");
                        if (c.equals("alreadyrolled") || c.equals("badtoken")) {
                            result = RollbackResult.ALREADY_ROLLED;
                        } else if (c.equals("onlyauthor")) {
                            result = RollbackResult.ONLY_AUTHOR;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Does an API query.
     *
     * @param arg POST arguments
     *
     * @return result
     */
    public static String apiQuery(final Map<String, String> arg) {
        if (arg.isEmpty()) {
            return null;
        }
        if (!arg.containsKey("format")) {
            arg.put("format", "xml");
        }
        StringBuilder data = new StringBuilder();
        boolean first = true;
        for (String key : arg.keySet()) {
            if (!first) {
                data.append("&");
            } else {
                first = false;
            }
            try {
                data.append(URLEncoder.encode(key, "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(arg.get(key), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.error("UTF-8 encoding not supported: " + e.getMessage());
                return null;
            }
        }
        if (path == null || path.isEmpty()) {
            path = Config.get("api.path");
        }
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(path);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Cookie", cookies);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(data.toString());
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                result.append(line);
                line = reader.readLine();
            }
            Map<String, List<String>> headers = conn.getHeaderFields();
            List<String> values = headers.get("Set-Cookie");
            if (values != null) {
                for (String value : values) {
                    if (cookies.isEmpty()) {
                        cookies = value;
                    } else {
                        cookies = cookies + ";" + value;
                    }
                }
            }
        } catch (MalformedURLException e) {
            logger.error("URL to api.php ist not valid: " + e.getMessage());
            return null;
        } catch (IOException e) {
            logger.error("Couldn't do API request: " + e.getMessage());
            return null;
        }
        return result.toString();
    }
}
