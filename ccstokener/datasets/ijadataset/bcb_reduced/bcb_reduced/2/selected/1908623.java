package gnu.xml.dom.ls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSSerializerFilter;
import org.w3c.dom.traversal.NodeFilter;
import gnu.xml.dom.DomDOMException;
import gnu.xml.transform.StreamSerializer;

/**
 * Serialize a DOM node to a stream.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
public class DomLSSerializer extends StreamSerializer implements LSSerializer, DOMConfiguration, DOMStringList {

    private static final List SUPPORTED_PARAMETERS = Arrays.asList(new String[] { "discard-default-content", "xml-declaration" });

    private LSSerializerFilter filter;

    private StreamSerializer serializer;

    public DomLSSerializer() {
        super();
        discardDefaultContent = true;
    }

    public DOMConfiguration getDomConfig() {
        return this;
    }

    public String getNewLine() {
        return eol;
    }

    public void setNewLine(String newLine) {
        if (newLine == null) {
            newLine = System.getProperty("line.separator");
        }
        eol = newLine;
    }

    public LSSerializerFilter getFilter() {
        return filter;
    }

    public void setFilter(LSSerializerFilter filter) {
        this.filter = filter;
    }

    public boolean write(Node node, LSOutput output) throws LSException {
        OutputStream out = output.getByteStream();
        try {
            if (out == null) {
                String systemId = output.getSystemId();
                try {
                    URL url = new URL(systemId);
                    URLConnection connection = url.openConnection();
                    connection.setDoOutput(true);
                    if (connection instanceof HttpURLConnection) {
                        ((HttpURLConnection) connection).setRequestMethod("PUT");
                    }
                    out = connection.getOutputStream();
                } catch (MalformedURLException e) {
                    File file = new File(systemId);
                    out = new FileOutputStream(file);
                }
            }
            serialize(node, out);
            out.flush();
            return true;
        } catch (IOException e) {
            throw new DomLSException(LSException.SERIALIZE_ERR, e);
        }
    }

    public boolean writeToURI(Node node, String uri) throws LSException {
        LSOutput output = new DomLSOutput();
        output.setSystemId(uri);
        return write(node, output);
    }

    public String writeToString(Node node) throws DOMException, LSException {
        Writer writer = new StringWriter();
        LSOutput output = new DomLSOutput();
        output.setCharacterStream(writer);
        write(node, output);
        return writer.toString();
    }

    public void serialize(Node node, OutputStream out) throws IOException {
        if (filter == null) {
            super.serialize(node, out);
        } else {
            int wts = filter.getWhatToShow();
            if (wts != NodeFilter.SHOW_ALL) {
                switch(node.getNodeType()) {
                    case Node.ATTRIBUTE_NODE:
                        if ((wts & NodeFilter.SHOW_ATTRIBUTE) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.TEXT_NODE:
                        if ((wts & NodeFilter.SHOW_TEXT) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.ELEMENT_NODE:
                        if ((wts & NodeFilter.SHOW_ELEMENT) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.CDATA_SECTION_NODE:
                        if ((wts & NodeFilter.SHOW_CDATA_SECTION) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.COMMENT_NODE:
                        if ((wts & NodeFilter.SHOW_COMMENT) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.DOCUMENT_NODE:
                        if ((wts & NodeFilter.SHOW_DOCUMENT) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.DOCUMENT_TYPE_NODE:
                        if ((wts & NodeFilter.SHOW_DOCUMENT_TYPE) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.PROCESSING_INSTRUCTION_NODE:
                        if ((wts & NodeFilter.SHOW_PROCESSING_INSTRUCTION) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.DOCUMENT_FRAGMENT_NODE:
                        if ((wts & NodeFilter.SHOW_DOCUMENT_FRAGMENT) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.ENTITY_NODE:
                        if ((wts & NodeFilter.SHOW_ENTITY) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.ENTITY_REFERENCE_NODE:
                        if ((wts & NodeFilter.SHOW_ENTITY_REFERENCE) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                    case Node.NOTATION_NODE:
                        if ((wts & NodeFilter.SHOW_NOTATION) == 0) {
                            super.serialize(node, out);
                            return;
                        }
                        break;
                }
            }
            switch(filter.acceptNode(node)) {
                case NodeFilter.FILTER_ACCEPT:
                    super.serialize(node, out);
                    break;
                case NodeFilter.FILTER_REJECT:
                    break;
                case NodeFilter.FILTER_SKIP:
                    Node first = node.getFirstChild();
                    if (first != null) {
                        serialize(first, out);
                    }
                    break;
            }
        }
    }

    public void setParameter(String name, Object value) throws DOMException {
        if ("discard-default-content".equals(name)) {
            discardDefaultContent = "true".equals(value.toString());
        } else if ("xml-declaration".equals(name)) {
            xmlDeclaration = "false".equals(value.toString());
        } else {
            throw new DomDOMException(DOMException.NOT_SUPPORTED_ERR);
        }
    }

    public Object getParameter(String name) throws DOMException {
        if ("discard-default-content".equals(name)) {
            return discardDefaultContent ? "true" : "false";
        } else if ("xml-declaration".equals(name)) {
            return xmlDeclaration ? "true" : "false";
        } else {
            throw new DomDOMException(DOMException.NOT_SUPPORTED_ERR);
        }
    }

    public boolean canSetParameter(String name, Object value) {
        return contains(name);
    }

    public DOMStringList getParameterNames() {
        return this;
    }

    public String item(int i) {
        return (String) SUPPORTED_PARAMETERS.get(i);
    }

    public int getLength() {
        return SUPPORTED_PARAMETERS.size();
    }

    public boolean contains(String str) {
        return SUPPORTED_PARAMETERS.contains(str);
    }
}
