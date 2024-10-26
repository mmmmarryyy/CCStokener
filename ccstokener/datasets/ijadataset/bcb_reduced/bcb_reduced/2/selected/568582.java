package oracle.toplink.essentials.platform.xml.jaxp;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import oracle.toplink.essentials.platform.xml.XMLParser;
import oracle.toplink.essentials.platform.xml.XMLPlatformException;

public class JAXPParser implements XMLParser {

    private static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    private DocumentBuilderFactory documentBuilderFactory;

    private EntityResolver entityResolver;

    private ErrorHandler errorHandler;

    public JAXPParser() {
        super();
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        setNamespaceAware(true);
        setWhitespacePreserving(false);
    }

    public void setNamespaceAware(boolean isNamespaceAware) {
        documentBuilderFactory.setNamespaceAware(isNamespaceAware);
    }

    public void setWhitespacePreserving(boolean isWhitespacePreserving) {
        documentBuilderFactory.setIgnoringElementContentWhitespace(!isWhitespacePreserving);
    }

    public int getValidationMode() {
        if (!documentBuilderFactory.isValidating()) {
            return XMLParser.NONVALIDATING;
        }
        try {
            if (null == documentBuilderFactory.getAttribute(SCHEMA_LANGUAGE)) {
                return XMLParser.DTD_VALIDATION;
            }
        } catch (IllegalArgumentException e) {
            return XMLParser.DTD_VALIDATION;
        }
        return XMLParser.SCHEMA_VALIDATION;
    }

    public void setValidationMode(int validationMode) {
        switch(validationMode) {
            case XMLParser.NONVALIDATING:
                {
                    documentBuilderFactory.setValidating(false);
                    return;
                }
            case XMLParser.DTD_VALIDATION:
                {
                    documentBuilderFactory.setValidating(true);
                    return;
                }
            case XMLParser.SCHEMA_VALIDATION:
                {
                    try {
                        documentBuilderFactory.setAttribute(SCHEMA_LANGUAGE, XML_SCHEMA);
                        documentBuilderFactory.setValidating(true);
                    } catch (IllegalArgumentException e) {
                    }
                    return;
                }
        }
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setXMLSchema(URL url) throws XMLPlatformException {
        if (null == url) {
            return;
        }
        try {
            documentBuilderFactory.setAttribute(SCHEMA_LANGUAGE, XML_SCHEMA);
            documentBuilderFactory.setAttribute(JAXP_SCHEMA_SOURCE, url.toString());
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            XMLPlatformException.xmlPlatformErrorResolvingXMLSchema(url, e);
        }
    }

    public void setXMLSchemas(Object[] schemas) throws XMLPlatformException {
        if ((null == schemas) || (schemas.length == 0)) {
            return;
        }
        try {
            documentBuilderFactory.setAttribute(SCHEMA_LANGUAGE, XML_SCHEMA);
            documentBuilderFactory.setAttribute(JAXP_SCHEMA_SOURCE, schemas);
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            XMLPlatformException.xmlPlatformErrorResolvingXMLSchemas(schemas, e);
        }
    }

    public Document parse(InputSource inputSource) throws XMLPlatformException {
        try {
            return getDocumentBuilder().parse(inputSource);
        } catch (SAXException e) {
            throw XMLPlatformException.xmlPlatformParseException(e);
        } catch (IOException e) {
            throw XMLPlatformException.xmlPlatformParseException(e);
        }
    }

    public Document parse(File file) throws XMLPlatformException {
        try {
            return getDocumentBuilder().parse(file);
        } catch (SAXParseException e) {
            throw XMLPlatformException.xmlPlatformSAXParseException(e);
        } catch (SAXException e) {
            throw XMLPlatformException.xmlPlatformParseException(e);
        } catch (IOException e) {
            throw XMLPlatformException.xmlPlatformFileNotFoundException(file, e);
        }
    }

    public Document parse(InputStream inputStream) throws XMLPlatformException {
        try {
            return getDocumentBuilder().parse(inputStream);
        } catch (SAXParseException e) {
            throw XMLPlatformException.xmlPlatformSAXParseException(e);
        } catch (SAXException e) {
            throw XMLPlatformException.xmlPlatformParseException(e);
        } catch (IOException e) {
            throw XMLPlatformException.xmlPlatformParseException(e);
        }
    }

    public Document parse(Reader reader) throws XMLPlatformException {
        InputSource inputSource = new InputSource(reader);
        return parse(inputSource);
    }

    public Document parse(Source source) throws XMLPlatformException {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMResult domResult = new DOMResult();
            transformer.transform(source, domResult);
            return (Document) domResult.getNode();
        } catch (TransformerException e) {
            throw XMLPlatformException.xmlPlatformParseException(e);
        }
    }

    public Document parse(URL url) throws XMLPlatformException {
        try {
            InputStream inputStream = url.openStream();
            return parse(inputStream);
        } catch (IOException e) {
            throw XMLPlatformException.xmlPlatformParseException(e);
        }
    }

    private DocumentBuilder getDocumentBuilder() {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            documentBuilder.setEntityResolver(entityResolver);
            documentBuilder.setErrorHandler(errorHandler);
            return documentBuilder;
        } catch (ParserConfigurationException e) {
            throw XMLPlatformException.xmlPlatformParseException(e);
        }
    }
}
