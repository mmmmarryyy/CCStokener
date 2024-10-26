package filebot.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Stack;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A very basic xml parser, that might work on invalid documents (especially webpages). Doesn't work on more complex xml
 * files (e.g. with namespaces) and ignores some more complex attributes like style="border: thin solid black;". I wrote
 * this because JTidy and nekohtml both couldn't handle TV.com or TVRage.com, but having spent a few hours
 * look at the html source, I can't really blame them, 'cause some of the "mistakes" are really cruel ...
 * 
 * HTML Sacrilege Highlights
 * 1. <li"> ... nice tag, resists any xml/html parser there is
 * 2. <table><tr><td></td></table> ... can't those stupid tags close themself?
 * and a lot more ... sadly
 * 
 * @author Reinhard
 * 
 */
public class WebpageXmlParser {

    public Document parse(URL url, String charsetName) throws IOException, ParserConfigurationException {
        URLConnection conn = url.openConnection();
        String encoding = conn.getContentEncoding();
        InputStream in = conn.getInputStream();
        if (encoding != null) if (encoding.equalsIgnoreCase("gzip")) in = new GZIPInputStream(in); else System.err.println("Unknown encoding");
        return parse(in, charsetName);
    }

    public Document parse(InputStream in, String charsetName) throws IOException, ParserConfigurationException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, charsetName));
        StringBuffer sb = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) sb.append(line);
        return parse(sb.toString());
    }

    public Document parse(String xml) throws ParserConfigurationException {
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Node node = dom;
        String tags[] = xml.replaceAll("\n", "").split("<");
        Stack<String> tagStack = new Stack<String>();
        boolean script = false;
        for (String e : tags) {
            if (e.startsWith("/")) {
                String endtag = e.split("[\\s+]", 0)[0].replaceAll("\\W", "");
                if (endtag.equalsIgnoreCase("script")) script = false;
                if (script) continue;
                if (!isUsuallyNotClosedTag(endtag)) {
                    String starttag = tagStack.pop();
                    if (!isEndTagMissing(starttag, endtag, tagStack)) {
                        if (node.getParentNode() != null) node = node.getParentNode();
                    } else {
                        for (int i = 0; i < 2; i++) {
                            if (node.getParentNode() != null) node = node.getParentNode();
                        }
                    }
                }
                continue;
            }
            if (script || !e.matches("\\w.*")) continue;
            int closetagPosition = e.indexOf(">");
            String tag = e.substring(0, closetagPosition);
            String textContent = e.substring(closetagPosition + 1, e.length());
            String tagname = tag.replaceAll("['\"]", "").split("[\\s+|/]", 0)[0];
            Element sub = dom.createElement(tagname);
            sub.setTextContent(textContent);
            String tagsplit[] = tag.replaceAll("['\"]", "").split("[\\s+]");
            for (int i = 1; i < tagsplit.length; i++) {
                String attr = tagsplit[i];
                if (attr.matches("\\w+=.+")) {
                    String a[] = attr.split("=", 2);
                    sub.setAttribute(a[0], a[1]);
                }
            }
            if (tag.endsWith("/") || isUsuallyNotClosedTag(tagname)) {
                node.appendChild(sub);
            } else {
                node.appendChild(sub);
                node = sub;
                tagStack.push(tagname);
                if (tagname.equalsIgnoreCase("script")) {
                    script = true;
                }
            }
        }
        return dom;
    }

    private String usuallyNotClosedTags[] = { "img", "br", "hr", "input", "i" };

    private boolean isUsuallyNotClosedTag(String tag) {
        for (String s : usuallyNotClosedTags) {
            if (tag.equalsIgnoreCase(s)) return true;
        }
        return false;
    }

    /**
	 * missing pair (a,b) means, that b is missing, when a has occured although b was expected
	 * 
	 * Example: <a> <b> <c> <c> </a>
	 * 
	 * I need this for parsing TVRage.com, because lots of tr-tags are never closed.
	 */
    private String missingPairs[][] = { { "table", "tr" } };

    private boolean isEndTagMissing(String starttag, String endtag, Stack<String> tagStack) {
        if (tagStack.isEmpty()) return false;
        String current = tagStack.peek();
        for (String[] pair : missingPairs) {
            if (current.equalsIgnoreCase(pair[0])) {
                if (starttag.equalsIgnoreCase(pair[1]) && endtag.equalsIgnoreCase(pair[0])) return true;
            }
        }
        return false;
    }
}
