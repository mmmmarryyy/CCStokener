package linkFinder;

import gui.linkFinder.GetLinksEvent;
import gui.linkFinder.GetLinksEventListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javax.swing.event.EventListenerList;
import linkFinder.js.JSFunction;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.EncodingChangeException;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

public class FindHTMLTag implements Runnable {

    private static final long serialVersionUID = 1L;

    protected EventListenerList listenerList = new EventListenerList();

    protected List<String> links = new ArrayList<String>();

    protected Map<String, JSFunction> jsFunctions = new TreeMap<String, JSFunction>();

    protected String path = "";

    protected Set<String> set = new HashSet<String>();

    protected boolean finished = false;

    public FindHTMLTag(String path) {
        this.path = path;
    }

    /**
	 * 
	 * @param listener
	 */
    public void addGetLinksEventListener(GetLinksEventListener listener) {
        listenerList.add(GetLinksEventListener.class, listener);
    }

    /**
	 * 
	 * @param listener
	 */
    public void removeGetLinksEventListener(GetLinksEventListener listener) {
        listenerList.remove(GetLinksEventListener.class, listener);
    }

    protected void fireGetLinksEvent(GetLinksEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == GetLinksEventListener.class) {
                ((GetLinksEventListener) listeners[i + 1]).endParsingForLinks(evt);
            }
        }
    }

    /**
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        try {
            URL url = new URL(this.path);
            this.path = url.getProtocol() + "://" + url.getHost() + url.getPath().substring(0, (url.getPath().lastIndexOf('/') != -1 ? url.getPath().lastIndexOf('/') : url.getPath().length()));
            URLConnection con = url.openConnection();
            Parser parser = new Parser(con);
            readJavaScript(parser, url);
            updateJSFunctions();
            con = url.openConnection();
            parser = new Parser(con);
            NodeFilter[] nf = new NodeFilter[2];
            nf[0] = new TagNameFilter("a");
            nf[1] = new HasAttributeFilter("onclick");
            NodeList nlist = parser.extractAllNodesThatMatch(new OrFilter(nf));
            SimpleNodeIterator i = nlist.elements();
            while (i.hasMoreNodes()) {
                Node node = i.nextNode();
                if (node instanceof Tag) {
                    Tag tag = (Tag) node;
                    String val = tag.getAttribute("onclick");
                    if (FindHTMLTag.isNullEmptyString(val)) {
                        val = tag.getAttribute("href");
                        if (!FindHTMLTag.isNullEmptyString(val)) {
                            jsFuncAction(val);
                        }
                    } else {
                        jsFuncAction(val);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        GetLinksEvent evt = new GetLinksEvent(this.getLinks());
        this.fireGetLinksEvent(evt);
    }

    protected void makeFile(URL url) {
        double num = Math.random();
        String fileName = String.valueOf(num);
        if (fileName.length() > 10) {
            fileName = fileName.substring(0, 10);
        }
        Iterator<String> it = this.getLinks().iterator();
        try {
            String header = "<html><head><title>" + url.getFile() + "</title></head><body>\n";
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("work/" + fileName + ".html")));
            out.print(header);
            while (it.hasNext()) {
                String link = it.next();
                if (!FindHTMLTag.isNullEmptyString(link)) {
                    out.print("<a href='" + link + "' target='_blank'>" + link + "</a><br>\n");
                }
            }
            String footer = "</body></html>";
            out.print(footer);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void jsFuncAction(String val) {
        if (val != null && val.toLowerCase().indexOf("javascript:") != -1) {
            val = val.replaceAll("javascript:", "");
            int open_ind = val.indexOf(JSFunction.OPEN);
            if (open_ind != -1) {
                String func_name = val.substring(0, open_ind).trim();
                if (jsFunctions.containsKey(func_name)) {
                    convertToLink(val);
                }
            }
        } else if (val != null && val.indexOf(JSFunction.OPEN) != -1 && val.indexOf(JSFunction.CLOSE) != -1) {
            convertToLink(val);
        } else if (val != null) {
            if (!val.toLowerCase().startsWith("http://") && !val.toLowerCase().startsWith("www.")) {
                val = getFullString(val);
            }
            links.add(val);
        }
    }

    protected void updateJSFunctions() {
        Iterator<String> i = jsFunctions.keySet().iterator();
        while (i.hasNext()) {
            jsFunctions.get(i.next()).setFunctionList(jsFunctions);
        }
    }

    protected void convertToLink(String val) {
        if (!FindHTMLTag.isNullEmptyString(val)) {
            String jsLink = val.replaceAll(";", "").trim();
            if (jsLink.indexOf(JSFunction.OPEN) > 0 && jsLink.endsWith(String.valueOf(JSFunction.CLOSE))) {
                String jsName = jsLink.substring(0, jsLink.indexOf(JSFunction.OPEN)).trim();
                if (!set.contains(jsName) && jsFunctions.containsKey(jsName)) {
                    set.add(jsName);
                    String parameters = jsLink.substring(jsLink.indexOf(JSFunction.OPEN) + 1, jsLink.lastIndexOf(JSFunction.CLOSE)).trim();
                    StringTokenizer tokens = new StringTokenizer(parameters, ",");
                    int i = 0;
                    JSFunction js = jsFunctions.get(jsName);
                    while (tokens.hasMoreTokens()) {
                        js.setParameterValue(i, tokens.nextToken());
                        i++;
                    }
                    if (js != null && js.getPrametersSize() == i) {
                        LinkedList<String> lst = js.parseFunction();
                        if (lst != null && !lst.isEmpty()) {
                            Iterator<String> it = lst.iterator();
                            while (it.hasNext()) {
                                String link = it.next();
                                link = getFullString(link);
                                links.add(link);
                            }
                        }
                    }
                }
            }
        }
    }

    protected String clean(String value) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != JSFunction.PLUS && !" ".equals(String.valueOf(value.charAt(i)))) {
                sb.append(value.charAt(i));
            }
        }
        return (sb.length() > 0 ? sb.toString() : value);
    }

    protected String getFullString(String value) {
        value = clean(value);
        if (!value.toLowerCase().startsWith("http://") && !value.toLowerCase().startsWith("www.")) {
            if (value.startsWith("/")) {
                value = value.substring(1);
            }
            if (value.startsWith("../")) {
                int last = this.path.lastIndexOf("/");
                if (last != -1) {
                    value = this.path.substring(0, last) + value.substring(2);
                } else {
                    value = this.path + "/" + value;
                }
            } else {
                value = this.path + "/" + value;
            }
        }
        return value;
    }

    protected void readJavaScript(Parser parser, URL url) throws ParserException {
        try {
            NodeFilter[] nf = { new TagNameFilter("script"), new TagNameFilter("SCRIPT") };
            NodeList nlist = parser.extractAllNodesThatMatch(new OrFilter(nf));
            int length = nlist.size();
            for (int i = 0; i < length; i++) {
                Node node = nlist.elementAt(i);
                if (node instanceof Tag) {
                    Tag tag = (Tag) node;
                    if ("script".equals(tag.getTagName().toLowerCase())) {
                        String lin = "";
                        String external_js_path = "";
                        String script_text = "";
                        if ((lin = tag.getAttribute("src")) != null) {
                            if (lin.toLowerCase().startsWith("http") || lin.toLowerCase().startsWith("www")) {
                                external_js_path = lin;
                            } else {
                                String host = url.getHost();
                                int last_ind = lin.lastIndexOf("../");
                                String n_url = "";
                                if (last_ind != -1) {
                                    n_url = lin.substring(last_ind + 2);
                                } else if (!lin.startsWith("/")) {
                                    n_url = "/" + lin;
                                }
                                external_js_path = "http://" + host + n_url;
                            }
                            if (!FindHTMLTag.isNullEmptyString(external_js_path)) {
                                try {
                                    URL js_url = new URL(external_js_path);
                                    URLConnection con = js_url.openConnection();
                                    con.connect();
                                    BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                                    String line = null;
                                    while ((line = br.readLine()) != null) {
                                        script_text += (line + "\n");
                                    }
                                } catch (Exception e) {
                                }
                            }
                        } else {
                            script_text = tag.toPlainTextString().trim();
                        }
                        if (!FindHTMLTag.isNullEmptyString(script_text)) {
                            getJSFunctionBody(script_text);
                        }
                    }
                }
            }
        } catch (EncodingChangeException ece) {
            ece.printStackTrace();
            System.out.println("\n" + this.path);
        } catch (org.htmlparser.util.ParserException pe) {
            pe.printStackTrace();
            System.out.println("\n" + this.path);
        }
    }

    /**
	 * Static method for checking if string null or empty
	 * @param str to be checked 
	 * @return <tt>true</tt> if string empty and false if else
	 */
    public static boolean isNullEmptyString(String str) {
        return !(str != null && !"".equals(str));
    }

    protected void getJSFunctionBody(String text) {
        text = removeComments(text);
        int text_index = 0;
        if (!FindHTMLTag.isNullEmptyString(text)) {
            int func_ind = -1;
            while ((func_ind = text.indexOf("function ", text_index)) != -1) {
                text_index = func_ind;
                int begin_substr = func_ind;
                LinkedList<String> lst = new LinkedList<String>();
                lst.add("function");
                do {
                    if (text.charAt(text_index) == '{') {
                        lst.add("{");
                    } else if (text.charAt(text_index) == '}') {
                        lst.removeLast();
                        if (lst.size() == 1) {
                            lst.removeLast();
                        }
                    }
                    text_index++;
                    func_ind++;
                } while (!lst.isEmpty() && text_index < text.length());
                try {
                    if (text != null && func_ind != -1) {
                        int end_point = (text_index > text.length() ? text.length() : text_index);
                        JSFunction jf = new JSFunction(text.substring(begin_substr, end_point).trim());
                        if (!FindHTMLTag.isNullEmptyString(jf.getFunctionName())) {
                            jsFunctions.put(jf.getFunctionName(), jf);
                        }
                    }
                } catch (StringIndexOutOfBoundsException siooob) {
                    siooob.printStackTrace();
                }
            }
        }
    }

    protected String removeComments(String text) {
        StringTokenizer tokens = new StringTokenizer(text, "\n");
        ;
        StringBuffer sb = new StringBuffer();
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (!token.trim().startsWith("//")) {
                sb.append(token + "\n");
            }
        }
        return sb.toString();
    }

    /**
	 * returns collection of links read from current file and its external js
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public Collection<String> getLinks() {
        Map<String, String> map = new TreeMap<String, String>(new LinkComparator());
        Iterator<String> i = links.iterator();
        while (i.hasNext()) {
            String link = i.next();
            map.put(link, link);
        }
        return map.values();
    }
}
