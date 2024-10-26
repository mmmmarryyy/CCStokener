package org.j3d.loaders.stl;

import java.net.URL;
import java.net.URLConnection;
import java.awt.Component;
import java.io.*;
import javax.swing.ProgressMonitorInputStream;

/**
 * Class to parse STL (stereolithography) files in binary format.<p>
 * @see STLFileReader
 * @see STLLoader
 * @author  Dipl. Ing. Paul Szawlowski -
 *          University of Vienna, Dept of Medical Computer Sciences
 * @version $Revision: 1.2 $
 */
class STLBinaryParser extends STLParser {

    /**
     * size of binary header
     */
    private static final int HEADER_SIZE = 84;

    /**
     * size of one facet record in binary format
     */
    private static final int RECORD_SIZE = 50;

    /**
     * size of comments in header
     */
    private static final int COMMENT_SIZE = 80;

    private BufferedInputStream itsStream;

    private final byte[] itsReadBuffer = new byte[48];

    private final int[] itsDataBuffer = new int[12];

    public STLBinaryParser() {
    }

    public void close() throws IOException {
        if (itsStream != null) {
            itsStream.close();
        }
    }

    public boolean parse(final URL url) throws IOException {
        InputStream stream = null;
        int length = -1;
        try {
            final URLConnection connection = url.openConnection();
            stream = connection.getInputStream();
            length = connection.getContentLength();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
        }
        itsStream = new BufferedInputStream(stream);
        try {
            for (int i = 0; i < COMMENT_SIZE; i++) {
                itsStream.read();
            }
            itsNumOfObjects = 1;
            itsNumOfFacets = new int[] { LittleEndianConverter.read4ByteBlock(itsStream) };
            itsNames = new String[1];
            if (length != -1) {
                if (length != itsNumOfFacets[0] * RECORD_SIZE + HEADER_SIZE) {
                    throw new IOException("File size does not match.");
                }
            }
        } catch (IOException e) {
            close();
            throw e;
        }
        return false;
    }

    public boolean parse(final URL url, final Component parentComponent) throws InterruptedIOException, IOException {
        InputStream stream = null;
        int length = -1;
        try {
            final URLConnection connection = url.openConnection();
            stream = connection.getInputStream();
            length = connection.getContentLength();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
        }
        stream = new ProgressMonitorInputStream(parentComponent, "parsing " + url.toString(), stream);
        itsStream = new BufferedInputStream(stream);
        try {
            for (int i = 0; i < COMMENT_SIZE; i++) {
                itsStream.read();
            }
            itsNumOfObjects = 1;
            itsNumOfFacets = new int[] { LittleEndianConverter.read4ByteBlock(itsStream) };
            itsNames = new String[1];
            if (length != -1) {
                if (length != itsNumOfFacets[0] * RECORD_SIZE + HEADER_SIZE) {
                    throw new IOException("File size does not match.");
                }
            }
        } catch (IOException e) {
            close();
            throw e;
        }
        return false;
    }

    public boolean getNextFacet(final double[] normal, double[][] vertices) throws InterruptedIOException, IOException {
        LittleEndianConverter.read(itsReadBuffer, itsDataBuffer, 0, 12, itsStream);
        for (int i = 0; i < 3; i++) {
            normal[i] = Float.intBitsToFloat(itsDataBuffer[i]);
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                vertices[i][j] = Float.intBitsToFloat(itsDataBuffer[i * 3 + j + 3]);
            }
        }
        itsStream.read();
        itsStream.read();
        return true;
    }
}
