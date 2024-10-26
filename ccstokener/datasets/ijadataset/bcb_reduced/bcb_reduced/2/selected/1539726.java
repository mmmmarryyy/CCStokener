package org.argouml.xml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.argouml.application.api.Argo;
import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Jim Holt
 */
public abstract class SAXParserBase extends HandlerBase {

    protected static final String _returnString = new String("\n      ");

    public SAXParserBase() {
    }

    protected static boolean _dbg = false;

    protected static boolean _verbose = false;

    private static XMLElement _elements[] = new XMLElement[100];

    private static int _nElements = 0;

    private static XMLElement _freeElements[] = new XMLElement[100];

    private static int _nFreeElements = 0;

    private static boolean _stats = true;

    private static long _parseTime = 0;

    protected boolean _startElement = false;

    public void setDebug(boolean debug) {
        _dbg = debug;
    }

    public void setStats(boolean stats) {
        _stats = stats;
    }

    public boolean getStats() {
        return _stats;
    }

    public long getParseTime() {
        return _parseTime;
    }

    public void parse(URL url) throws Exception {
        parse(url.openStream());
    }

    public void parse(InputStream is) throws Exception {
        long start, end;
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        try {
            SAXParser parser = factory.newSAXParser();
            InputSource input = new InputSource(is);
            input.setSystemId(getJarResource("org.argouml.kernel.Project"));
            start = System.currentTimeMillis();
            parser.parse(input, this);
            end = System.currentTimeMillis();
            _parseTime = end - start;
            if (_stats) {
                Argo.log.info("Elapsed time: " + (end - start) + " ms");
            }
        } catch (SAXException saxEx) {
            Exception ex = saxEx.getException();
            if (ex == null) {
                saxEx.printStackTrace();
            } else {
                ex.printStackTrace();
            }
        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    protected abstract void handleStartElement(XMLElement e);

    protected abstract void handleEndElement(XMLElement e);

    public void startElement(String name, AttributeList atts) throws SAXException {
        _startElement = true;
        XMLElement e = null;
        if (_nFreeElements > 0) {
            e = _freeElements[--_nFreeElements];
            e.setName(name);
            e.setAttributes(atts);
            e.resetText();
        } else e = new XMLElement(name, atts);
        if (_dbg) {
            System.out.println("START: " + name + " " + e);
            for (int i = 0; i < atts.getLength(); i++) {
                System.out.println("   ATT: " + atts.getName(i) + " " + atts.getValue(i));
            }
        }
        _elements[_nElements++] = e;
        handleStartElement(e);
        _startElement = false;
    }

    public void endElement(String name) throws SAXException {
        XMLElement e = _elements[--_nElements];
        if (_dbg) {
            System.out.println("END: " + e.getName() + " [" + e.getText() + "] " + e);
            for (int i = 0; i < e.getNumAttributes(); i++) {
                System.out.println("   ATT: " + e.getAttributeName(i) + " " + e.getAttributeValue(i));
            }
        }
        handleEndElement(e);
        _freeElements[_nFreeElements++] = e;
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        for (int i = 0; i < _nElements; i++) {
            XMLElement e = _elements[i];
            String test = e.getText();
            if (test.length() > 0) e.addText(_returnString);
            e.addText(new String(ch, start, length));
        }
    }

    public InputSource resolveEntity(String publicId, String systemId) {
        try {
            URL testIt = new URL(systemId);
            InputSource s = new InputSource(testIt.openStream());
            return s;
        } catch (Exception e) {
            if (_dbg || _verbose) {
                System.out.println("NOTE: Could not open DTD " + systemId);
            }
            String dtdName = systemId.substring(systemId.lastIndexOf('/') + 1);
            String dtdPath = "/org/argouml/xml/dtd/" + dtdName;
            InputStream is = SAXParserBase.class.getResourceAsStream(dtdPath);
            if (is == null) {
                try {
                    is = new FileInputStream(dtdPath.substring(1));
                } catch (Exception ex) {
                }
            }
            return new InputSource(is);
        }
    }

    public String getJarResource(String cls) {
        String jarFile = "";
        String fileSep = System.getProperty("file.separator");
        String classFile = cls.replace('.', fileSep.charAt(0)) + ".class";
        ClassLoader thisClassLoader = this.getClass().getClassLoader();
        URL url = thisClassLoader.getResource(classFile);
        if (url != null) {
            String urlString = url.getFile();
            int idBegin = urlString.indexOf("file:");
            int idEnd = urlString.indexOf("!");
            if (idBegin > -1 && idEnd > -1 && idEnd > idBegin) jarFile = urlString.substring(idBegin + 5, idEnd);
        }
        return jarFile;
    }

    public void ignoreElement(XMLElement e) {
        System.out.println("NOTE: ignoring tag:" + e.getName());
    }

    public void notImplemented(XMLElement e) {
        System.out.println("NOTE: element not implemented: " + e.getName());
    }
}
