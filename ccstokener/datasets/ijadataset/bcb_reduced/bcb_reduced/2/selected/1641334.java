package org.apache.batik.transcoder.image;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.apache.batik.transcoder.TranscoderInput;

/**
 * Test the ImageTranscoder input with a Reader.
 *
 * @author <a href="mailto:Thierry.Kormann@sophia.inria.fr">Thierry Kormann</a>
 * @version $Id: ReaderTest.java 475685 2006-11-16 11:16:05Z cam $
 */
public class ReaderTest extends AbstractImageTranscoderTest {

    /** The URI of the input image. */
    protected String inputURI;

    /** The URI of the reference image. */
    protected String refImageURI;

    /**
     * Constructs a new <tt>ReaderTest</tt>.
     *
     * @param inputURI the URI of the input image
     * @param refImageURI the URI of the reference image
     */
    public ReaderTest(String inputURI, String refImageURI) {
        this.inputURI = inputURI;
        this.refImageURI = refImageURI;
    }

    /**
     * Creates the <tt>TranscoderInput</tt>.
     */
    protected TranscoderInput createTranscoderInput() {
        try {
            URL url = resolveURL(inputURI);
            Reader reader = new InputStreamReader(url.openStream());
            TranscoderInput input = new TranscoderInput(reader);
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
