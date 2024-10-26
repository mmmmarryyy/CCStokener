package data;

import java.net.*;
import java.io.*;
import java.util.StringTokenizer;
import javax.swing.text.*;
import javax.xml.parsers.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.swing.text.html.HTML;
import java.util.*;

/**
 * @author Sardi Carlo
 * 
 */
public class Modification implements Runnable {

    String l, webpage, tmp, oldstatus;

    configuration config;

    Data data;

    StringTokenizer parser;

    int i, j;

    private URL adminURL;

    private String url;

    private HTMLDocument docd;

    List<String> addlist;

    List<String> dellist;

    List<String> changelist;

    public Modification(String url) {
        this.url = url;
    }

    public void run() {
        try {
            adminURL = new URL(this.url);
            try {
                HttpURLConnection urlconn = (HttpURLConnection) adminURL.openConnection();
                urlconn.disconnect();
                urlconn.setRequestProperty("User-agent", configuration.getConfigurationObject().version);
                urlconn.connect();
                try {
                    InputStream is = urlconn.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    HTMLEditorKit htmlKit = new HTMLEditorKit();
                    docd = (HTMLDocument) htmlKit.createDefaultDocument();
                    HTMLEditorKit.Parser parser = new ParserDelegator();
                    HTMLEditorKit.ParserCallback callback = docd.getReader(0);
                    parser.parse(br, callback, true);
                    ElementIterator iterator = new ElementIterator(docd);
                    Element element;
                    boolean add = false;
                    boolean del = false;
                    addlist = new LinkedList<String>();
                    dellist = new LinkedList<String>();
                    changelist = new LinkedList<String>();
                    while ((element = iterator.next()) != null) {
                        if (element.getName().equals("td")) {
                            AttributeSet attributes = element.getAttributes();
                            String srcString = (String) attributes.getAttribute(HTML.Attribute.CLASS);
                            if (srcString != null && srcString.equals("diff-addedline")) {
                                if (!add) {
                                    System.out.println(srcString);
                                }
                                String text = getModificationText(element, srcString);
                                addlist.add(text);
                                printModification(text);
                                del = false;
                                add = true;
                            } else if (srcString != null && srcString.equals("diff-deletedline")) {
                                if (!del) {
                                    System.out.println(srcString);
                                }
                                String text = getModificationText(element, srcString);
                                dellist.add(text);
                                printModification(text);
                                del = true;
                                add = false;
                            }
                        }
                        if (element.getName().equals("span")) {
                            AttributeSet attributes = element.getAttributes();
                            String srcString = (String) attributes.getAttribute(HTML.Attribute.CLASS);
                            if (srcString != null && srcString.equals("diffchange")) {
                                System.out.println(srcString);
                                String text = getModificationText(element, srcString);
                                changelist.add(text);
                                printModification(text);
                                System.out.println(i);
                                i++;
                                del = false;
                                add = false;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Parser Exception");
                    System.out.println(e.getMessage());
                    System.out.println(e.getLocalizedMessage());
                    System.out.println(e.toString());
                    e.printStackTrace();
                }
            } catch (IOException e) {
                System.out.println("IOException, creating BufferedInputStream: ");
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            System.out.println("malformed url: " + adminURL);
            e.printStackTrace();
        }
    }

    public void printModification(String text) {
        System.out.println(text);
    }

    public String getModificationText(Element el, String s) {
        try {
            int startOffset = el.getStartOffset();
            int endOffset = el.getEndOffset();
            int length = endOffset - startOffset;
            String text = docd.getText(startOffset, length);
            return text;
        } catch (Exception e) {
            return "";
        }
    }
}
