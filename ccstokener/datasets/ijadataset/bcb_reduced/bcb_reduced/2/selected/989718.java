package net.n3.nanoxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

/**
 * Extend the XMLBuilder to add XInclude functionality
 */
public class XIncludeXMLBuilder extends StdXMLBuilder {

    /**
     * Namespace for XInclude  (NOTE that this is not used
     * at the moment). The specification can be found
     * <a href="http://www.w3.org/TR/xinclude/">here</a>.
     */
    public static final String INCLUDE_NS = "http://www.w3.org/2001/XInclude";

    /**
     * The name of the include element (this should be "include" using the
     * {@link #INCLUDE_NS} but namespaces are not supported
     */
    public static final String INCLUDE_ELEMENT = "xinclude";

    /**
     * The location of the included data
     */
    public static final String HREF_ATTRIB = "href";

    /**
     * The xpointer attribute. This must not be used when "parse='text'"
     */
    public static final String XPOINTER_ATTRIB = "xpointer";

    /**
     * The attribute to decribe the encoding of the text include (no effect when
     * parse='xml')
     */
    public static final String ENCODING_ATTRIB = "encoding";

    /**
     * The attribute describing the accept header that will be used with
     * http based includes.
     */
    public static final String ACCEPT_ENCODING = "accept";

    /**
     * The element for handling fallbacks. This should be called "fallback" and
     * be in the {@link #INCLUDE_NS} but namespaces are not supported
     */
    public static final String FALLBACK_ELEMENT = "xfallback";

    /**
     * Parse attribute. If missing this implies "xml" its other valid value
     * is "text"
     */
    public static final String PARSE_ATTRIB = "parse";

    /**
     * Namespace for the "fragment" element used to include xml documents with
     * no explicit root node.
     */
    public static final String FRAGMENT_NS = "http://izpack.org/izpack/fragment";

    /**
     * The fragment element is a root node element that can be
     * used to wrap xml fragments for inclusion. It is removed during the
     * include operation. This should be called "fragment" and be in the
     * {@link #FRAGMENT_NS} but namespaces are not supported.
     */
    public static final String FRAGMENT = "xfragment";

    public void endElement(String name, String nsPrefix, String nsSystemID) {
        XMLElement element = getCurrentElement();
        super.endElement(name, nsPrefix, nsSystemID);
        processXInclude(element);
    }

    /**
     * This method handles XInclude elements in the code
     *
     * @param element the node currently being procesed. In this case it should
     *                be the {@link #INCLUDE_ELEMENT}
     */
    private void processXInclude(final XMLElement element) {
        if (INCLUDE_ELEMENT.equals(element.getName())) {
            Vector<XMLElement> fallbackChildren = element.getChildrenNamed(FALLBACK_ELEMENT);
            if (element.getChildrenCount() != fallbackChildren.size() || fallbackChildren.size() > 1) {
                throw new RuntimeException(new XMLParseException(element.getSystemID(), element.getLineNr(), INCLUDE_ELEMENT + " can optionally have a single " + FRAGMENT + " as a child"));
            }
            boolean usingFallback = false;
            String href = element.getAttribute(HREF_ATTRIB, "");
            if (!href.equals("")) {
                IXMLReader reader = null;
                try {
                    reader = getReader(element);
                } catch (Exception e) {
                    reader = handleFallback(element);
                    usingFallback = true;
                }
                String parse = element.getAttribute(PARSE_ATTRIB, "xml");
                if ("text".equals(parse) && !usingFallback) {
                    includeText(element, reader);
                } else if ("xml".equals(parse)) {
                    includeXML(element, reader);
                } else {
                    throw new RuntimeException(new XMLParseException(element.getSystemID(), element.getLineNr(), PARSE_ATTRIB + " attribute of " + INCLUDE_ELEMENT + " must be \"xml\" or \"text\" but was " + parse));
                }
            } else {
                if (!element.hasAttribute(XPOINTER_ATTRIB)) {
                    throw new RuntimeException(new XMLParseException(element.getSystemID(), element.getLineNr(), XPOINTER_ATTRIB + "must be specified if href is " + "empty or missing"));
                }
            }
        }
    }

    /**
     * Handle the fallback if one exists. If one does not exist then throw
     * a runtime exception as this is a fatal error
     *
     * @param include the include element
     * @return a reader for the fallback
     */
    private IXMLReader handleFallback(XMLElement include) {
        Vector<XMLElement> fallbackChildren = include.getChildrenNamed(FALLBACK_ELEMENT);
        if (fallbackChildren.size() == 1) {
            XMLElement fallback = fallbackChildren.get(0);
            String content = fallback.getContent();
            if (content != null) {
                content = content.trim();
            }
            if ("".equals(content) || content == null) {
                content = "<?xml version=\"1.0\" encoding=\"iso-8859-1\" standalone=\"yes\" ?><" + FRAGMENT + "/>";
            }
            return StdXMLReader.stringReader(content);
        } else {
            throw new RuntimeException(new XMLParseException(include.getSystemID(), include.getLineNr(), "could not load content"));
        }
    }

