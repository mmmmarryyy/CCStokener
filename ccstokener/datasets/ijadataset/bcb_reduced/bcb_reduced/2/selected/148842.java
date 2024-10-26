package com.ingenta.workbench.servlet;

import com.ingenta.workbench.WorkbenchException;
import com.ingenta.workbench.WorkbenchUserError;
import com.ingenta.workbench.data.*;
import com.ingenta.workbench.xml.*;
import com.ingenta.workbench.util.*;
import com.ingenta.clownbike.ClownbikeError;
import com.ingenta.clownbike.servlet.ClownbikeServlet;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.w3c.tidy.Tidy;
import org.xml.sax.SAXException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.StringBufferInputStream;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

public class WorkbenchServlet extends ClownbikeServlet {

    private ArrayList _datatypes;

    public void init() {
        DatatypeHandler handler = new DatatypeHandler();
        try {
            WorkbenchUtil.parseXml(handler, new InputSource(getServletContext().getInitParameter("dataLocation") + "/" + getServletContext().getInitParameter("objectsFile")));
            _datatypes = handler.getDatatypes();
        } catch (WorkbenchException e) {
            getLogger().log(Level.SEVERE, "Workbench servlet not initialized: (" + getServletContext().getInitParameter("dataLocation") + "/" + getServletContext().getInitParameter("objectsFile") + "): ");
            e.printStackTrace();
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html; charset=utf-8");
        try {
            run(req, resp);
        } catch (WorkbenchUserError e) {
            req.setAttribute("msg", e.getMessage());
            e.printStackTrace();
            forward(req, resp, "error");
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            req.setAttribute("msg", e.getMessage());
            e.printStackTrace();
            forward(req, resp, "error");
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html; charset=utf-8");
        try {
            run(req, resp);
        } catch (WorkbenchUserError e) {
            req.setAttribute("msg", e.getMessage());
            forward(req, resp, "error");
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            req.setAttribute("msg", e.getMessage());
            e.printStackTrace();
            forward(req, resp, "error");
        }
    }

    protected void run(HttpServletRequest req, HttpServletResponse resp) throws WorkbenchException, IOException, ServletException {
        String service = req.getParameter("workbench:service");
        String[] deletions = req.getParameterValues("workbench:delete");
        Enumeration names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if (name.startsWith("workbench:query")) {
                String id = extractId(name);
                doQuery(req.getParameter("workbench:input-" + id), req.getParameter("workbench:service-" + id), req, resp);
                return;
            }
        }
        if (!isEmpty(service)) {
            doQuery(req, resp);
        } else if (deletions != null && deletions.length > 0) {
            doDeleteRequest(deletions, req, resp);
        } else {
            doDisplayState(req, resp);
        }
    }

    protected void doDisplayState(HttpServletRequest req, HttpServletResponse resp) throws WorkbenchException, IOException, ServletException {
        SessionState sessionState = getSessionState(req);
        printSessionState(sessionState, req, resp);
    }

    protected void doQuery(HttpServletRequest req, HttpServletResponse resp) throws WorkbenchException, IOException, ServletException {
        doQuery(req.getParameter("workbench:input"), req.getParameter("workbench:service"), req, resp);
    }

    protected void doQuery(String input, String serviceId, HttpServletRequest req, HttpServletResponse resp) throws WorkbenchException, IOException, ServletException {
        SessionState sessionState = getSessionState(req);
        Service service = sessionState.getService(serviceId);
        HashMap params = new HashMap();
        String templateLocation = getServletContext().getInitParameter("templateLocation");
        InputStream is = null;
        if (service instanceof SimpleService) {
            SimpleService sservice = (SimpleService) service;
            if (isEmpty(sservice.getInputName())) throw new WorkbenchException("Misconfigured service: " + sservice.getName());
            String param = sservice.getInputName();
            params.put(param, input);
            if (isEmpty(input)) throw new WorkbenchException("Missing parameter " + param + " for service " + service.getName());
            is = post(sservice.getUrl(), params);
        } else {
            ComplexService cservice = (ComplexService) service;
            params = buildParamsForService(cservice, req);
            is = post(cservice.getUrl(), params);
        }
        Document resultsDoc = null;
        if (service.getConverter() != null) {
            Tidy tidy = new Tidy();
            tidy.setXmlOut(true);
            resultsDoc = tidy.parseDOM(is, null);
            Writer writer = new StringWriter();
            File xslFile = new File(templateLocation + "/" + service.getConverter());
            transformXml(xslFile, resultsDoc, writer, params);
            resultsDoc = xmlToDoc(writer.toString());
        } else {
            StringBuffer xml = new StringBuffer(16);
            InputStreamReader stream = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(stream);
            String line;
            while ((line = br.readLine()) != null) {
                xml.append(line);
                xml.append('\n');
            }
            br.close();
            stream.close();
            resultsDoc = xmlToDoc(xml.toString());
            System.out.println("\n\nRaw query results");
            printDOMToLog(resultsDoc);
        }
        is.close();
        if (service.getUntidyConverter() != null) {
            Writer writer = new StringWriter();
            File xslFile = new File(templateLocation + "/" + service.getUntidyConverter());
            System.out.println("Untidy Converter sheet: " + xslFile.toString());
            transformXml(xslFile, resultsDoc, writer, params);
            System.out.println("\n\nUntidy string result" + writer.toString());
            resultsDoc = xmlToDoc(writer.toString());
            System.out.println("\n\nUntidyConversion results");
            printDOMToLog(resultsDoc);
        }
        Node resultTree = sessionState.addResults(resultsDoc, service, params);
        doDisplayState(req, resp);
    }

    protected void doDeleteRequest(String[] deletions, HttpServletRequest req, HttpServletResponse resp) throws WorkbenchException, IOException, ServletException {
        for (int i = 0; i < deletions.length; i++) {
            String input = deletions[i];
            try {
                input = URLDecoder.decode(input, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
            }
            String divider = "-";
            int pos = input.indexOf(divider);
            if (pos == -1) throw new WorkbenchException("Unable to determine location of result to delete: " + input);
            String resultTreeId = input.substring(0, pos);
            String positionStr = input.substring(pos + 1);
            if (isEmpty(resultTreeId)) throw new WorkbenchException("Result tree ID must be specified in order to perform delete function.");
            if (isEmpty(positionStr)) throw new WorkbenchException("Result number must be specified in order to perform delete function.");
            int position = Integer.parseInt(positionStr);
            SessionState sessionState = getSessionState(req);
            Node resultTree = sessionState.getResultTree(resultTreeId);
            if (resultTree == null) throw new WorkbenchException("No result tree with ID of " + resultTreeId);
            NodeList children = resultTree.getChildNodes();
            int currentPosition = 0;
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (position == currentPosition) {
                        Attr deleted = sessionState.getDOM().createAttribute("deleted");
                        deleted.setValue("deleted");
                        ((Element) child).setAttributeNode(deleted);
                    }
                    currentPosition++;
                }
            }
        }
        doDisplayState(req, resp);
    }

