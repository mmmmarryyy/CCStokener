package org.apache.xmlrpc.applet;

import uk.co.wilson.xml.MinML;
import org.xml.sax.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;

/**
 *  A simple XML-RPC client 
 */
public class SimpleXmlRpcClient {

    URL url;

    /** 
     * Construct a XML-RPC client with this URL.
     */
    public SimpleXmlRpcClient(URL url) {
        this.url = url;
    }

    /** 
     * Construct a XML-RPC client for the URL represented by this String.
     */
    public SimpleXmlRpcClient(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    /** 
     * Construct a XML-RPC client for the specified hostname and port.
     */
    public SimpleXmlRpcClient(String hostname, int port) throws MalformedURLException {
        this.url = new URL("http://" + hostname + ":" + port + "/RPC2");
    }

    public Object execute(String method, Vector params) throws XmlRpcException, IOException {
        return new XmlRpcSupport(url).execute(method, params);
    }
}

class XmlRpcSupport extends HandlerBase {

    URL url;

    String methodName;

    boolean fault = false;

    Object result = null;

    Stack values;

    Value currentValue;

    boolean readCdata;

    static final DateFormat format = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");

    StringBuffer cdata = new StringBuffer();

    static final int STRING = 0;

    static final int INTEGER = 1;

    static final int BOOLEAN = 2;

    static final int DOUBLE = 3;

    static final int DATE = 4;

    static final int BASE64 = 5;

    static final int STRUCT = 6;

    static final int ARRAY = 7;

    public static boolean debug = false;

    static final String types[] = { "String", "Integer", "Boolean", "Double", "Date", "Base64", "Struct", "Array" };

    public XmlRpcSupport(URL url) {
        this.url = url;
    }

    /**
     * Switch debugging output on/off.
     */
    public static void setDebug(boolean val) {
        debug = val;
    }

    /** 
     * Parse the input stream. For each root level object, method <code>objectParsed</code>
     * is called.
     */
    synchronized void parse(InputStream is) throws Exception {
        values = new Stack();
        long now = System.currentTimeMillis();
        MinML parser = new MinML();
        parser.setDocumentHandler(this);
        parser.setErrorHandler(this);
        parser.parse(new InputSource(is));
        if (debug) System.out.println("Spent " + (System.currentTimeMillis() - now) + " parsing");
    }

    /**
     * Writes the XML representation of a supported Java object to the XML writer.
     */
    void writeObject(Object what, XmlWriter writer) throws IOException {
        writer.startElement("value");
        if (what instanceof String) {
            writer.write(what.toString());
        } else if (what instanceof Integer) {
            writer.startElement("int");
            writer.write(what.toString());
            writer.endElement("int");
        } else if (what instanceof Boolean) {
            writer.startElement("boolean");
            writer.write(((Boolean) what).booleanValue() ? "1" : "0");
            writer.endElement("boolean");
        } else if (what instanceof Double) {
            writer.startElement("double");
            writer.write(what.toString());
            writer.endElement("double");
        } else if (what instanceof Date) {
            writer.startElement("dateTime.iso8601");
            Date d = (Date) what;
            writer.write(format.format(d));
            writer.endElement("dateTime.iso8601");
        } else if (what instanceof byte[]) {
            writer.startElement("base64");
            sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            writer.write(encoder.encodeBuffer((byte[]) what));
            writer.endElement("base64");
        } else if (what instanceof Vector) {
            writer.startElement("array");
            writer.startElement("data");
            Vector v = (Vector) what;
            int l2 = v.size();
            for (int i2 = 0; i2 < l2; i2++) writeObject(v.elementAt(i2), writer);
            writer.endElement("data");
            writer.endElement("array");
        } else if (what instanceof Hashtable) {
            writer.startElement("struct");
            Hashtable h = (Hashtable) what;
            for (Enumeration e = h.keys(); e.hasMoreElements(); ) {
                String nextkey = (String) e.nextElement();
                Object nextval = h.get(nextkey);
                writer.startElement("member");
                writer.startElement("name");
                writer.write(nextkey);
                writer.endElement("name");
                writeObject(nextval, writer);
                writer.endElement("member");
            }
            writer.endElement("struct");
        } else {
            String unsupportedType = what == null ? "null" : what.getClass().toString();
            throw new IOException("unsupported Java type: " + unsupportedType);
        }
        writer.endElement("value");
    }

