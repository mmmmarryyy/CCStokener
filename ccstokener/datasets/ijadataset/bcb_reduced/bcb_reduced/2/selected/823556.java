package net.sf.gridarta.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Little helper class for XML, holds a {@link DocumentBuilder} and an {@link
 * XPath} that are setup for Gridarta.
 * @author Andreas Kirschbaum
 */
public class XmlHelper {

    /**
     * The {@link Pattern} for matching the directory part of a system ID.
     */
    @NotNull
    private static final Pattern PATTERN_DIRECTORY_PART = Pattern.compile(".*/");

    /**
     * DocumentBuilder.
     */
    private final DocumentBuilder documentBuilder;

    /**
     * The XPath for using XPath.
     */
    private final XPath xpath;

    /**
     * Initialize the XML engine.
     * @throws ParserConfigurationException in case the xml parser couldn't be
     * set up
     */
    public XmlHelper() throws ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setCoalescing(true);
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);
        dbf.setValidating(true);
        dbf.setXIncludeAware(true);
        documentBuilder = dbf.newDocumentBuilder();
        documentBuilder.setEntityResolver(new GridartaEntityResolver());
        xpath = XPathFactory.newInstance().newXPath();
    }

    /**
     * Return the DocumentBuilder.
     * @return the document builder
     */
    public DocumentBuilder getDocumentBuilder() {
        return documentBuilder;
    }

    /**
     * Return the XPath for using XPath.
     * @return the XPath
     */
    public XPath getXPath() {
        return xpath;
    }

    /**
     * Implements an {@link EntityResolver} for looking up built-in .dtd files.
     */
    private static class GridartaEntityResolver implements EntityResolver {

        @Nullable
        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) throws IOException {
            if (systemId.endsWith(".xml")) {
                return null;
            }
            InputSource inputSource = null;
            final URL url = IOUtils.getResource(new File("system/dtd"), PATTERN_DIRECTORY_PART.matcher(systemId).replaceAll(""));
            final InputStream inputStream = url.openStream();
            try {
                final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                try {
                    inputSource = new InputSource(bufferedInputStream);
                } finally {
                    if (inputSource == null) {
                        bufferedInputStream.close();
                    }
                }
            } finally {
                if (inputSource == null) {
                    inputStream.close();
                }
            }
            return inputSource;
        }
    }
}
