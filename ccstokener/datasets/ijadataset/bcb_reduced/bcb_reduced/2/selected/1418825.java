package com.rapidminer.io.community;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.rapid_i.deployment.update.client.ProgressReportingInputStream;
import com.rapid_i.deployment.update.client.ProgressReportingOutputStream;
import com.rapidminer.gui.tools.PasswordDialog;
import com.rapidminer.io.Base64;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.GlobalAuthenticator;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;

/** Connects to the myexperiment web server.
 * 
 * @author Simon Fischer
 *
 */
public class MyExperimentConnection {

    private static final String DEFAULT_URL = "http://www.myexperiment.org/";

    private static final String PARAMETER_MYEXPERIMENT_URL = "rapidminer.tools.myexperiment_url";

    static {
        ParameterService.registerParameter(new ParameterTypeString(PARAMETER_MYEXPERIMENT_URL, "URL of the myexperiment server.", DEFAULT_URL));
    }

    private static final String ENCODING_NAME = "UTF-8";

    private static final Charset ENCODING = Charset.forName(ENCODING_NAME);

    private List<License> allLicenses;

    private PasswordAuthentication authentication;

    private static MyExperimentConnection THE_INSTANCE = new MyExperimentConnection();

    private MyExperimentConnection() {
        GlobalAuthenticator.registerServerAuthenticator(new GlobalAuthenticator.URLAuthenticator() {

            @Override
            public PasswordAuthentication getAuthentication(URL url) {
                if (url.toString().startsWith(getBaseUrl())) {
                    LogService.getRoot().info("Authentication requested for " + url);
                    login();
                    return authentication;
                } else {
                    return null;
                }
            }

            @Override
            public String getName() {
                return "myExperiment authenticator.";
            }

            @Override
            public String toString() {
                return getName();
            }
        });
    }

    public static MyExperimentConnection getInstance() {
        return THE_INSTANCE;
    }

