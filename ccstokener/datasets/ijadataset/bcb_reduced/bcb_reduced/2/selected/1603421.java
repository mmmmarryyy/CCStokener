package org.eclipse.swordfish.tooling.ui.wizards;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * 
 */
public class RegistryAccess {

    private static final Log LOG = LogFactory.getLog(RegistryAccess.class);

    private URL registryUrl;

    private WSDLFactory wsdlFactory;

    /**
	 * @throws WSDLException *
	 * @throws MalformedURLException	 * 
	 * @param registryUrl *
	 */
    public RegistryAccess(String registryUrl) throws WSDLException, MalformedURLException {
        if (!registryUrl.endsWith("/")) {
            this.registryUrl = new URL(registryUrl + "/");
        } else {
            this.registryUrl = new URL(registryUrl);
        }
        this.wsdlFactory = WSDLFactory.newInstance();
    }

    /**
	 * @throws Exception * 
	 * @return Map<QName, URL> *
	 */
    @SuppressWarnings("unchecked")
    public Map<QName, URL> getRegisteredServices() throws Exception {
        Map<QName, URL> services = new HashMap<QName, URL>();
        Iterable<String> urls;
        try {
            urls = getWSDLUrls();
        } catch (IOException e) {
            throw new IOException("Cannot access service registry at " + registryUrl.toString());
        } catch (SAXException e) {
            throw new Exception("Invalid response from service registry at " + registryUrl.toString(), e);
        }
        for (String url : urls) {
            InputStream is = null;
            URL fullUrl = null;
            try {
                fullUrl = new URL(registryUrl, url);
                is = fullUrl.openStream();
                WSDLReader wr = wsdlFactory.newWSDLReader();
                Definition wsdl = wr.readWSDL(null, new InputSource(is));
                Map<QName, PortType> portTypes = (Map<QName, PortType>) wsdl.getPortTypes();
                for (QName qName : portTypes.keySet()) {
                    services.put(qName, fullUrl);
                }
            } catch (IOException e) {
                LOG.warn("WSDL not accessible at " + fullUrl.toString() + ", skipping.", e);
            } catch (WSDLException e) {
                LOG.warn("WSDL not valid at " + fullUrl.toString() + ", skipping.", e);
            } finally {
                is.close();
            }
        }
        return services;
    }

    private Iterable<String> getWSDLUrls() throws IOException, SAXException {
        URLConnection urlConnection = registryUrl.openConnection();
        urlConnection.addRequestProperty("accept", "application/xml");
        InputStream is = urlConnection.getInputStream();
        RegistryResponseParser parser = new RegistryResponseParser();
        parser.parse(is);
        return parser.getUrls();
    }

    /**
	 * 
	 */
    class RegistryResponseParser implements ContentHandler {

        private Set<String> urls = new HashSet<String>();

        int state = 0;

        StringBuffer strbuf = null;

        public void parse(InputStream is) throws SAXException, IOException {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(this);
            xr.parse(new InputSource(is));
        }

        public Iterable<String> getUrls() {
            return urls;
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (state == 3) {
                strbuf = new StringBuffer();
                strbuf.append(new String(ch, start, length));
            }
        }

        public void endDocument() throws SAXException {
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (state == 3 && "url".equals(localName)) {
                urls.add(strbuf.toString());
                strbuf = null;
                state = 2;
            }
            if (state == 2 && "wsdlList".equals(localName)) {
                state = 1;
            }
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void ignorableWhitespace(char[] ch, int start, int length) {
        }

        public void processingInstruction(String target, String data) throws SAXException {
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void skippedEntity(String name) throws SAXException {
        }

        public void startDocument() throws SAXException {
            state = 1;
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (state == 1 && "wsdlList".equals(localName)) {
                state = 2;
            }
            if (state == 2 && "url".equals(localName)) {
                state = 3;
            }
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }
    }
}
