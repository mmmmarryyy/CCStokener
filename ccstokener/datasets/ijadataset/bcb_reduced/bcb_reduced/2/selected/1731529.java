package org.apache.myfaces.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.ExternalContext;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.myfaces.shared_impl.util.ClassUtils;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class ConfigFilesXmlValidationUtils {

    public static final LSResourceResolver JAVAEE_5_LS_RESOURCE_RESOLVER = new ValidatorLSResourceResolver();

    public static final ErrorHandler VALIDATION_ERROR_HANDLER = new ValidationErrorHandler();

    private static final String FACES_CONFIG_SCHEMA_PATH_12 = "org/apache/myfaces/resource/web-facesconfig_1_2.xsd";

    private static final String FACES_CONFIG_SCHEMA_PATH_20 = "org/apache/myfaces/resource/web-facesconfig_2_0.xsd";

    private static final String FACES_TAGLIB_SCHEMA_PATH = "org/apache/myfaces/resource/web-facelettaglibrary_2_0.xsd";

    public static class LSInputImpl implements LSInput {

        private final String _publicId;

        private final String _systemId;

        private final String _baseURI;

        private final InputStream _input;

        public LSInputImpl(String publicId, String systemId, String baseURI, InputStream input) {
            super();
            _publicId = publicId;
            _systemId = systemId;
            _baseURI = baseURI;
            _input = input;
        }

        public String getBaseURI() {
            return _baseURI;
        }

        public InputStream getByteStream() {
            return _input;
        }

        public boolean getCertifiedText() {
            return false;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public String getEncoding() {
            return null;
        }

        public String getPublicId() {
            return _publicId;
        }

        public String getStringData() {
            return null;
        }

        public String getSystemId() {
            return _systemId;
        }

        public void setBaseURI(String baseURI) {
        }

        public void setByteStream(InputStream byteStream) {
        }

        public void setCertifiedText(boolean certifiedText) {
        }

        public void setCharacterStream(Reader characterStream) {
        }

        public void setEncoding(String encoding) {
        }

        public void setPublicId(String publicId) {
        }

        public void setStringData(String stringData) {
        }

        public void setSystemId(String systemId) {
        }
    }

    public static class ValidatorLSResourceResolver implements LSResourceResolver {

        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            if ("http://www.w3.org/TR/REC-xml".equals(type) && "datatypes.dtd".equals(systemId)) {
                return new LSInputImpl(publicId, systemId, baseURI, ClassUtils.getResourceAsStream("org/apache/myfaces/resource/datatypes.dtd"));
            }
            if ("-//W3C//DTD XMLSCHEMA 200102//EN".equals(publicId) && "XMLSchema.dtd".equals(systemId)) {
                return new LSInputImpl(publicId, systemId, baseURI, ClassUtils.getResourceAsStream("org/apache/myfaces/resource/XMLSchema.dtd"));
            }
            if ("http://java.sun.com/xml/ns/javaee".equals(namespaceURI)) {
                if ("javaee_5.xsd".equals(systemId)) {
                    return new LSInputImpl(publicId, systemId, baseURI, ClassUtils.getResourceAsStream("org/apache/myfaces/resource/javaee_5.xsd"));
                } else if ("javaee_web_services_client_1_2.xsd".equals(systemId)) {
                    return new LSInputImpl(publicId, systemId, baseURI, ClassUtils.getResourceAsStream("org/apache/myfaces/resource/javaee_web_services_client_1_2.xsd"));
                }
            }
            if ("http://www.w3.org/XML/1998/namespace".equals(namespaceURI)) {
                return new LSInputImpl(publicId, systemId, baseURI, ClassUtils.getResourceAsStream("org/apache/myfaces/resource/xml.xsd"));
            }
            return null;
        }
    }

    public static class ValidationErrorHandler implements ErrorHandler {

        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void error(SAXParseException exception) throws SAXException {
            Logger log = Logger.getLogger(ConfigFilesXmlValidationUtils.class.getName());
            log.log(Level.SEVERE, exception.getMessage(), exception);
        }

        public void warning(SAXParseException exception) throws SAXException {
            Logger log = Logger.getLogger(ConfigFilesXmlValidationUtils.class.getName());
            log.log(Level.WARNING, exception.getMessage(), exception);
        }
    }

    public static void validateFacesConfigFile(URL xmlFile, ExternalContext externalContext, String version) throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = getFacesConfigSchemaFileAsSource(externalContext, version);
        if (schemaFile == null) {
            throw new IOException("Could not find schema file for validation.");
        }
        schemaFactory.setResourceResolver(JAVAEE_5_LS_RESOURCE_RESOLVER);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        URLConnection conn = xmlFile.openConnection();
        conn.setUseCaches(false);
        InputStream is = conn.getInputStream();
        Source source = new StreamSource(is);
        validator.setErrorHandler(VALIDATION_ERROR_HANDLER);
        validator.validate(source);
    }

    private static Source getFacesConfigSchemaFileAsSource(ExternalContext externalContext, String version) {
        String xmlSchema = "1.2".equals(version) ? FACES_CONFIG_SCHEMA_PATH_12 : FACES_CONFIG_SCHEMA_PATH_20;
        InputStream stream = ClassUtils.getResourceAsStream(xmlSchema);
        if (stream == null) {
            stream = externalContext.getResourceAsStream(xmlSchema);
        }
        if (stream == null) {
            return null;
        }
        return new StreamSource(stream);
    }

    public static final String getFacesConfigVersion(URL url) {
        URLConnection conn = null;
        InputStream input = null;
        String result = "2.0";
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser;
            FacesConfigVersionCheckHandler handler = new FacesConfigVersionCheckHandler();
            factory.setNamespaceAware(false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setValidating(false);
            parser = factory.newSAXParser();
            conn = url.openConnection();
            conn.setUseCaches(false);
            input = conn.getInputStream();
            try {
                parser.parse(input, handler);
            } catch (SAXException e) {
            }
            result = handler.isVersion20OrLater() ? "2.0" : (handler.isVersion12() ? "1.2" : "1.1");
        } catch (Throwable e) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable e) {
                }
            }
        }
        return result;
    }

    private static class FacesConfigVersionCheckHandler extends DefaultHandler {

        private boolean version12;

        private boolean version20OrLater;

        public boolean isVersion12() {
            return this.version12;
        }

        public boolean isVersion20OrLater() {
            return this.version20OrLater;
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            if (name.equals("faces-config")) {
                int length = attributes.getLength();
                for (int i = 0; i < length; i++) {
                    String attrName = attributes.getLocalName(i);
                    attrName = (attrName != null) ? ((attrName.length() > 0) ? attrName : attributes.getQName(i)) : attributes.getQName(i);
                    if (attrName.equals("version")) {
                        if (attributes.getValue(i).equals("1.2")) {
                            this.version12 = true;
                            this.version20OrLater = false;
                        } else {
                            this.version20OrLater = true;
                            this.version12 = false;
                        }
                    }
                }
            }
        }
    }

    public static void validateFaceletTagLibFile(URL xmlFile, ExternalContext externalContext, String version) throws SAXException, IOException, ParserConfigurationException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = getFaceletSchemaFileAsSource(externalContext);
        if (schemaFile == null) {
            throw new IOException("Could not find schema file for validation.");
        }
        schemaFactory.setResourceResolver(ConfigFilesXmlValidationUtils.JAVAEE_5_LS_RESOURCE_RESOLVER);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        URLConnection conn = xmlFile.openConnection();
        conn.setUseCaches(false);
        InputStream is = conn.getInputStream();
        Source source = new StreamSource(is);
        validator.setErrorHandler(VALIDATION_ERROR_HANDLER);
        validator.validate(source);
    }

    private static Source getFaceletSchemaFileAsSource(ExternalContext externalContext) {
        InputStream stream = ClassUtils.getResourceAsStream(FACES_TAGLIB_SCHEMA_PATH);
        if (stream == null) {
            stream = externalContext.getResourceAsStream(FACES_TAGLIB_SCHEMA_PATH);
        }
        if (stream == null) {
            return null;
        }
        return new StreamSource(stream);
    }

    public static final String getFaceletTagLibVersion(URL url) {
        if (isTaglibDocument20OrLater(url)) {
            return "2.0";
        } else {
            return "1.0";
        }
    }

    private static final boolean isTaglibDocument20OrLater(URL url) {
        URLConnection conn = null;
        InputStream input = null;
        boolean result = false;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser;
            VersionCheckHandler handler = new VersionCheckHandler();
            factory.setNamespaceAware(false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setValidating(false);
            parser = factory.newSAXParser();
            conn = url.openConnection();
            conn.setUseCaches(false);
            input = conn.getInputStream();
            try {
                parser.parse(input, handler);
            } catch (SAXException e) {
            }
            result = handler.isVersion20OrLater();
        } catch (Throwable e) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable e) {
                }
            }
        }
        return result;
    }

    private static class VersionCheckHandler extends DefaultHandler {

        private boolean version20OrLater;

        public boolean isVersion20OrLater() {
            return this.version20OrLater;
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            if (name.equals("facelet-taglib")) {
                int length = attributes.getLength();
                for (int i = 0; i < length; i++) {
                    String attrName = attributes.getLocalName(i);
                    attrName = (attrName != null) ? ((attrName.length() > 0) ? attrName : attributes.getQName(i)) : attributes.getQName(i);
                    if (attrName.equals("version")) {
                        this.version20OrLater = true;
                    }
                }
            }
        }
    }
}
