package com.hack23.cia.service.external.common.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.jdom.Document;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.Unmarshaller;

/**
 * The Class AbstractXmlAgentImpl.
 */
public abstract class AbstractXmlAgentImpl {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractXmlAgentImpl.class);

    /** The xml request number. */
    private long xmlRequestNumber = 1;

    /**
	 * Instantiates a new abstract xml agent impl.
	 */
    public AbstractXmlAgentImpl() {
        super();
    }

    /**
	 * Read with string buffer.
	 * 
	 * @param fr
	 *            the fr
	 * @return the string
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    String readWithStringBuffer(final Reader fr) throws IOException {
        final BufferedReader br = new BufferedReader(fr);
        String line;
        final StringBuffer result = new StringBuffer();
        while ((line = br.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    /**
	 * Retrive content.
	 * 
	 * @param accessUrl
	 *            the access url
	 * @return the string
	 * @throws Exception
	 *             the exception
	 */
    protected String retriveContent(final String accessUrl) throws Exception {
        final URL url = new URL(accessUrl);
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(url.openStream()));
        return readWithStringBuffer(inputStream);
    }

    /**
	 * Sets the name space on xml stream.
	 * 
	 * @param in
	 *            the in
	 * @param nameSpace
	 *            the name space
	 * @return the source
	 * @throws Exception
	 *             the exception
	 */
    public Source setNameSpaceOnXmlStream(final InputStream in, final String nameSpace) throws Exception {
        final SAXBuilder sb = new SAXBuilder(false);
        final Document doc = sb.build(in);
        doc.getRootElement().setNamespace(Namespace.getNamespace(nameSpace));
        return new JDOMSource(doc);
    }

    /**
	 * Unmarshall xml.
	 * 
	 * @param unmarshaller
	 *            the unmarshaller
	 * @param accessUrl
	 *            the access url
	 * @return the object
	 * @throws Exception
	 *             the exception
	 */
    protected Object unmarshallXml(final Unmarshaller unmarshaller, final String accessUrl) throws Exception {
        return unmarshallXml(unmarshaller, accessUrl, null, null, null);
    }

    /**
	 * Unmarshall xml.
	 * 
	 * @param unmarshaller
	 *            the unmarshaller
	 * @param accessUrl
	 *            the access url
	 * @param nameSpace
	 *            the name space
	 * @param replace
	 *            the replace
	 * @param with
	 *            the with
	 * @return the object
	 * @throws Exception
	 *             the exception
	 */
    protected Object unmarshallXml(final Unmarshaller unmarshaller, final String accessUrl, final String nameSpace, final String replace, final String with) throws Exception {
        final URL url = new URL(accessUrl);
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(url.openStream()));
        String xmlContent = readWithStringBuffer(inputStream);
        if (replace != null) {
            xmlContent = xmlContent.replace(replace, with);
        }
        LOGGER.info("Calls " + accessUrl);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\nXml:" + accessUrl + "\n" + xmlContent);
        }
        if (LOGGER.isDebugEnabled()) {
            final BufferedWriter out = new BufferedWriter(new FileWriter("target/XmlAgentLog" + xmlRequestNumber++ + ".txt"));
            out.write(xmlContent);
            out.close();
        }
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xmlContent.getBytes());
        Source source;
        if (nameSpace != null) {
            source = setNameSpaceOnXmlStream(byteArrayInputStream, nameSpace);
        } else {
            source = new StreamSource(byteArrayInputStream);
        }
        return unmarshaller.unmarshal(source);
    }
}
