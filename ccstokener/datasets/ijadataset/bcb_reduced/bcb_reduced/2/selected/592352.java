package com.ibm.hamlet;

import java.io.*;
import java.net.*;
import javax.servlet.http.*;
import org.apache.log4j.*;
import org.xml.sax.*;

public abstract class Hamlet extends HttpServlet implements ContentHandler {

    private static Category category = Category.getInstance(Hamlet.class);

    private int port;

    private String sid, path, oldTemplate;

    private Template templateClass;

    public int getElementRepeatCount(String id, String name, Attributes atts) throws Exception {
        return 0;
    }

    public String getElementReplacement(String id, String name, Attributes atts) throws Exception {
        return "";
    }

    public Attributes getElementAttributes(String id, String name, Attributes atts) throws Exception {
        return atts;
    }

    public InputStream getElementIncludeSource(String id, String name, Attributes atts) throws Exception {
        URL url = getIncludeURL(atts.getValue("SRC"));
        return url.openStream();
    }

    public String getDocumentType() {
        return "text/html";
    }

    public URL getIncludeURL(String fileName) throws Exception {
        StringBuffer buf = new StringBuffer(path);
        buf.append("/");
        int pos = fileName.indexOf("?");
        if (pos != -1) {
            buf.append(fileName.substring(0, pos));
            buf.append(sid);
            buf.append(fileName.substring(pos));
        } else {
            buf.append(fileName);
            buf.append(sid);
        }
        category.debug("Include URL: " + buf.toString());
        return new URL("http", "localhost", port, buf.toString(), null);
    }

    public TemplateEngine getTemplateEngine() {
        return new DefaultEngine();
    }

    private void initialize(HttpServletRequest req) {
        sid = "";
        HttpSession session = req.getSession(false);
        if (session != null) sid = ";jsessionid=" + session.getId();
        port = req.getServerPort();
        path = req.getContextPath();
    }

    private void findTemplateClass(String template) throws Exception {
        if (!template.equals(oldTemplate)) {
            String className = RuntimeUtilities.getClassName(template);
            try {
                category.debug("Loading class '" + className + "' ...");
                Class c = Class.forName(className);
                templateClass = (Template) c.newInstance();
                category.debug("Class '" + className + "' loaded");
                System.out.println("Class '" + className + "' loaded");
            } catch (ClassNotFoundException e) {
                category.debug("Cannot load class '" + className + "'");
            }
            oldTemplate = template;
        }
    }

    private void serveDoc(PrintWriter out, String template, ContentHandler handler) throws Exception {
        findTemplateClass(template);
        if (templateClass != null) {
            templateClass.serveDoc(out, handler);
        } else {
            TemplateEngine engine = getTemplateEngine();
            InputStream in = getServletContext().getResourceAsStream(template);
            category.debug("Parsing '" + template + "' ...");
            long t1 = System.currentTimeMillis();
            engine.perform(in, handler, out);
            long t2 = System.currentTimeMillis();
            category.debug("Parsed '" + template + "' in " + (t2 - t1) + " ms.");
        }
    }

    public void serveDoc(HttpServletRequest req, HttpServletResponse res, String template, ContentHandler handler) throws Exception {
        initialize(req);
        PrintWriter out = res.getWriter();
        res.setContentType(getDocumentType());
        serveDoc(out, template, handler);
    }

    public void serveDoc(HttpServletRequest req, HttpServletResponse res, String template) throws Exception {
        serveDoc(req, res, template, this);
    }

    public void serveDoc(HttpServletRequest req, HttpServletResponse res, Class c, ContentHandler handler) throws Exception {
        initialize(req);
        PrintWriter out = res.getWriter();
        res.setContentType(getDocumentType());
        Template templateClass = (Template) c.newInstance();
        templateClass.serveDoc(out, handler);
    }
}
