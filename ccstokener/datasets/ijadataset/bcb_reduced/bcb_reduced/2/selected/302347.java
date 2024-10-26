package org.ist_spice.mdcs.servlet;

import java.io.IOException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.ist_spice.mdcs.interfaces.RDSBean_IF;

/**
 * Servlet implementation class for Servlet: PresentationControlServlet
 * 
 */
public class PresentationControllerServlet extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    static final long serialVersionUID = 1L;

    private String DMD_NOTIFICATION_ENGINE = "http://159.217.144.83:8081/DynamicMobileDesktop/NotificationEngine";

    private String ANNOTATOR = "http://192.168.1.101:8008";

    private String TEST_USER = "stefan.meissner";

    private String SERVLET_URL = "http://192.168.1.103:8080/PresentationController/PresentationControllerServlet";

    private String IMAGE_URL = null;

    private String IMAGE_NAME = "poster.jpg";

    private String currentXPath = null;

    private String title = null;

    private String summary = null;

    private XmlRpcClient client;

    private boolean running = true;

    private boolean paused = false;

    private void getCurrentXPath() {
        try {
            currentXPath = client.execute("get_xpath_current", new Object[0]).toString();
        } catch (XmlRpcException e) {
            log("current XPath could not be executed properly");
            e.printStackTrace();
        }
    }

    private void getSummary() {
        if (currentXPath != null) {
            Object[] param = { currentXPath };
            try {
                summary = client.execute("get_summary_for_node", param).toString();
            } catch (XmlRpcException e) {
                log("current Summary could not be executed properly");
                e.printStackTrace();
            }
        }
    }

    private void getTitle() {
        if (currentXPath != null) {
            Object[] param = { currentXPath };
            try {
                title = client.execute("get_title_for_node", param).toString();
            } catch (XmlRpcException e) {
                log("current Title could not be executed properly");
                e.printStackTrace();
            }
        }
    }

    private String getSMILfile() {
        String filename = "C:\\tmp\\ITEAdoc.smil";
        String smil = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            StringBuilder builder = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            smil = builder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return smil;
    }

    private void getPoster() {
        if (currentXPath != null) {
            Object[] param = { currentXPath };
            try {
                byte[] poster = (byte[]) client.execute("get_poster_for_node", param);
                ServletContext ctx = this.getServletContext();
                File tempdir = (File) ctx.getAttribute("javax.servlet.context.tempdir");
                String filename = tempdir + "\\" + IMAGE_NAME;
                FileOutputStream fos = new FileOutputStream(filename);
                fos.write(poster);
                fos.close();
            } catch (XmlRpcException e) {
                log("current Title could not be executed properly");
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                log("file: " + IMAGE_NAME + " not found");
                e.printStackTrace();
            } catch (IOException e) {
                log("could not write poster");
                e.printStackTrace();
            }
        }
    }

    public void initializeServlet(String annotator, String user) {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        try {
            config.setServerURL(new URL(annotator));
            client = new XmlRpcClient();
            client.setConfig(config);
        } catch (MalformedURLException e) {
            log(annotator + " is not a valid address");
        }
    }

    public PresentationControllerServlet() {
        super();
    }

    public static String postRequest(String url, String content) throws IOException {
        InputStream is = null;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        String result = null;
        try {
            Object obj = openConnection(url, content, "POST", "text/xml");
            if (obj instanceof InputStream) {
                is = (InputStream) obj;
            } else {
                return "Cannot open a connection with " + url + " : " + obj.toString();
            }
            int c = is.read();
            while (c != -1) {
                buf.write(c);
                c = is.read();
            }
            result = new String(buf.toByteArray());
        } finally {
            if (is != null) {
                is.close();
            }
            if (buf != null) {
                buf.close();
            }
        }
        return result;
    }

    private void updateWidget(String user) throws UpdateException {
        if (running) {
            getCurrentXPath();
            getTitle();
            getSummary();
            getPoster();
            IMAGE_URL = SERVLET_URL;
        } else {
            currentXPath = null;
            title = "stopped";
            summary = "You can start the presentation again by pressing the play button.";
            IMAGE_URL = "";
        }
        log("update widget: title: " + title + " summary: " + summary + " user: " + user);
        String params = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<notify user=\"" + user + "\" suffix=\"/UpdateTitle\">" + "<presentation title=\"" + title + "\"" + " summary=\"" + summary + "\"" + " imgurl=\"" + IMAGE_URL + "\"/>" + "</notify>";
        log("update Widget: " + params);
        try {
            postRequest(DMD_NOTIFICATION_ENGINE, params);
        } catch (IOException e) {
            throw new UpdateException("Error during Updating the Widget");
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletContext sc = getServletContext();
        String tempdir = sc.getAttribute("javax.servlet.context.tempdir").toString();
        String filename = tempdir + "\\" + IMAGE_NAME;
        String mimeType = sc.getMimeType(filename);
        if (mimeType == null) {
            sc.log("Could not get MIME type of " + filename);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        resp.setContentType(mimeType);
        File file = new File(filename);
        resp.setContentLength((int) file.length());
        FileInputStream in = new FileInputStream(file);
        OutputStream out = resp.getOutputStream();
        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = in.read(buf)) >= 0) {
            out.write(buf, 0, count);
        }
        in.close();
        out.close();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        String name = request.getParameter("name");
        log("[doPost] action=" + action);
        if (name == null) name = ANNOTATOR;
        initializeServlet(name, TEST_USER);
        String errorMessage = "general error";
        Object result = "";
        if (action.equals("openfile")) {
            log("[doPost] openfile called");
            String file = getSMILfile();
            file.trim();
            log("[doPost after calling getSMILfile] SMIL file: " + file);
            Object[] param = { file };
            try {
                result = client.execute("openfile", param);
                log("[doPost] Result : " + result.toString());
                sendOKResponse("Presentation Controller", result != null ? result.toString() : "", response);
            } catch (XmlRpcException e) {
                log("[doPost] problem with XML-RPC call!");
                sendErrorResponse("Presentation Controller", "problem with annotator's open file", response);
                e.printStackTrace();
            }
        } else if (action.equals("bringme")) {
            try {
                InitialContext ctx = new InitialContext();
                RDSBean_IF rds = (RDSBean_IF) ctx.lookup("MDCS_EAR/RDSBean/local");
                rds.onSessionTransfer("http://ontology.ist.spice.org/mobile-ontology/0/10/dcs/0/mobilephone.owl#ConsuelosMobilePhone");
            } catch (NamingException e) {
                e.printStackTrace();
            }
            log("[doPost] session should be transferred to the mobile");
            sendOKResponse("Presentation Controller", result != null ? result.toString() : "", response);
        } else if (action.equals("refresh")) {
            if (running) {
                log("[doPost] widget should be refreshed");
                try {
                    updateWidget(TEST_USER);
                    sendOKResponse("Presentation Controller", result != null ? result.toString() : "", response);
                } catch (UpdateException e) {
                    errorMessage = "update widget failed";
                    log("[doPost] " + errorMessage + "!");
                    sendErrorResponse("Presentation Controller", errorMessage, response);
                }
            } else {
                errorMessage = "widget can't be refreshed --> no running presentation";
                log("[doPost] " + errorMessage + "!");
                sendErrorResponse("Presentation Controller", errorMessage, response);
            }
        } else {
            if (running) {
                if (paused) {
                    if (action.equals("pause")) {
                        log("[doPost] continue playing");
                        action = "play";
                        paused = false;
                    }
                    if (action.equals("goto_next_scene") || action.equals("goto_prev_scene")) {
                        errorMessage = "presentation must not paused!";
                        action = "no action";
                        log("[doPost] " + errorMessage);
                    }
                } else {
                    if (action.equals("play")) {
                        errorMessage = "presentation is already playing";
                        action = "no action";
                    }
                    if (action.equals("pause")) {
                        log("[doPost] pause presentation");
                        paused = true;
                    }
                }
                if (action.equals("stop")) {
                    log("[doPost] stop presentation");
                    running = false;
                    paused = false;
                }
            } else {
                if (action.equals("play")) {
                    log("[doPost] start playing");
                    running = true;
                    paused = false;
                } else {
                    if (action.equals("stop")) {
                        errorMessage = "presentation is already stopped";
                    } else {
                        errorMessage = "presentation is stopped";
                    }
                    log("[doPost] " + errorMessage);
                    action = "no action";
                }
            }
            if (action.equals("no action")) {
                log("[doPost] no action required, see error message!");
                sendErrorResponse("Presentation Controller", errorMessage, response);
            } else {
                try {
                    result = client.execute(action, new Object[0]);
                    updateWidget(TEST_USER);
                    sendOKResponse("Presentation Controller", result != null ? result.toString() : "", response);
                } catch (XmlRpcException e) {
                    log("[doPost] problem with XML-RPC call!");
                    sendErrorResponse("Presentation Controller", "annotator unavailable", response);
                } catch (UpdateException e) {
                    log("[doPost] problem with updating the widget!");
                    sendErrorResponse("Presentation Controller", "update widget failed", response);
                }
            }
        }
    }

    /**
	 * Open connection to a URL and return InputStream instance if there is no
	 * problem.
	 */
    public static Object openConnection(String connection_url, String content, String method, String contentType) throws IOException {
        URL url = new URL(connection_url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        if (method != null) {
            connection.setRequestMethod(method);
        }
        if (contentType != null) {
            connection.setRequestProperty("Content-Type", contentType);
        }
        if (content != null) {
            connection.setRequestProperty("Content-Length", "" + content.length());
            OutputStream out = null;
            try {
                out = connection.getOutputStream();
                out.write(content.getBytes());
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return connection.getInputStream();
        }
        return connection.getResponseCode() + ":" + connection.getResponseMessage();
    }

    /**
	 * Send a response (with OutputStream reference to avoid exception due to
	 * multiple access to getOutputStream).
	 */
    public void sendResponse(String name, String status, String content, HttpServletResponse response, OutputStream out) throws IOException {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffer.append("<response request_name=\"" + name + "\" status=\"" + status + "\">");
        if (content != null) {
            buffer.append(content);
        }
        buffer.append("</response>");
        response.setContentType("text/xml");
        response.setContentLength(buffer.length());
        out.write(buffer.toString().getBytes());
        out.flush();
        out.close();
    }

    /** Send an error message. */
    public void sendErrorResponse(String name, String msg, HttpServletResponse response) throws IOException {
        sendResponse(name, "error", "<error>" + msg + "</error>", response, response.getOutputStream());
    }

    /** Send the XML content. */
    public void sendOKResponse(String name, String content, HttpServletResponse response) throws IOException {
        sendResponse(name, "OK", content, response, response.getOutputStream());
    }

    private class UpdateException extends Exception {

        /**
		 * 
		 */
        private static final long serialVersionUID = -8189026996654578039L;

        private UpdateException() {
        }

        private UpdateException(String s) {
            super(s);
        }
    }
}
