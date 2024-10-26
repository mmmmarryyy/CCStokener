package lug.gui.gridbag;

import java.awt.GridBagConstraints;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Vector;
import org.apache.commons.lang.StringUtils;

/**
 *  Extension to the GridBagParser that uses a list of ConstraintLines for
 *  speedier access.
 */
public class AsciiParser extends GridBagParser {

    private String[] constraints = null;

    /**
     * Constructs an AsciiParser
     * @param lines the list of ConstraintLines.
     */
    public AsciiParser(String[] lines) {
        constraints = lines;
    }

    /**
     * Constructs an AsciiReader .
     * @param f file containing the constraints information.
     * @throws GridBagException
     */
    public AsciiParser(File f) throws GridBagException {
        try {
            FileReader freader = new FileReader(f);
            constraints = getLines(freader);
            freader.close();
        } catch (IOException ie1) {
            throw new GridBagException("Cannot read from source file.");
        }
    }

    /**
     * Construct an AsciiParser from a url.*/
    public AsciiParser(URL url) throws GridBagException {
        try {
            InputStream instream = url.openStream();
            constraints = getLines(instream);
            instream.close();
        } catch (IOException ie1) {
            throw new GridBagException("Cannot read from source Reader.");
        }
    }

    /**
     * Construct an AsciiParser from java.io.Reader.*/
    public AsciiParser(java.io.Reader reader) throws GridBagException {
        try {
            constraints = getLines(reader);
        } catch (IOException ie1) {
            throw new GridBagException("Cannot read from source Reader.");
        }
    }

    /**
     * Construct an AsciiParser from an InputStream.*/
    public AsciiParser(InputStream in) throws GridBagException {
        try {
            constraints = getLines(in);
        } catch (IOException ie1) {
            throw new GridBagException("Cannot read from source Reader.");
        }
    }

    /**
     * Construct an AsciiParser from an URI.*/
    public AsciiParser(String systemID) throws GridBagException {
        String id = systemID;
        if (id.endsWith(".xml")) {
            id = StringUtils.replace(id, ".xml", ".gbc");
        }
        ClassLoader loader = this.getClass().getClassLoader();
        URL url = loader.getResource(id);
        if (url == null) {
            throw new GridBagException("Cannot located resource : \"" + systemID + "\".");
        }
        try {
            InputStream inStream = url.openStream();
            constraints = getLines(inStream);
            inStream.close();
        } catch (IOException ie1) {
            throw new GridBagException("Cannot read from resource " + id);
        }
    }

    /**
     * Returns all lines from the given input stream.
     * @param inStream Stream to read from
     * @return All lines in the stream
     * @throws IOException
     */
    private String[] getLines(InputStream inStream) throws IOException {
        InputStreamReader inReader = new InputStreamReader(inStream);
        BufferedReader breader = new BufferedReader(inReader);
        String inline = null;
        Vector<String> v = new Vector<String>();
        while ((inline = breader.readLine()) != null) {
            v.add(inline);
        }
        breader.close();
        inReader.close();
        String[] res = v.toArray(new String[0]);
        v.clear();
        return res;
    }

    /**
     * Returns all lines from the given reader
     * @param inReader Reader to read from
     * @return All lines in the stream
     * @throws IOException
     */
    private String[] getLines(Reader inReader) throws IOException {
        BufferedReader breader = new BufferedReader(inReader);
        String inline = null;
        Vector<String> v = new Vector<String>();
        while ((inline = breader.readLine()) != null) {
            v.add(inline);
        }
        breader.close();
        String[] res = v.toArray(new String[0]);
        v.clear();
        return res;
    }

    /**
     * Returns the constraints for a given name.
     * @param name name of the component to get the GridBagConstraints for.
     */
    @Override
    public GridBagConstraints parse(String name) throws GridBagException {
        String line = null;
        for (String s : constraints) {
            int ptr = s.indexOf(":");
            if (ptr != -1) {
                String lname = s.substring(0, ptr);
                if (lname.equals(name)) {
                    line = s;
                    break;
                }
            }
        }
        if (line == null) {
            throw new GridBagException("Unknown component \"" + name + "\".");
        }
        GridBagConstraints c = ConstraintsLine.getConstraints(line);
        return c;
    }

    /**
     * @return the names of all components in the current parser.
     */
    @Override
    public String[] getComponentNames() {
        Vector<String> v = new Vector<String>();
        for (String s : constraints) {
            int ptr = s.indexOf(":");
            if (ptr != -1) {
                v.add(s.substring(0, ptr));
            }
        }
        String[] res = v.toArray(new String[0]);
        v.clear();
        return res;
    }
}