    private HttpURLConnection connectTo(String command, Map<String, String> arguments) throws IOException {
        StringBuffer urlB = new StringBuffer(getBaseUrl());
        urlB.append(command);
        if (arguments != null) {
            boolean first = true;
            for (Map.Entry<String, String> arg : arguments.entrySet()) {
                if (first) {
                    urlB.append("?");
                    first = false;
                } else {
                    urlB.append("&");
                }
                urlB.append(URLEncoder.encode(arg.getKey(), ENCODING_NAME)).append("=").append(URLEncoder.encode(arg.getValue(), ENCODING_NAME));
            }
        }
        URL url = new URL(urlB.toString());
        LogService.getRoot().info("Connecting to: " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        authenticateIfLoggedIn(connection);
        return connection;
    }

    private Document getAsXML(HttpURLConnection connection) throws IOException {
        try {
            if ((connection.getResponseCode() < 200) || (connection.getResponseCode() > 300)) {
                Document errorDoc = XMLTools.parse(connection.getErrorStream());
                String reason;
                if (errorDoc.getDocumentElement().getTagName().equals("error")) {
                    reason = XMLTools.getTagContents(errorDoc.getDocumentElement(), "reason");
                } else {
                    reason = "No reason given.";
                }
                LogService.getRoot().warning("Answer from myexperiment server: " + connection.getResponseCode() + ": " + connection.getResponseMessage() + ". Reason: " + reason);
                throw new IOException("Answer from myexperiment server: " + connection.getResponseCode() + ": " + connection.getResponseMessage() + ". Reason: " + reason);
            } else {
                Document document = XMLTools.parse(connection.getInputStream());
                Element root = document.getDocumentElement();
                if (root.getTagName().equals("error")) {
                    throw new IOException("Error " + root.getAttribute("code") + " from my experiment: " + root.getAttribute("message"));
                }
                return document;
            }
        } catch (SAXException e) {
            LogService.getRoot().log(Level.WARNING, "Cannot parse response: " + e, e);
            throw new IOException("Cannot parse response: " + e, e);
        }
    }

    private Document getAsXML(String command, Map<String, String> arguments) throws IOException {
        return getAsXML(connectTo(command, arguments));
    }

    public List<WorkflowSynopsis> getWorkflows(String searchTerms, boolean onlyRapidMiner) {
        List<WorkflowSynopsis> result = new LinkedList<WorkflowSynopsis>();
        try {
            if (searchTerms == null) {
                searchTerms = "";
            }
            if (onlyRapidMiner) {
                if (!searchTerms.isEmpty()) {
                    searchTerms += " ";
                }
                searchTerms += "kind:(RapidMiner)";
            }
            Document doc;
            if (!searchTerms.isEmpty()) {
                Map<String, String> args = new HashMap<String, String>();
                args.put("type", "workflow");
                args.put("query", searchTerms);
                args.put("num", "100");
                doc = getAsXML("search.xml", args);
            } else {
                Map<String, String> args = new HashMap<String, String>();
                args.put("num", "100");
                doc = getAsXML("workflows.xml", args);
            }
            NodeList workflowElements = doc.getDocumentElement().getElementsByTagName("workflow");
            for (int i = 0; i < workflowElements.getLength(); i++) {
                Element workflowElement = (Element) workflowElements.item(i);
                result.add(new WorkflowSynopsis(workflowElement.getAttribute("resource"), workflowElement.getAttribute("uri"), workflowElement.getTextContent()));
            }
        } catch (Exception e) {
            LogService.getRoot().log(Level.WARNING, "Error retrieving workflows from MyExperiment: " + e, e);
        }
        return result;
    }

    /** Note: This method may requires downloading the workflow data, so it is may be slow. */
    public WorkflowDetails getWorkflowDetails(WorkflowSynopsis ws) throws IOException {
        if (ws.getDetails() != null) {
            return ws.getDetails();
        } else {
            HttpURLConnection con = (HttpURLConnection) new URL(ws.getUri()).openConnection();
            authenticateIfLoggedIn(con);
            con.setDoInput(true);
            con.setDoOutput(false);
            Document response = getAsXML(con);
            WorkflowDetails details = new WorkflowDetails(response);
            ws.setDetails(details);
            return details;
        }
    }

    public void uploadWorkflow(String message, ProgressListener listener) throws ParserConfigurationException, IOException, XMLException {
        HttpURLConnection connection = connectTo("workflow.xml", null);
        connection.setRequestProperty("Content-Type", "application/vnd.rapidminer.rmp+xml");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        OutputStream out;
        try {
            out = connection.getOutputStream();
        } catch (IOException e) {
            throw new IOException(connection.getResponseCode() + ": " + connection.getResponseMessage(), e);
        }
        try {
            byte[] bytes = message.getBytes(ENCODING);
            OutputStream pout = new ProgressReportingOutputStream(out, listener, 20, 100, bytes.length);
            pout.write(bytes);
            pout.flush();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
        Document response = getAsXML(connection);
        LogService.getRoot().info("MyExperiment upload response: " + XMLTools.toString(response, ENCODING));
    }

    private List<License> downloadLicenses() throws IOException {
        List<License> result = new LinkedList<License>();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("elements", "title,unique-name");
        Document response = getAsXML("licenses.xml", arguments);
        NodeList licenseElements = response.getDocumentElement().getElementsByTagName("license");
        for (int i = 0; i < licenseElements.getLength(); i++) {
            result.add(new License((Element) licenseElements.item(i)));
        }
        return result;
    }

    public synchronized List<License> getLicenses() {
        initialize();
        return allLicenses;
    }

    /** Returns true if licenses are downloaded already. */
    public boolean isInitialized() {
        return allLicenses != null;
    }

    public synchronized void initialize() {
        if (this.allLicenses == null) {
            try {
                this.allLicenses = downloadLicenses();
            } catch (IOException e) {
                LogService.getRoot().log(Level.WARNING, "Error downloading licenses: " + e, e);
                this.allLicenses = new LinkedList<License>();
            }
        }
    }

    public String open(WorkflowSynopsis workflow, ProgressListener listener) throws IOException, XMLException {
        URL contentURL = getWorkflowDetails(workflow).getContentURL();
        LogService.getRoot().info("Downloading workflow from " + contentURL);
        HttpURLConnection con = (HttpURLConnection) contentURL.openConnection();
        authenticateIfLoggedIn(con);
        con.setDoInput(true);
        con.setDoOutput(false);
        InputStream in;
        try {
            in = con.getInputStream();
        } catch (IOException e) {
            throw new IOException(con.getResponseCode() + ": " + con.getResponseMessage(), e);
        }
        String lengthStr = con.getHeaderField("Content-Length");
        if (lengthStr != null) {
            try {
                in = new ProgressReportingInputStream(in, listener, 0, 100, Long.parseLong(lengthStr));
            } catch (NumberFormatException e) {
                LogService.getRoot().warning("myExperiment server delivered illegal content length: " + lengthStr);
            }
        }
        try {
            ZipInputStream zin = new ZipInputStream(in);
            LogService.getRoot().info("Opened zip stream");
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if ("process.xml".equals(entry.getName())) {
                    String processXML = Tools.readTextFile(zin);
                    return processXML;
                }
            }
            throw new IOException("Malformed contents from myExperiment server. Zip file does not contain process.xml. Either this is not a RapidMiner workflow, or you do not have the permissions to open this file.");
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    private void authenticateIfLoggedIn(HttpURLConnection con) {
        if (authentication != null) {
            String userPass = authentication.getUserName() + ":" + new String(authentication.getPassword());
            con.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(userPass.getBytes()));
        }
    }

    public static String getBaseUrl() {
        String url = System.getProperty(PARAMETER_MYEXPERIMENT_URL);
        if (url != null) {
            return url;
        } else {
            return DEFAULT_URL;
        }
    }

    public void login() {
        authentication = PasswordDialog.getPasswordAuthentication(getBaseUrl().toString(), false);
    }

    public void logout() {
        authentication = null;
    }

    public boolean isLoggedIn() {
        return authentication != null;
    }
}
