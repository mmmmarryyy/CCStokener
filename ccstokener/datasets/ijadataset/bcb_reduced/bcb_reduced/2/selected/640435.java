package org.apache.batik.transcoder;

import com.google.code.appengine.awt.Toolkit;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.keys.BooleanKey;
import org.apache.batik.transcoder.keys.FloatKey;
import org.apache.batik.transcoder.keys.IntegerKey;
import org.apache.batik.util.SVGConstants;
import org.xml.sax.XMLFilter;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** This class allows to simplify the creation of a transcoder which transcodes to
 *  SVG content.
 *  <p>To use this class, you just have to implement the <i>transcode</i> method of
 *  the <i>AbstractTranscoder</i> class :
 *  <ul>
 *  <li>first get  the associated Document from the <i>TranscoderOutput</i> :
 *  {@link #createDocument(TranscoderOutput)}, then create a new
 *  {@link org.apache.batik.svggen.SVGGraphics2D} with this Document</li>
 *  <pre>
 *    Document doc = this.createDocument(output);
 *    svgGenerator = new SVGGraphics2D(doc);
 *  </pre>
 *  <li>Perform the effective transcoding, using the
 *  {@link org.apache.batik.svggen.SVGGraphics2D} previously created</li>
 *  <li>then call the
 *  {@link #writeSVGToOutput(SVGGraphics2D, Element, TranscoderOutput)} to create the
 *  effective output file (if the output is set to be a File or URI)</li>
 *  <pre>
 *    Element svgRoot = svgGenerator.getRoot();
 *    writeSVGToOutput(svgGenerator, svgRoot, output);
 *  </pre>
 *  </ul>
 *  </p>
 *
 *  <p>Several transcoding hints are defined for this abstract transcoder, but no default
 *  implementation is provided. Subclasses must implement which keys are relevant to them :</p>
 *  <ul>
 *  <li>KEY_INPUT_WIDTH, KEY_INPUT_HEIGHT, KEY_XOFFSET, KEY_YOFFSET : this Integer keys allows to
 *  set the  portion of the image to transcode, defined by the width, height, and offset
 *  of this portion in Metafile units.
 *  <li>KEY_ESCAPED : this Boolean ley allow to escape XML characters in the output</li>
 *  </ul>
 *  <pre>
 *     transcoder.addTranscodingHint(ToSVGAbstractTranscoder.KEY_INPUT_WIDTH, new Integer(input_width));
 *  </pre>
 *  </li>
 *  <li>KEY_WIDTH, KEY_HEIGHT : this Float values allows to force the width and height of the output:
 *  </ul>
 *  <pre>
 *     transcoder.addTranscodingHint(ToSVGAbstractTranscoder.KEY_WIDTH, new Float(width));
 *  </pre>
 *  </li>
 *  </li>
 *  </ul>
 *
 * @version $Id: ToSVGAbstractTranscoder.java 498740 2007-01-22 18:35:57Z dvholten $
 */
public abstract class ToSVGAbstractTranscoder extends AbstractTranscoder implements SVGConstants {

    public static float PIXEL_TO_MILLIMETERS;

    public static float PIXEL_PER_INCH;

    static {
        PIXEL_TO_MILLIMETERS = 25.4f / (float) Toolkit.getDefaultToolkit().getScreenResolution();
        PIXEL_PER_INCH = Toolkit.getDefaultToolkit().getScreenResolution();
    }

    public static final int TRANSCODER_ERROR_BASE = 0xff00;

    public static final int ERROR_NULL_INPUT = TRANSCODER_ERROR_BASE + 0;

    public static final int ERROR_INCOMPATIBLE_INPUT_TYPE = TRANSCODER_ERROR_BASE + 1;

    public static final int ERROR_INCOMPATIBLE_OUTPUT_TYPE = TRANSCODER_ERROR_BASE + 2;

    public static final TranscodingHints.Key KEY_WIDTH = new FloatKey();

    public static final TranscodingHints.Key KEY_HEIGHT = new FloatKey();

    public static final TranscodingHints.Key KEY_INPUT_WIDTH = new IntegerKey();

    public static final TranscodingHints.Key KEY_INPUT_HEIGHT = new IntegerKey();

    public static final TranscodingHints.Key KEY_XOFFSET = new IntegerKey();

    public static final TranscodingHints.Key KEY_YOFFSET = new IntegerKey();

    public static final TranscodingHints.Key KEY_ESCAPED = new BooleanKey();

    protected SVGGraphics2D svgGenerator;

    /** Create an empty Document from a TranscoderOutput.
     *  <ul>
     *  <li>If the TranscoderOutput already contains an empty Document : returns this
     *  Document</li>
     *  <li>else create a new empty DOM Document</li>
     *  </ul>
     */
    protected Document createDocument(TranscoderOutput output) {
        Document doc;
        if (output.getDocument() == null) {
            DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
            doc = domImpl.createDocument(SVG_NAMESPACE_URI, SVG_SVG_TAG, null);
        } else doc = output.getDocument();
        return doc;
    }

    /** Get the {@link org.apache.batik.svggen.SVGGraphics2D} associated
     *  with this transcoder.
     */
    public SVGGraphics2D getGraphics2D() {
        return svgGenerator;
    }

    /** Writes the SVG content held by the svgGenerator to the
     * <tt>TranscoderOutput</tt>. This method does nothing if the output already
     * contains a Document.
     */
    protected void writeSVGToOutput(SVGGraphics2D svgGenerator, Element svgRoot, TranscoderOutput output) throws TranscoderException {
        Document doc = output.getDocument();
        if (doc != null) return;
        XMLFilter xmlFilter = output.getXMLFilter();
        if (xmlFilter != null) {
            handler.fatalError(new TranscoderException("" + ERROR_INCOMPATIBLE_OUTPUT_TYPE));
        }
        try {
            boolean escaped = false;
            if (hints.containsKey(KEY_ESCAPED)) escaped = ((Boolean) hints.get(KEY_ESCAPED)).booleanValue();
            OutputStream os = output.getOutputStream();
            if (os != null) {
                svgGenerator.stream(svgRoot, new OutputStreamWriter(os), false, escaped);
                return;
            }
            Writer wr = output.getWriter();
            if (wr != null) {
                svgGenerator.stream(svgRoot, wr, false, escaped);
                return;
            }
            String uri = output.getURI();
            if (uri != null) {
                try {
                    URL url = new URL(uri);
                    URLConnection urlCnx = url.openConnection();
                    os = urlCnx.getOutputStream();
                    svgGenerator.stream(svgRoot, new OutputStreamWriter(os), false, escaped);
                    return;
                } catch (MalformedURLException e) {
                    handler.fatalError(new TranscoderException(e));
                } catch (IOException e) {
                    handler.fatalError(new TranscoderException(e));
                }
            }
        } catch (IOException e) {
            throw new TranscoderException(e);
        }
        throw new TranscoderException("" + ERROR_INCOMPATIBLE_OUTPUT_TYPE);
    }
}
