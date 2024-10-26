package com.jes.classfinder.helpers.preferences;

import java.io.IOException;
import java.net.URL;
import org.apache.log4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author JDickerson
 * 
 * Created on 16 Jun 2007
 */
public class PreferencesXmlParserEntityResolver implements EntityResolver {

    protected Logger logger = Logger.getLogger(PreferencesXmlParserEntityResolver.class);

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        URL urlClassloaderSchemaFile = this.getClass().getResource("preferences.xsd");
        InputSource inputSource = new InputSource(urlClassloaderSchemaFile.openStream());
        return inputSource;
    }
}
