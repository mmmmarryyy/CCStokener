package com.volantis.xml.utilities.sax;

import com.volantis.xml.utilities.sax.stream.AddRootElementInputStream;
import com.volantis.xml.utilities.sax.stream.AddRootElementReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import org.xml.sax.InputSource;

/**
 * Takes and InputStream or systemID and uses the AddRootElementInputStream to
 * wrap a root element around it
 */
public class DocumentFragmentInputSource extends InputSource {

    /**
     * Store the namespaces that will be added to the outermost element
     */
    private Map namespaces = null;

    /**
     * Open a URL to the specified system id
     *
     * @param systemId
     */
    public DocumentFragmentInputSource(String systemId) throws IOException {
        InputStream urlStream = new URL(systemId).openStream();
        this.setByteStream(new AddRootElementInputStream(urlStream));
        this.setSystemId(systemId);
    }

    /**
     * Wrapped the specified input source in an AddRootElementInputStream
     *
     * @param inputSource
     */
    public DocumentFragmentInputSource(InputSource inputSource) throws IOException {
        if (inputSource.getByteStream() != null) {
            this.setByteStream(new AddRootElementInputStream(inputSource.getByteStream()));
        } else if (inputSource.getSystemId() != null) {
            InputStream urlStream = new URL(inputSource.getSystemId()).openStream();
            this.setByteStream(new AddRootElementInputStream(urlStream));
            this.setSystemId(inputSource.getSystemId());
        } else {
            this.setCharacterStream(new AddRootElementReader(inputSource.getCharacterStream()));
        }
    }

    /**
     * Open a URL to the specified system id
     *
     * @param systemId
     * @param namespacePrefix the prefix for the "fragment" element
     * @param namespaces a map of prefix->url namespaces to be applied to the
     *                   "fragment" element. A prefix of "" indicated teh
     *                   default namespace
     */
    public DocumentFragmentInputSource(String systemId, String namespacePrefix, Map namespaces) throws IOException {
        InputStream urlStream = new URL(systemId).openStream();
        this.setByteStream(new AddRootElementInputStream(urlStream, namespacePrefix, namespaces));
        this.setSystemId(systemId);
    }

    /**
     * Wrapped the specified input source in an AddRootElementInputStream
     *
     * @param inputSource
     * @param namespaces  a map of prefix->url namespaces to be applied to the
     *                    "fragment" element. A prefix of "" indicated teh
     *                    default namespace
     */
    public DocumentFragmentInputSource(InputSource inputSource, String namespacePrefix, Map namespaces) throws IOException {
        if (inputSource.getByteStream() != null) {
            this.setByteStream(new AddRootElementInputStream(inputSource.getByteStream(), namespacePrefix, namespaces));
        } else if (inputSource.getSystemId() != null) {
            InputStream urlStream = new URL(inputSource.getSystemId()).openStream();
            this.setByteStream(new AddRootElementInputStream(urlStream, namespacePrefix, namespaces));
            this.setSystemId(inputSource.getSystemId());
        } else {
            this.setCharacterStream(new AddRootElementReader(inputSource.getCharacterStream(), namespacePrefix, namespaces));
        }
    }
}
