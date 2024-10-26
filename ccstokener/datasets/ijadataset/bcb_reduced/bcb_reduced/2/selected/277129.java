package org.jaffa.rules.metadata;

import java.util.StringTokenizer;
import java.net.URL;
import org.jaffa.cache.CacheManager;
import org.jaffa.cache.ICache;
import org.jaffa.util.URLHelper;
import org.jaffa.datatypes.Parser;
import org.jaffa.util.DefaultEntityResolver;
import org.jaffa.util.DefaultErrorHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import org.jaffa.config.Config;

/** This parses the Validator config file and creates instances of the FieldValidatorMetaData class. The instances will be cached.
 */
public class ValidatorMetaDataService {

    private static final String PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    private static final String XML_FILE = "classpath:///resources/validators.xml";

    private static final String SUCCESS_MESSAGE = ValidatorMetaDataService.class.getName();

    private static ICache c_cache = CacheManager.getCache(ValidatorMetaDataService.class.getName());

    /** This will return a FieldValidatorMetaData instance for the input name. A null will be returned, in case no definition is found in the Validator config file.
     * @param name The name of the validator.
     * @return a FieldValidatorMetaData instance
     * @throws SAXException If any exception is thrown while parsing the XML file.
     * @throws IOException If any I/O error occurs in reading the XML file. Note: A FileNotFoundException will be thrown, in case the file is not found.
     */
    public static FieldValidatorMetaData getFieldValidatorMetaData(String name) throws SAXException, IOException {
        if (!c_cache.containsKey(name)) find(name);
        return (FieldValidatorMetaData) c_cache.get(name);
    }

    /** Parse the validator-xml file for the given named validator.
     */
    private static synchronized void find(String name) throws SAXException, IOException {
        if (c_cache.containsKey(name)) return;
        CustomHandler handler = null;
        URL url = null;
        String validatorFiles = (String) Config.getProperty(Config.PROP_RULES_ENGINE_VALIDATORS_URL_LIST, XML_FILE);
        for (StringTokenizer strtknzr = new StringTokenizer(validatorFiles, ","); strtknzr.hasMoreTokens(); ) {
            String validatorFile = strtknzr.nextToken();
            try {
                url = URLHelper.newExtendedURL(validatorFile);
            } catch (MalformedURLException e) {
                url = null;
            }
            if (url == null) throw new FileNotFoundException("File not found - " + validatorFile);
            try {
                handler = new CustomHandler(name);
                XMLReader reader = XMLReaderFactory.createXMLReader(PARSER_NAME);
                reader.setContentHandler(handler);
                reader.setEntityResolver(new DefaultEntityResolver());
                reader.setErrorHandler(new DefaultErrorHandler());
                reader.parse(new InputSource(url.openStream()));
            } catch (SAXException e) {
                if (SUCCESS_MESSAGE.equals(e.getMessage()) && handler != null) break; else throw e;
            } catch (IOException e) {
                throw e;
            }
            if (handler.getFieldValidatorMetaData() != null) break;
        }
        c_cache.put(name, handler != null ? handler.getFieldValidatorMetaData() : null);
    }

    /** This will handle the SAX events raised while parsing the validators.xml file. */
    private static class CustomHandler extends DefaultHandler {

        private static final String FIELD_VALIDATOR = "field-validator";

        private static final String NAME = "name";

        private static final String DESCRIPTION = "description";

        private static final String CLASS = "class";

        private static final String MANDATORY = "mandatory";

        private static final String PARAM = "param";

        private static final String VALUE = "value";

        private String m_name = null;

        private FieldValidatorMetaData m_fieldValidatorMetaData = null;

        private String m_contents = null;

        /** Create an instance.
         */
        private CustomHandler(String name) {
            m_name = name;
        }

        /** Receive notification of the start of an element.
         * @param uri The uri.
         * @param sName The local name (without prefix), or the empty string if Namespace processing is not being performed.
         * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
         * @param atts The specified or defaulted attributes
         */
        public void startElement(String uri, String sName, String qName, Attributes atts) {
            if (m_fieldValidatorMetaData != null) {
                if (sName.equals(PARAM)) {
                    m_fieldValidatorMetaData.addParameter(atts.getValue(NAME), atts.getValue(VALUE));
                }
            }
        }

        /** Receive notification of the end of an element.
         * @param uri The uri.
         * @param sName The local name (without prefix), or the empty string if Namespace processing is not being performed.
         * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
         * @throws SAXException Any SAX exception, possibly wrapping another exception. NOTE: This exception will also be thrown after the named validator is found, so as to terminate any further parsing.
         */
        public void endElement(String uri, String sName, String qName) throws SAXException {
            if (sName.equals(NAME) && m_name.equals(m_contents)) {
                m_fieldValidatorMetaData = new FieldValidatorMetaData();
                m_fieldValidatorMetaData.setName(m_contents);
            } else if (m_fieldValidatorMetaData != null) {
                if (sName.equals(DESCRIPTION)) {
                    m_fieldValidatorMetaData.setDescription(m_contents);
                } else if (sName.equals(CLASS)) {
                    m_fieldValidatorMetaData.setClassName(m_contents);
                } else if (sName.equals(MANDATORY)) {
                    m_fieldValidatorMetaData.setMandatory(Parser.parseBoolean(m_contents).booleanValue());
                } else if (sName.equals(FIELD_VALIDATOR)) {
                    throw new SAXException(SUCCESS_MESSAGE);
                }
            }
        }

        /** Receive notification of character data inside an element.
         * @param ch The characters.
         * @param start The start position in the character array.
         * @param length The number of characters to use from the character array.
         */
        public void characters(char[] ch, int start, int length) {
            m_contents = new String(ch, start, length);
        }

        /** Returns the FieldValidatorMetaData for the given name. A null may be returned, if no matching definition was found.
         */
        private FieldValidatorMetaData getFieldValidatorMetaData() {
            return m_fieldValidatorMetaData;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println(getFieldValidatorMetaData("generic-foreign-key"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
