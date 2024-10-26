package org.j3d.loaders.stl;

import java.net.URL;
import java.net.URLConnection;
import java.awt.Component;
import java.io.*;
import java.util.*;
import javax.swing.ProgressMonitorInputStream;

/**
 * Class to parse STL (stereolithography) files in ASCII format.<p>
 * @see STLFileReader
 * @see STLLoader
 * @author  Dipl. Ing. Paul Szawlowski -
 *          University of Vienna, Dept of Medical Computer Sciences
 * @version $Revision: 1.2 $
 */
class STLASCIIParser extends STLParser {

    private BufferedReader itsReader;

    private StreamTokenizer itsTokenizer;

    public STLASCIIParser() {
    }

    public void close() throws IOException {
        if (itsReader != null) {
            itsReader.close();
        }
    }

    public boolean getNextFacet(final double[] normal, double[][] vertices) throws IOException {
        int type = itsTokenizer.nextToken();
        if (type == StreamTokenizer.TT_EOF) {
            close();
            throw new IOException("Unexpected EOF");
        } else if (type == StreamTokenizer.TT_WORD) {
            if (itsTokenizer.sval.indexOf("s") >= 0) {
                skipObjectName(itsTokenizer);
                type = itsTokenizer.nextToken();
                if (type == StreamTokenizer.TT_EOF) {
                    close();
                    return false;
                }
                skipObjectName(itsTokenizer);
            } else {
                itsTokenizer.pushBack();
            }
            readVector(itsTokenizer, normal);
            for (int i = 0; i < 3; i++) {
                readVector(itsTokenizer, vertices[i]);
            }
            return true;
        } else {
            close();
            throw new IOException("Unexpected data found");
        }
    }

    public boolean parse(final URL url, final Component parentComponent) throws InterruptedIOException, IOException {
        InputStream stream = null;
        try {
            stream = url.openStream();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
            throw e;
        }
        stream = new ProgressMonitorInputStream(parentComponent, "analyzing " + url.toString(), stream);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        boolean isAscii = false;
        try {
            isAscii = parse(reader);
        } finally {
            reader.close();
        }
        if (isAscii) {
            try {
                stream = url.openStream();
            } catch (IOException e) {
                stream.close();
                throw e;
            }
            stream = new ProgressMonitorInputStream(parentComponent, "parsing " + url.toString(), stream);
            reader = new BufferedReader(new InputStreamReader(stream));
            try {
                configureTokenizer(reader);
            } catch (IOException e) {
                reader.close();
                throw e;
            }
            itsReader = reader;
        }
        return isAscii;
    }

    public boolean parse(final URL url) throws IOException {
        InputStream stream = null;
        try {
            stream = url.openStream();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
            throw e;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        boolean isAscii = false;
        try {
            isAscii = parse(reader);
        } catch (InterruptedIOException e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }
        if (isAscii) {
            try {
                stream = url.openStream();
            } catch (IOException e) {
                stream.close();
                throw e;
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            try {
                configureTokenizer(reader);
            } catch (IOException e) {
                reader.close();
                throw e;
            }
            itsReader = reader;
        }
        return isAscii;
    }

    private boolean parse(final BufferedReader reader) throws InterruptedIOException, IOException {
        int numOfObjects = 0;
        int numOfFacets = 0;
        final ArrayList facetsPerObject = new ArrayList(10);
        final ArrayList names = new ArrayList(10);
        boolean isAscii = true;
        String line = reader.readLine();
        if (line.indexOf("solid") < 0) {
            return false;
        }
        while (line != null) {
            if (line.indexOf("facet") >= 0) {
                numOfFacets++;
                for (int i = 0; i < 6; i++) {
                    reader.readLine();
                }
            } else if (line.indexOf("endsolid") >= 0) {
                facetsPerObject.add(new Integer(numOfFacets));
                numOfFacets = 0;
                numOfObjects++;
            } else if (line.indexOf("solid") >= 0) {
                names.add(line.substring(6));
            } else {
                return false;
            }
            line = reader.readLine();
        }
        itsNumOfObjects = numOfObjects;
        itsNumOfFacets = new int[numOfObjects];
        itsNames = new String[numOfObjects];
        for (int i = 0; i < numOfObjects; i++) {
            final Integer num = (Integer) facetsPerObject.get(i);
            itsNumOfFacets[i] = num.intValue();
            itsNames[i] = (String) names.get(i);
        }
        return true;
    }

    /**
     * Set the <code>BufferedReader</code> object for reading the facet data.
     */
    private void configureTokenizer(final BufferedReader reader) throws IOException {
        reader.readLine();
        itsTokenizer = new StreamTokenizer(reader);
        itsTokenizer.resetSyntax();
        itsTokenizer.wordChars('0', '9');
        itsTokenizer.wordChars('E', 'E');
        itsTokenizer.wordChars('+', '+');
        itsTokenizer.wordChars('-', '-');
        itsTokenizer.wordChars('.', '.');
        itsTokenizer.wordChars('s', 's');
        itsTokenizer.whitespaceChars(0, ' ');
        itsTokenizer.whitespaceChars('a', 'r');
        itsTokenizer.whitespaceChars('t', 'z');
    }

    private void readVector(final StreamTokenizer tokenizer, final double[] vector) throws IOException {
        for (int i = 0; i < 3; i++) {
            final int type = tokenizer.nextToken();
            if (type == StreamTokenizer.TT_WORD) {
                try {
                    vector[i] = Double.parseDouble(tokenizer.sval);
                } catch (NumberFormatException e) {
                    throw new IOException("Unexpected data found");
                }
            } else if (type == StreamTokenizer.TT_EOF) {
                throw new IOException("Unexpected EOF");
            } else {
                throw new IOException("Unexpected data found.");
            }
        }
    }

    private void skipObjectName(final StreamTokenizer tokenizer) throws IOException {
        itsTokenizer.eolIsSignificant(true);
        int type = 0;
        while (type != StreamTokenizer.TT_EOL) {
            type = itsTokenizer.nextToken();
        }
        itsTokenizer.eolIsSignificant(false);
    }
}
