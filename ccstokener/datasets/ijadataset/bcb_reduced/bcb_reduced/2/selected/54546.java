package nu.staldal.xodus;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.DeclHandler;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

/**
 * Serialize SAX2 events to its textual XML representation.
 *<p>
 * Support output to XML, XHTML, HTML and Text. Full support for XML namespaces.
 *<p>
 * The {@link #startDocument} and {@link #endDocument} method must be used.
 *<p>
 * Output properties controls the serialization process, it uses the keys in 
 * <code>javax.xml.transform.OutputKeys</code>.
 *<p>
 * The METHOD output property must be specified. It can be 
 * "xml", "xhtml", "text" or "html".
 *<p>
 * The CDATA_SECTION_ELEMENTS output property is not supported.
 *<p>
 * The ENCODING output property defaults to UTF-8 for XML and XHTML, 
 * and to ISO-8859-1 for Text and HTML.
 *<p>
 * The "html" output method does <em>not</em> escape non-ASCII characters 
 * in URI attribute values, as specified in the XSLT 1.0 specification.
 *<p>
 * <code>javax.xml.transform.Result.PI_DISABLE_OUTPUT_ESCAPING</code> 
 * and 
 * <code>javax.xml.transform.Result.PI_ENABLE_OUTPUT_ESCAPING</code>
 *  can be used as processingInstruction targets to disable output escaping. 
 */
public abstract class Serializer implements ContentHandler, LexicalHandler, DTDHandler, DeclHandler {

    protected final OutputConfig outputConfig;

    protected final String systemId;

    protected final XMLCharacterEncoder out;

    private final boolean doClose;

    /**
     * Factory method, create a new Serializer.
     *
     * @param result  specifies where to write the textual representation.
     * @param outputProperties output properties, uses {@link javax.xml.transform.OutputKeys}
     *
     * @return a new Serializer
     *
     * @throws UnsupportedEncodingException if the ENCODING output property 
     *         is invalid.
     * @throws IllegalArgumentException if any other output property is invalid.
     * @throws IOException if the result is invalid.
     */
    public static Serializer createSerializer(StreamResult result, Properties outputProperties) throws IllegalArgumentException, IOException, UnsupportedEncodingException {
        OutputConfig outputConfig = OutputConfig.createOutputConfig(outputProperties);
        if (outputConfig.isHtml) return new HTMLSerializer(result, outputConfig); else if (outputConfig.isXhtml) return new XMLSerializer(result, outputConfig); else if (outputConfig.isText) return new TextSerializer(result, outputConfig); else return new XMLSerializer(result, outputConfig);
    }

    protected Serializer(StreamResult result, OutputConfig outputConfig) throws IllegalArgumentException, IOException, UnsupportedEncodingException {
        this.outputConfig = outputConfig;
        if (!outputConfig.cdata_section_elements.isEmpty()) {
            throw new IllegalArgumentException("cdata_section_elements is not supported");
        }
        this.systemId = result.getSystemId();
        OutputStream os = result.getOutputStream();
        Writer w = result.getWriter();
        if (os != null) {
            out = new XMLCharacterEncoder(os, outputConfig.encoding);
            doClose = false;
        } else if (w != null) {
            out = new XMLCharacterEncoder(w);
            doClose = false;
        } else if (systemId != null) {
            OutputStream _os;
            try {
                URI uri = new URI(systemId);
                if (!uri.isAbsolute()) {
                    File file = new File(systemId);
                    _os = new FileOutputStream(file);
                } else if (uri.getScheme().equals("file")) {
                    File file = new File(uri);
                    _os = new FileOutputStream(file);
                } else {
                    URL url = uri.toURL();
                    URLConnection urlConn = url.openConnection();
                    urlConn.setDoOutput(true);
                    _os = urlConn.getOutputStream();
                }
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid systemId: " + e.getMessage());
            }
            os = new BufferedOutputStream(_os);
            out = new XMLCharacterEncoder(os, outputConfig.encoding);
            doClose = true;
        } else {
            throw new IllegalArgumentException("Empty StreamResult");
        }
    }

    /**
     * Finish writing to output. Does <em>not</em> close output if
     * an {@link java.io.OutputStream} or {@link Writer} was provided.
     */
    protected void finishOutput() throws IOException {
        out.finish();
        if (doClose) out.close();
    }

    /**
     * Write a newline.
     */
    protected void newline() throws IOException {
        out.write('\n');
    }
}