    protected SessionState getSessionState(HttpServletRequest req) throws WorkbenchException {
        HttpSession session = req.getSession();
        SessionState sessionState = (SessionState) session.getAttribute("sessionState");
        if (sessionState == null) {
            synchronized (session) {
                sessionState = (SessionState) session.getAttribute("sessionState");
                if (sessionState == null) {
                    sessionState = new SessionState();
                    session.setAttribute("sessionState", sessionState);
                    addServices(sessionState);
                }
            }
        }
        return sessionState;
    }

    protected void printDOMToLog(Document dom) throws IOException, WorkbenchException {
        StringWriter writer = new StringWriter();
        HashMap params = new HashMap();
        transformXml(getCopyTemplate(), dom, writer, params);
        System.out.println(writer.toString());
    }

    protected Document getTestResultsTree() throws WorkbenchException {
        DocumentBuilder builder = WorkbenchUtil.newDocumentBuilder();
        Document resultsDoc = null;
        try {
            resultsDoc = builder.parse("/home/jphekman/src/workbench/data/article-test.xml");
        } catch (SAXException e) {
            e.printStackTrace();
            throw new WorkbenchException("Unable to parse test results file: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new WorkbenchException("Unable to open test results file: " + e.getMessage());
        }
        return resultsDoc;
    }

    protected void printSessionState(SessionState sessionState, HttpServletRequest req, HttpServletResponse resp) throws IOException, WorkbenchException {
        HashMap params = new HashMap();
        String open = req.getParameter("open");
        if (!isEmpty(open)) {
            params.put("open", open);
        }
        transformXml(getDefaultTemplate(), sessionState.getDOM(), resp.getWriter(), params);
    }

    protected Document xmlToDoc(String xml) throws WorkbenchException {
        StringReader reader = new StringReader(xml);
        DocumentBuilder builder = WorkbenchUtil.newDocumentBuilder();
        Document doc = null;
        try {
            doc = builder.parse(new InputSource(reader));
        } catch (SAXException e) {
            e.printStackTrace();
            throw new WorkbenchException("Unable to parse xml: " + e.getMessage() + "***" + xml);
        } catch (IOException e) {
            e.printStackTrace();
            throw new WorkbenchException("Unable to read xml: " + e.getMessage() + "***" + xml);
        } finally {
            reader.close();
        }
        return doc;
    }

    protected Document parseStream(InputStream is) throws WorkbenchException {
        InputSource foo = new InputSource(is);
        DocumentBuilder builder = WorkbenchUtil.newDocumentBuilder();
        Document doc = null;
        try {
            doc = builder.parse(is);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new WorkbenchException("Unable to parse xml: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new WorkbenchException("Unable to read xml: " + e.getMessage());
        }
        return doc;
    }

    protected File getDefaultTemplate() {
        return new File(getServletContext().getInitParameter("templateLocation") + "/" + getServletContext().getInitParameter("defaultTemplate"));
    }

    protected File getCopyTemplate() {
        return new File(getServletContext().getInitParameter("templateLocation") + "/" + getServletContext().getInitParameter("copyTemplate"));
    }

    protected File getResultsTemplate() {
        return new File(getServletContext().getInitParameter("templateLocation") + "/" + getServletContext().getInitParameter("resultsTemplate"));
    }

    protected void transformXml(File xslFile, Document doc, Writer output, HashMap params) throws WorkbenchException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Templates template = factory.newTemplates(new StreamSource(new FileInputStream(xslFile)));
            Transformer xformer = template.newTransformer();
            if (params == null) params = new HashMap();
            Iterator iter = params.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                xformer.setParameter(key, params.get(key));
            }
            xformer.transform(new DOMSource(doc), new StreamResult(output));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new WorkbenchException("Unable to find template: " + xslFile.toString());
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            throw new WorkbenchException("Unable to load transformer parser: " + e.getMessage());
        } catch (TransformerException e) {
            SourceLocator locator = e.getLocator();
            int col = locator.getColumnNumber();
            int line = locator.getLineNumber();
            String publicId = locator.getPublicId();
            String systemId = locator.getSystemId();
            e.printStackTrace();
            throw new WorkbenchException("Unable to parse default template: col " + col + ", line " + line);
        }
    }

    protected void addServices(SessionState sessionState) throws WorkbenchException {
        DocumentBuilder builder = WorkbenchUtil.newDocumentBuilder();
        Document directory = null;
        try {
            directory = builder.parse(getServletContext().getInitParameter("dataLocation") + "/" + getServletContext().getInitParameter("directoryFile"));
        } catch (SAXException e) {
            e.printStackTrace();
            throw new WorkbenchException("Unable to parse directory file: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new WorkbenchException("Unable to open directory file: " + e.getMessage());
        }
        Document sessionDOM = sessionState.getDOM();
        NodeList services = directory.getDocumentElement().getChildNodes();
        for (int i = 0; i < services.getLength(); i++) {
            sessionState.getServices().appendChild(sessionDOM.importNode(services.item(i), true));
        }
    }

    protected Document addServicesFromSessionState(Node node, SessionState sessionState) throws WorkbenchException {
        DocumentBuilder builder = WorkbenchUtil.newDocumentBuilder();
        Document newDoc = builder.newDocument();
        Element root = newDoc.createElement("workbench:results_and_services");
        newDoc.appendChild(root);
        Node services = sessionState.getServices().cloneNode(true);
        services = newDoc.importNode(services, true);
        root.appendChild(services);
        Node results = node.cloneNode(true);
        results = newDoc.importNode(results, true);
        root.appendChild(results);
        return newDoc;
    }

    protected HashMap buildParamsForService(ComplexService service, HttpServletRequest req) {
        HashMap params = service.getParams();
        Iterator iter = params.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            ServiceParameter param = (ServiceParameter) params.get(name);
            param.setValue(req.getParameter(name));
        }
        return params;
    }

    protected InputStream post(String loc, HashMap params) throws IOException {
        Iterator iter = params.keySet().iterator();
        StringBuffer data = new StringBuffer();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String value;
            if (params.get(key) instanceof String) value = (String) params.get(key); else value = ((ServiceParameter) params.get(key)).getValue();
            data.append(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8"));
            if (iter.hasNext()) data.append("&");
        }
        URL url = new URL(loc + "?" + data.toString());
        System.out.println("QUerying service: " + url.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setFollowRedirects(true);
        conn.connect();
        int code = conn.getResponseCode();
        if (code >= 200 && code <= 204) {
            return conn.getInputStream();
        } else {
            return new StringBufferInputStream("<workbench:object xmlns:workbench='http://www.oriel.org/workbench'><workbench:empty-result/></workbench:object>");
        }
    }

    public static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    protected String extractId(String input) throws WorkbenchException {
        int pos = input.indexOf("-");
        if (pos == -1) throw new WorkbenchException("Received poorly formatted string for id extraction: " + input);
        return (input.substring(pos + 1));
    }

    protected Logger getLogger() {
        ServletContext context = getServletContext();
        Logger logger = (Logger) context.getAttribute("logger");
        if (logger == null) {
            synchronized (context) {
                logger = (Logger) context.getAttribute("logger");
                if (logger == null) {
                    logger = Logger.getLogger("com.ingenta.workbench");
                    context.setAttribute("logger", logger);
                }
            }
        }
        return logger;
    }
}