    /**
     * Generate an XML-RPC request and send it to the server. Parse the result and
     * return the corresponding Java object.
     *
     * @exception XmlRpcException: If the remote host returned a fault message.
     * @exception IOException: If the call could not be made for lower level problems.
     */
    public Object execute(String method, Vector arguments) throws XmlRpcException, IOException {
        fault = false;
        long now = System.currentTimeMillis();
        try {
            StringBuffer strbuf = new StringBuffer();
            XmlWriter writer = new XmlWriter(strbuf);
            writeRequest(writer, method, arguments);
            byte[] request = strbuf.toString().getBytes();
            URLConnection con = url.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setAllowUserInteraction(false);
            con.setRequestProperty("Content-Length", Integer.toString(request.length));
            con.setRequestProperty("Content-Type", "text/xml");
            OutputStream out = con.getOutputStream();
            out.write(request);
            out.flush();
            InputStream in = con.getInputStream();
            parse(in);
            System.out.println("result = " + result);
        } catch (Exception x) {
            x.printStackTrace();
            throw new IOException(x.getMessage());
        }
        if (fault) {
            XmlRpcException exception = null;
            try {
                Hashtable f = (Hashtable) result;
                String faultString = (String) f.get("faultString");
                int faultCode = Integer.parseInt(f.get("faultCode").toString());
                exception = new XmlRpcException(faultCode, faultString.trim());
            } catch (Exception x) {
                throw new XmlRpcException(0, "Invalid fault response");
            }
            throw exception;
        }
        System.out.println("Spent " + (System.currentTimeMillis() - now) + " in request");
        return result;
    }

    /**
     * Called when the return value has been parsed. 
     */
    void objectParsed(Object what) {
        result = what;
    }

    /**
     * Generate an XML-RPC request from a method name and a parameter vector.
     */
    void writeRequest(XmlWriter writer, String method, Vector params) throws IOException {
        writer.startElement("methodCall");
        writer.startElement("methodName");
        writer.write(method);
        writer.endElement("methodName");
        writer.startElement("params");
        int l = params.size();
        for (int i = 0; i < l; i++) {
            writer.startElement("param");
            writeObject(params.elementAt(i), writer);
            writer.endElement("param");
        }
        writer.endElement("params");
        writer.endElement("methodCall");
    }

    /**
     * Method called by SAX driver.
     */
    public void characters(char ch[], int start, int length) throws SAXException {
        if (!readCdata) return;
        cdata.append(ch, start, length);
    }

    /**
      * Method called by SAX driver.
     */
    public void endElement(String name) throws SAXException {
        if (debug) System.err.println("endElement: " + name);
        if (currentValue != null && readCdata) {
            currentValue.characterData(cdata.toString());
            cdata.setLength(0);
            readCdata = false;
        }
        if ("value".equals(name)) {
            int depth = values.size();
            if (depth < 2 || values.elementAt(depth - 2).hashCode() != STRUCT) {
                Value v = currentValue;
                values.pop();
                if (depth < 2) {
                    objectParsed(v.value);
                    currentValue = null;
                } else {
                    currentValue = (Value) values.peek();
                    currentValue.endElement(v);
                }
            }
        }
        if ("member".equals(name)) {
            Value v = currentValue;
            values.pop();
            currentValue = (Value) values.peek();
            currentValue.endElement(v);
        } else if ("methodName".equals(name)) {
            methodName = cdata.toString();
            cdata.setLength(0);
            readCdata = false;
        }
    }

