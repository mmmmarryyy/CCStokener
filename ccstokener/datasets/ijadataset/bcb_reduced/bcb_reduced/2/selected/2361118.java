package org.fudaa.dodico.crue.common;

import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.lang.StringUtils;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.dodico.crue.io.UnicodeInputStream;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlVersionFinder extends DefaultHandler {

    String versionFound;

    public static final String ENCODING = "UTF-8";

    public String getVersion(final URL url) {
        versionFound = null;
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        SAXParser newSAXParser = null;
        UnicodeInputStream unicodeStream = null;
        try {
            InputStream resourceAsStream = url.openStream();
            unicodeStream = new UnicodeInputStream(resourceAsStream, XmlVersionFinder.ENCODING);
            unicodeStream.init();
            newSAXParser = factory.newSAXParser();
            newSAXParser.parse(unicodeStream, this);
        } catch (final Exception e) {
        } finally {
            CtuluLibFile.close(unicodeStream);
        }
        return versionFound;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
        if (attributes != null) {
            for (int i = attributes.getLength() - 1; i >= 0; i--) {
                if ("xsi:schemaLocation".equals(attributes.getLocalName(i))) {
                    final String value = attributes.getValue(i);
                    if (value != null) {
                        final String last = StringUtils.substringAfterLast(value, "-");
                        versionFound = StringUtils.substringBeforeLast(last, ".");
                    }
                    throw new SAXException("ok");
                }
            }
        }
        throw new SAXException("notOk");
    }
}
