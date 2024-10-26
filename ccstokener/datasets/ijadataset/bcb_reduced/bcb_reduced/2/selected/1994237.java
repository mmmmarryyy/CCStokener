package dcf.tasks;

import dcf.server.*;
import java.net.*;
import java.rmi.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.util.*;
import java.io.*;

/**
 * Implements a Solver for the <b>HTMLCrawlerTask.</b><br>
 * This solver is a part of a system to index PDF files.<br>
 * A different Task-Solver is later used to parse the PDF files.<br>
 * @author Tal Salmona
 */
public class HTMLCrawlerSolver implements Solver, Serializable {

    private Task task;

    private String[] suffix = new String[] { ".html", ".htm", ".jsp", ".asp", ".cfm", ".php" };

    private String[] leaveSuffix = new String[] { ".zip", ".arj", ".gz", ".tar", ".jpg", ".gif", ".bmp", ".mov", ".mpg", ".mpeg", ".exe" };

    private String toSearch = "";

    private Vector links = new Vector();

    public HTMLCrawlerSolver() {
    }

    public void setFor(Task t) {
        task = t;
    }

    public void solve(Task t) {
        int size = t.getSize();
        float step = 100f / size;
        float progress = 0;
        while (t.hasMore()) {
            Object o = t.next();
            if (o instanceof String) {
                Vector res = parse((String) o);
                setResult(res);
            }
            progress += step;
            ((HTMLCrawlerTask) task).setProgress(progress);
        }
    }

    public Task getTask() {
        return task;
    }

    public void setResult(Object obj) {
        task.addResult(obj);
    }

    public Vector parse(String link) {
        addMessage("Parsing: " + link);
        links.removeAllElements();
        URLConnection conn = null;
        Reader rd = null;
        EditorKit kit = new HTMLEditorKit();
        Document doc = kit.createDefaultDocument();
        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        URL url = null;
        try {
            url = new URL(link);
        } catch (MalformedURLException err) {
            System.out.println("Malformed URL");
            return links;
        }
        try {
            conn = new URL(link).openConnection();
            rd = new InputStreamReader(conn.getInputStream());
        } catch (Exception err) {
            err.printStackTrace();
            return links;
        }
        try {
            kit.read(rd, doc, 0);
            ElementIterator it = new ElementIterator(doc);
            javax.swing.text.Element elem;
            while ((elem = it.next()) != null) {
                SimpleAttributeSet s = (SimpleAttributeSet) elem.getAttributes().getAttribute(HTML.Tag.A);
                if (s != null) {
                    if (s.toString().indexOf("script") >= 0) continue;
                    String lnk = "";
                    try {
                        lnk = s.getAttribute(HTML.Attribute.HREF).toString();
                    } catch (Exception err) {
                        continue;
                    }
                    int j = 0;
                    if ((j = lnk.indexOf('#')) >= 0) lnk = lnk.substring(0, j);
                    URL urlLink = new URL(url, lnk);
                    if (!url.getHost().equals(urlLink.getHost())) continue;
                    String str = urlLink.toString();
                    if (!str.startsWith("http")) continue;
                    if (str.endsWith(".pdf")) {
                        continue;
                    }
                    for (int i = 0; i < leaveSuffix.length; i++) {
                        if ((str.endsWith(leaveSuffix[i]))) continue;
                    }
                    boolean skip = false;
                    for (int i = 0; i < suffix.length; i++) {
                        if ((str.endsWith(suffix[i]))) skip = true;
                    }
                    if (!skip) {
                        try {
                            conn = urlLink.openConnection();
                        } catch (Exception err) {
                        }
                        String contentType = null;
                        if (contentType == null) contentType = conn.getContentType();
                        if (contentType.equals("application/pdf")) {
                            continue;
                        } else if (!contentType.equals("text/html")) {
                            continue;
                        }
                    }
                    if (!links.contains(urlLink.toString())) {
                        links.addElement(urlLink.toString());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return links;
    }

    public void addMessage(String str) {
        ((HTMLCrawlerTask) task).addMessage(str);
    }
}
