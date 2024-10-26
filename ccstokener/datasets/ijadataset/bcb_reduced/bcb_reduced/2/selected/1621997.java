package org.apache.batik.transcoder.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.batik.transcoder.TranscoderInput;

/**
 * Test the ImageTranscoder input with a InputStream.
 *
 * @author <a href="mailto:Thierry.Kormann@sophia.inria.fr">Thierry Kormann</a>
 * @version $Id: InputStreamTest.java 475685 2006-11-16 11:16:05Z cam $
 */
public class InputStreamTest extends AbstractImageTranscoderTest {

    /** The URI of the input image. */
    protected String inputURI;

    /** The URI of the reference image. */
    protected String refImageURI;

    /**
     * Constructs a new <tt>InputStreamTest</tt>.
     *
     * @param inputURI the URI of the input image
     * @param refImageURI the URI of the reference image
     */
    public InputStreamTest(String inputURI, String refImageURI) {
        this.inputURI = inputURI;
        this.refImageURI = refImageURI;
    }

    /**
     * Creates the <tt>TranscoderInput</tt>.
     */
    protected TranscoderInput createTranscoderInput() {
        try {
            URL url = resolveURL(inputURI);
            InputStream istream = url.openStream();
            TranscoderInput input = new TranscoderInput(istream);
            input.setURI(url.toString());
            return input;
        } catch (IOException ex) {
            throw new IllegalArgumentException(inputURI);
        }
    }

    /**
     * Returns the reference image for this test.
     */
    protected byte[] getReferenceImageData() {
        return createBufferedImageData(resolveURL(refImageURI));
    }
}
