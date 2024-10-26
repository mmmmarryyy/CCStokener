package org.openexi.fujitsu.util;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

public abstract class XMLResourceBundle extends ResourceBundle {

    private final HashMap<String, String> m_msgs = new HashMap<String, String>();

    public XMLResourceBundle() throws MissingResourceException {
        String systemId = getShortName() + ".xml";
        URL url;
        if ((url = getClass().getResource(systemId)) != null) {
            InputStream is = null;
            try {
                is = url.openStream();
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(false);
                factory.setValidating(false);
                XMLReader xmlReader = factory.newSAXParser().getXMLReader();
                xmlReader.setContentHandler(new MessageContentHandler());
                xmlReader.parse(new InputSource(is));
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
                ioe.printStackTrace();
            } catch (SAXException se) {
                System.err.println(se.getMessage());
                se.printStackTrace();
            } catch (ParserConfigurationException pce) {
                System.err.println(pce.getMessage());
                pce.printStackTrace();
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException ioe) {
                    System.err.println(ioe.getMessage());
                    ioe.printStackTrace();
                }
            }
        } else {
            throw new MissingResourceException("Resource file '" + systemId + "' could not be found.", systemId, null);
        }
    }

    /**
   * Returns the short name of the class. Short name is the class name
   * minus its package name part.
   */
    protected String getShortName() {
        String pname = getClass().getPackage().getName();
        String lname = getClass().getName();
        return lname.substring(pname.length() + 1, lname.length());
    }

    public Enumeration<String> getKeys() {
        Set<String> keySet = m_msgs.keySet();
        final Iterator<String> iter = keySet.iterator();
        return new Enumeration<String>() {

            public String nextElement() {
                return iter.next();
            }

            public boolean hasMoreElements() {
                return iter.hasNext();
            }
        };
    }

    protected Object handleGetObject(String key) throws MissingResourceException {
        return (String) m_msgs.get(key);
    }

    private final class MessageContentHandler extends DefaultHandler {

        private String m_key;

        private int m_msg_level;

        private int m_level;

        private final StringBuffer m_buf = new StringBuffer();

        public void startDocument() throws SAXException {
            m_level = 0;
            m_msg_level = 0;
            m_key = null;
        }

        public void startElement(String namespaceURI, String localName, String qualName, Attributes atts) throws SAXException {
            ++m_level;
            if (qualName.equals("msg")) {
                if ((m_key = atts.getValue("id")) != null) {
                    m_key = m_key.trim();
                    m_msg_level = m_level;
                }
                m_buf.delete(0, m_buf.length());
            }
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (m_msg_level > 0 && m_level >= m_msg_level) m_buf.append(ch, start, length);
        }

        public void endElement(String namespaceURI, String localName, String qualName) throws SAXException {
            if (m_level == m_msg_level) {
                if (m_key != null && m_key.length() > 0) m_msgs.put(m_key, m_buf.toString());
                m_msg_level = 0;
            }
            --m_level;
        }
    }
}