    /**
      * Method called by SAX driver.
      */
    public void startElement(String name, AttributeList atts) throws SAXException {
        if (debug) System.err.println("startElement: " + name);
        if ("value".equals(name)) {
            Value v = new Value();
            values.push(v);
            currentValue = v;
            cdata.setLength(0);
            readCdata = true;
        } else if ("methodName".equals(name)) {
            cdata.setLength(0);
            readCdata = true;
        } else if ("name".equals(name)) {
            cdata.setLength(0);
            readCdata = true;
        } else if ("string".equals(name)) {
            cdata.setLength(0);
            readCdata = true;
        } else if ("i4".equals(name) || "int".equals(name)) {
            currentValue.setType(INTEGER);
            cdata.setLength(0);
            readCdata = true;
        } else if ("boolean".equals(name)) {
            currentValue.setType(BOOLEAN);
            cdata.setLength(0);
            readCdata = true;
        } else if ("double".equals(name)) {
            currentValue.setType(DOUBLE);
            cdata.setLength(0);
            readCdata = true;
        } else if ("dateTime.iso8601".equals(name)) {
            currentValue.setType(DATE);
            cdata.setLength(0);
            readCdata = true;
        } else if ("base64".equals(name)) {
            currentValue.setType(BASE64);
            cdata.setLength(0);
            readCdata = true;
        } else if ("struct".equals(name)) currentValue.setType(STRUCT); else if ("array".equals(name)) currentValue.setType(ARRAY);
    }

    public void error(SAXParseException e) throws SAXException {
        System.err.println("Error parsing XML: " + e);
    }

    public void fatalError(SAXParseException e) throws SAXException {
        System.err.println("Fatal error parsing XML: " + e);
    }

    /**
      * This represents an XML-RPC Value while the request is being parsed.
      */
    class Value {

        int type;

        Object value;

        String nextMemberName;

        Hashtable struct;

        Vector array;

        /**
         * Constructor.
         */
        public Value() {
            this.type = STRING;
        }

        /**
          * Notification that a new child element has been parsed.
          */
        public void endElement(Value child) {
            if (type == ARRAY) array.addElement(child.value); else if (type == STRUCT) struct.put(nextMemberName, child.value);
        }

        /**
          * Set the type of this value. If it's a container, create the corresponding java container.
          */
        public void setType(int type) {
            this.type = type;
            if (type == ARRAY) value = array = new Vector();
            if (type == STRUCT) value = struct = new Hashtable();
        }

        /**
          * Set the character data for the element and interpret it according to the
          * element type
          */
        public void characterData(String cdata) {
            switch(type) {
                case INTEGER:
                    value = new Integer(cdata.trim());
                    break;
                case BOOLEAN:
                    value = new Boolean("1".equals(cdata.trim()));
                    break;
                case DOUBLE:
                    value = new Double(cdata.trim());
                    break;
                case DATE:
                    try {
                        value = format.parse(cdata.trim());
                    } catch (ParseException p) {
                        throw new RuntimeException(p.getMessage());
                    }
                    break;
                case BASE64:
                    sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
                    try {
                        value = decoder.decodeBuffer(cdata);
                    } catch (IOException x) {
                        throw new RuntimeException("Error decoding base64 tag: " + x.getMessage());
                    }
                    break;
                case STRING:
                    value = cdata;
                    break;
                case STRUCT:
                    nextMemberName = cdata;
                    break;
            }
        }

        public int hashCode() {
            return type;
        }

        public String toString() {
            return (types[type] + " element " + value);
        }
    }

    class XmlWriter {

        StringBuffer buf;

        String enc;

        public XmlWriter(StringBuffer buf) {
            this.buf = buf;
            buf.append("<?xml version=\"1.0\"?>");
        }

        public void startElement(String elem) {
            buf.append("<");
            buf.append(elem);
            buf.append(">");
        }

        public void endElement(String elem) {
            buf.append("</");
            buf.append(elem);
            buf.append(">");
        }

        public void emptyElement(String elem) {
            buf.append("<");
            buf.append(elem);
            buf.append("/>");
        }

        public void chardata(String text) {
            int l = text.length();
            for (int i = 0; i < l; i++) {
                char c = text.charAt(i);
                switch(c) {
                    case '<':
                        buf.append("&lt;");
                        break;
                    case '>':
                        buf.append("&gt;");
                        break;
                    case '&':
                        buf.append("&amp;");
                        break;
                    default:
                        buf.append(c);
                }
            }
        }

        public void write(char[] text) {
            buf.append(text);
        }

        public void write(String text) {
            buf.append(text);
        }

        public String toString() {
            return buf.toString();
        }

        public byte[] getBytes() throws UnsupportedEncodingException {
            return buf.toString().getBytes();
        }
    }
}