    /**
     * Include the xml contained in the specified reader. This content will be
     * parsed and attached to the parent of the <param>element</param> node
     *
     * @param element the include element
     * @param reader  the reader containing the xml to parse and include.
     */
    private void includeXML(final XMLElement element, IXMLReader reader) {
        try {
            Stack<XMLElement> stack = getStack();
            StdXMLParser parser = new StdXMLParser();
            parser.setBuilder(XMLBuilderFactory.createXMLBuilder());
            parser.setReader(reader);
            parser.setValidator(new NonValidator());
            XMLElement childroot = (XMLElement) parser.parse();
            if (stack.isEmpty()) {
                setRootElement(childroot);
            } else {
                XMLElement parent = stack.peek();
                parent.removeChild(element);
                if (FRAGMENT.equals(childroot.getName())) {
                    Vector grandchildren = childroot.getChildren();
                    Iterator it = grandchildren.iterator();
                    while (it.hasNext()) {
                        XMLElement grandchild = (XMLElement) it.next();
                        parent.addChild(grandchild);
                    }
                } else {
                    parent.addChild(childroot);
                }
            }
        } catch (XMLException e) {
            throw new RuntimeException(new XMLParseException(element.getSystemID(), element.getLineNr(), e.getMessage()));
        }
    }

    /**
     * Include plain text. The reader contains the content in the appropriate
     * encoding as determined by the {@link #ENCODING_ATTRIB} if one was
     * present.
     *
     * @param element the include element
     * @param reader  the reader containing the include text
     */
    private void includeText(XMLElement element, IXMLReader reader) {
        if (element.getAttribute("xpointer") != null) {
            throw new RuntimeException(new XMLParseException("xpointer cannot be used with parse='text'"));
        }
        Stack<XMLElement> stack = getStack();
        if (stack.isEmpty()) {
            throw new RuntimeException(new XMLParseException(element.getSystemID(), element.getLineNr(), "cannot include text as the root node"));
        }
        XMLElement parent = stack.peek();
        parent.removeChild(element);
        StringBuffer buffer = new StringBuffer();
        try {
            while (!reader.atEOF()) {
                buffer.append(reader.read());
            }
        } catch (IOException e) {
            throw new RuntimeException(new XMLParseException(element.getSystemID(), element.getLineNr(), e.getMessage()));
        }
        if (parent.getChildrenCount() == 0) {
            parent.setContent(buffer.toString());
        } else {
            XMLElement content = new XMLElement();
            content.setContent(buffer.toString());
            parent.addChild(content);
        }
    }

    /**
     * Return a reader for the specified {@link #INCLUDE_ELEMENT}. The caller
     * is responsible for closing the reader produced.
     *
     * @param element the include element to obtain a reader for
     * @return a reader for the include element
     * @throws XMLParseException if a problem occurs parsing the
     *                           {@link #INCLUDE_ELEMENT}
     * @throws IOException       if the href cannot be read
     */
    private IXMLReader getReader(XMLElement element) throws XMLParseException, IOException {
        String href = element.getAttribute(HREF_ATTRIB);
        URL url = null;
        try {
            url = new URL(href);
        } catch (MalformedURLException e) {
            try {
                if (href.charAt(0) == '/') {
                    url = new URL("file://" + href);
                } else {
                    url = new URL(new URL(element.getSystemID()), href);
                }
            } catch (MalformedURLException e1) {
                new XMLParseException(element.getSystemID(), element.getLineNr(), "malformed url '" + href + "'");
            }
        }
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpURLConnection && element.hasAttribute(ENCODING_ATTRIB)) {
            connection.setRequestProperty("accept", element.getAttribute(ENCODING_ATTRIB));
        }
        InputStream is = connection.getInputStream();
        InputStreamReader reader = null;
        if (element.getAttribute(PARSE_ATTRIB, "xml").equals("text") && element.hasAttribute(ENCODING_ATTRIB)) {
            reader = new InputStreamReader(is, element.getAttribute(ENCODING_ATTRIB, ""));
        } else {
            reader = new InputStreamReader(is);
        }
        IXMLReader ireader = new StdXMLReader(reader);
        ireader.setSystemID(url.toExternalForm());
        return ireader;
    }

    /**
     * used to record the system id for this document.
     *
     * @param systemID the system id of the document being built
     * @param lineNr   the line number
     */
    public void startBuilding(String systemID, int lineNr) {
        super.startBuilding(systemID, lineNr);
    }
}
